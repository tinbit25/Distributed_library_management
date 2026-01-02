package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;

public class ReportsHandler implements HttpHandler {
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

        Map<String, String> params = HttpUtil.parseParams(exchange.getRequestURI().getQuery());
        String type = params.get("type");

        try (Connection conn = DatabaseUtil.getConnection()) {
            if ("total_books".equals(type)) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books");
                rs.next();
                HttpUtil.sendResponse(exchange, 200, String.valueOf(rs.getInt(1)));

            } else if ("issued_books".equals(type)) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM borrow_records WHERE return_date IS NULL");
                rs.next();
                HttpUtil.sendResponse(exchange, 200, String.valueOf(rs.getInt(1)));

            } else if ("overdue_books".equals(type)) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT book_id, member_id, borrow_date, due_date FROM borrow_records " +
                    "WHERE return_date IS NULL AND due_date < ?");
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                ResultSet rs = ps.executeQuery();
                StringBuilder json = new StringBuilder("[");
                while (rs.next()) {
                    json.append(String.format("{\"bookId\":%d,\"memberId\":\"%s\",\"borrowDate\":\"%s\",\"dueDate\":\"%s\"},",
                            rs.getInt("book_id"), rs.getString("member_id"),
                            rs.getDate("borrow_date"), rs.getDate("due_date")));
                }
                if (json.length() > 1) json.deleteCharAt(json.length() - 1);
                json.append("]");
                HttpUtil.sendResponse(exchange, 200, json.toString());

            } else {
                HttpUtil.sendError(exchange, 400);
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500);
        }
    }
}