package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.DatabaseUtil;
import util.HttpUtil;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

public class SearchMembersHandler implements HttpHandler {
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
        String name = params.get("name");

        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, email FROM members WHERE LOWER(name) LIKE ?");
            ps.setString(1, "%" + name.toLowerCase() + "%");
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            while (rs.next()) {
                json.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\"},",
                        rs.getString("id"), rs.getString("name").replace("\"", "\\\""),
                        rs.getString("email") == null ? "" : rs.getString("email").replace("\"", "\\\"")));
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