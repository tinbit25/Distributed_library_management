package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class BooksHandler implements HttpHandler {
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
        String role = LoginHandler.getRoleFromToken(token);

        try (Connection conn = DatabaseUtil.getConnection()) {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, title, author, available FROM books");
                StringBuilder json = new StringBuilder("[");
                while (rs.next()) {
                    json.append(String.format("{\"id\":%d,\"title\":\"%s\",\"author\":\"%s\",\"available\":%b},",
                            rs.getInt("id"), rs.getString("title").replace("\"", "\\\""),
                            rs.getString("author").replace("\"", "\\\""), rs.getBoolean("available")));
                }
                if (json.length() > 1) json.deleteCharAt(json.length() - 1);
                json.append("]");
                HttpUtil.sendResponse(exchange, 200, json.toString());

            } else if ("POST".equals(method) && "admin".equals(role)) {
                Map<String, String> params = HttpUtil.parseParams(HttpUtil.readBody(exchange));
                PreparedStatement ps = conn.prepareStatement("INSERT INTO books (title, author) VALUES (?, ?)");
                ps.setString(1, params.get("title"));
                ps.setString(2, params.get("author"));
                ps.executeUpdate();
                exchange.sendResponseHeaders(200, -1);

            } else if ("PUT".equals(method) && "admin".equals(role)) {
                Map<String, String> params = HttpUtil.parseParams(HttpUtil.readBody(exchange));
                String sql = "UPDATE books SET title = COALESCE(?, title), author = COALESCE(?, author) WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, params.get("title"));
                ps.setString(2, params.get("author"));
                ps.setInt(3, Integer.parseInt(params.get("id")));
                ps.executeUpdate();
                exchange.sendResponseHeaders(200, -1);

            } else if ("DELETE".equals(method) && "admin".equals(role)) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = HttpUtil.parseParams(query);
                PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id = ?");
                ps.setInt(1, Integer.parseInt(params.get("id")));
                ps.executeUpdate();
                exchange.sendResponseHeaders(200, -1);

            } else {
                HttpUtil.sendError(exchange, 403);
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500);
        }
    }
}