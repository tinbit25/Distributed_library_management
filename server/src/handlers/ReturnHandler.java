package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class ReturnHandler implements HttpHandler {
    private static final double FINE_PER_DAY = 1.0;

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

        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpUtil.sendError(exchange, 405);
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            Map<String, String> params = HttpUtil.parseParams(HttpUtil.readBody(exchange));
            String memberId = params.get("memberId");
            int bookId = Integer.parseInt(params.get("bookId"));
            LocalDate returnDate = LocalDate.parse(params.get("returnDate"));

            // Find active borrow record
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, due_date FROM borrow_records WHERE book_id = ? AND member_id = ? AND return_date IS NULL");
            ps.setInt(1, bookId);
            ps.setString(2, memberId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                HttpUtil.sendResponse(exchange, 404, "{\"error\":\"No active borrow record\"}");
                return;
            }

            int recordId = rs.getInt("id");
            LocalDate dueDate = rs.getDate("due_date").toLocalDate();

            // Calculate fine
            double fine = 0;
            if (returnDate.isAfter(dueDate)) {
                long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
                fine = daysLate * FINE_PER_DAY;
            }

            // Update return date and fine
            PreparedStatement psUpdate = conn.prepareStatement(
                "UPDATE borrow_records SET return_date = ?, fine = ? WHERE id = ?");
            psUpdate.setDate(1, Date.valueOf(returnDate));
            psUpdate.setDouble(2, fine);
            psUpdate.setInt(3, recordId);
            psUpdate.executeUpdate();

            // Make book available again
            PreparedStatement psBook = conn.prepareStatement("UPDATE books SET available = true WHERE id = ?");
            psBook.setInt(1, bookId);
            psBook.executeUpdate();

            exchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500);
        }
    }
}