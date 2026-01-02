package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class MembersHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
    HttpUtil.handleOptions(exchange);
    return;
}
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        if (!LoginHandler.isValidToken(token, "admin")) {
            HttpUtil.sendError(exchange, 401);
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, name, email FROM members");
                StringBuilder json = new StringBuilder("[");
                while (rs.next()) {
                    json.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\"},",
                            rs.getString("id"), rs.getString("name").replace("\"", "\\\""),
                            rs.getString("email") == null ? "" : rs.getString("email").replace("\"", "\\\"")));
                }
                if (json.length() > 1) json.deleteCharAt(json.length() - 1);
                json.append("]");
                HttpUtil.sendResponse(exchange, 200, json.toString());

            } else if ("POST".equals(method)) {
                Map<String, String> params = HttpUtil.parseParams(HttpUtil.readBody(exchange));
                PreparedStatement ps = conn.prepareStatement("INSERT INTO members (id, name, email) VALUES (?, ?, ?)");
                ps.setString(1, params.get("id"));
                ps.setString(2, params.get("name"));
                ps.setString(3, params.get("email"));
                ps.executeUpdate();
                exchange.sendResponseHeaders(200, -1);

            } else if ("PUT".equals(method)) {
                Map<String, String> params = HttpUtil.parseParams(HttpUtil.readBody(exchange));
                PreparedStatement ps = conn.prepareStatement("UPDATE members SET name = ?, email = ? WHERE id = ?");
                ps.setString(1, params.get("name"));
                ps.setString(2, params.get("email"));
                ps.setString(3, params.get("id"));
                ps.executeUpdate();
                exchange.sendResponseHeaders(200, -1);

            } else if ("DELETE".equals(method)) {
                Map<String, String> params = HttpUtil.parseParams(exchange.getRequestURI().getQuery());
                PreparedStatement ps = conn.prepareStatement("DELETE FROM members WHERE id = ?");
                ps.setString(1, params.get("id"));
                ps.executeUpdate();
                exchange.sendResponseHeaders(200, -1);

            } else {
                HttpUtil.sendError(exchange, 405);
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500);
        }
    }
}