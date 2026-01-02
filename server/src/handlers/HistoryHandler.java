package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class HistoryHandler implements HttpHandler {
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
        String memberId = params.get("memberId");

        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT b.title, br.borrow_date, br.return_date, br.fine " +
                "FROM borrow_records br JOIN books b ON br.book_id = b.id " +
                "WHERE br.member_id = ? ORDER BY br.borrow_date DESC");
            ps.setString(1, memberId);
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            while (rs.next()) {
                json.append(String.format("{\"title\":\"%s\",\"borrowDate\":\"%s\",\"returnDate\":%s,\"fine\":%.2f},",
                        rs.getString("title").replace("\"", "\\\""),
                        rs.getDate("borrow_date"),
                        rs.getObject("return_date") == null ? "null" : "\"" + rs.getDate("return_date") + "\"",
                        rs.getDouble("fine")));
            }
            if (json.length() > 1) json.deleteCharAt(json.length() - 1);
            json.append("]");
            HttpUtil.sendResponse(exchange, 200, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500);
        }
    }
}