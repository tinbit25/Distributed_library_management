package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;

public class UsersHandler implements HttpHandler {
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
            if ("GET".equals(exchange.getRequestMethod())) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, username, role FROM users WHERE role != 'admin'");
                StringBuilder json = new StringBuilder("[");
                while (rs.next()) {
                    json.append(String.format("{\"id\":%d,\"username\":\"%s\",\"role\":\"%s\"},", 
                            rs.getInt("id"), rs.getString("username"), rs.getString("role")));
                }
                if (json.length() > 1) json.deleteCharAt(json.length() - 1);
                json.append("]");
                HttpUtil.sendResponse(exchange, 200, json.toString());
            } else if ("POST".equals(exchange.getRequestMethod())) {
                var params = HttpUtil.parseParams(HttpUtil.readBody(exchange));
                String role = params.get("role");
                if (role == null || (!role.equals("employee") && !role.equals("student"))) {
                    role = "employee"; // Default or fallback
                }
                PreparedStatement ps = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
                ps.setString(1, params.get("username"));
                ps.setString(2, params.get("password"));
                ps.setString(3, role);
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