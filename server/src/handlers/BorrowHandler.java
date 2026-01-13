package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;

public class BorrowHandler implements HttpHandler {
    private static final int BORROW_DAYS = 14;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpUtil.handleOptions(exchange);
            return;
        }

        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (!LoginHandler.isValidToken(token, null)) {
            HttpUtil.sendError(exchange, 401);
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                // List all borrowed books
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT br.book_id, br.member_id, br.borrow_date, br.due_date, br.return_date, br.fine, b.title " +
                    "FROM borrow_records br JOIN books b ON br.book_id = b.id"
                );

                StringBuilder json = new StringBuilder("[");
                while (rs.next()) {
                    json.append(String.format(
                        "{\"bookId\":%d,\"memberId\":\"%s\",\"borrowDate\":\"%s\",\"dueDate\":\"%s\",\"returnDate\":%s,\"fine\":%.2f,\"title\":\"%s\"},",
                        rs.getInt("book_id"),
                        rs.getString("member_id"),
                        rs.getDate("borrow_date"),
                        rs.getDate("due_date"),
                        rs.getObject("return_date") == null ? "null" : "\"" + rs.getDate("return_date") + "\"",
                        rs.getDouble("fine"),
                        rs.getString("title").replace("\"", "\\\"")
                    ));
                }
                if (json.length() > 1) json.deleteCharAt(json.length() - 1);
                json.append("]");
                HttpUtil.sendResponse(exchange, 200, json.toString());

            } else if ("POST".equals(method)) {
                Map<String, String> params = HttpUtil.parseParams(HttpUtil.readBody(exchange));
                String memberId = params.get("memberId"); // must match members.id
                int bookId = Integer.parseInt(params.get("bookId"));
                LocalDate borrowDate = LocalDate.parse(params.get("borrowDate"));

                String resourceId = "book_" + bookId;

                // 1. Distributed Mutual Exclusion: Try to acquire lock
                if (!util.DistributedClient.acquireLock(resourceId)) {
                    HttpUtil.sendResponse(exchange, 409, "{\"error\":\"Resource is busy. Please try again later.\"}");
                    return;
                }

                try {
                    // Check if member exists
                    PreparedStatement psMember = conn.prepareStatement("SELECT id FROM members WHERE id = ?");
                    psMember.setString(1, memberId);
                    ResultSet rsMember = psMember.executeQuery();
                    if (!rsMember.next()) {
                        HttpUtil.sendResponse(exchange, 400, "{\"error\":\"Member does not exist\"}");
                        return;
                    }

                    // Check if book exists and is available
                    PreparedStatement psBook = conn.prepareStatement("SELECT available FROM books WHERE id = ?");
                    psBook.setInt(1, bookId);
                    ResultSet rsBook = psBook.executeQuery();
                    if (!rsBook.next()) {
                        HttpUtil.sendResponse(exchange, 400, "{\"error\":\"Book does not exist\"}");
                        return;
                    }
                    if (!rsBook.getBoolean("available")) {
                        HttpUtil.sendResponse(exchange, 400, "{\"error\":\"Book not available\"}");
                        return;
                    }

                    // Insert borrow record
                    LocalDate dueDate = borrowDate.plusDays(BORROW_DAYS);
                    PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO borrow_records (book_id, member_id, borrow_date, due_date) VALUES (?, ?, ?, ?)"
                    );
                    ps.setInt(1, bookId);
                    ps.setString(2, memberId);
                    ps.setDate(3, Date.valueOf(borrowDate));
                    ps.setDate(4, Date.valueOf(dueDate));
                    ps.executeUpdate();

                    // Update book availability
                    PreparedStatement psUpdate = conn.prepareStatement("UPDATE books SET available = false WHERE id = ?");
                    psUpdate.setInt(1, bookId);
                    psUpdate.executeUpdate();

                    // 2. Distributed Logging
                    util.DistributedClient.log(
                        exchange.getRemoteAddress().getAddress().getHostAddress(),
                        "Book Borrowed - BookID: " + bookId + " by Member: " + memberId
                    );

                    exchange.sendResponseHeaders(200, -1);
                } finally {
                    // 3. Release Lock
                    util.DistributedClient.releaseLock(resourceId);
                }

            } else {
                HttpUtil.sendError(exchange, 405);
            }

        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500);
        }
    }
}
