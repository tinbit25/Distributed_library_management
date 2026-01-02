package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class SearchBooksHandler implements HttpHandler {
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
        String field = params.containsKey("title") ? "title" : "author";
        String value = params.get(field);

        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, title, author, available FROM books WHERE LOWER(" + field + ") LIKE ?");
            ps.setString(1, "%" + value.toLowerCase() + "%");
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            while (rs.next()) {
                json.append(String.format("{\"id\":%d,\"title\":\"%s\",\"author\":\"%s\",\"available\":%b},",
                        rs.getInt("id"), rs.getString("title").replace("\"", "\\\""),
                        rs.getString("author").replace("\"", "\\\""), rs.getBoolean("available")));
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