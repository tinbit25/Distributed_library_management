package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.HttpUtil;
import util.DatabaseUtil;
import util.DistributedClient;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class LoginHandler implements HttpHandler {

    // Simple static token validator used by other handlers
    public static boolean isValidToken(String token, String requiredRole) {
        if (token == null || token.isEmpty()) return false;
        
        // Check if token corresponds to role (using our simple prefix strategy)
        String role = getRoleFromToken(token);
        if ("admin".equals(requiredRole) && !"admin".equals(role)) return false;
        // logic for other roles if needed, or if requiredRole is null just check validity
        
        return true;
    }

    public static String getRoleFromToken(String token) {
        if (token == null) return "";
        if (token.startsWith("admin-")) return "admin";
        if (token.startsWith("employee-")) return "employee";
        if (token.startsWith("student-")) return "student";
        return "";
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // ✅ Handle CORS preflight
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.handleOptions(exchange);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        // ✅ READ BODY FIRST
        String body = HttpUtil.readBody(exchange);

        System.out.println("RAW BODY: " + body); // DEBUG LINE

        // ✅ PARSE BODY
        Map<String, String> params = HttpUtil.parseParams(body);

        String username = params.get("username");
        String password = params.get("password");

        System.out.println("Login attempt: username='" + username + "', password='" + password + "'");

        if (username == null || password == null) {
            HttpUtil.sendError(exchange, 400, "Missing username or password");
            return;
        }

        // ✅ DATABASE CHECK
        try (Connection conn = DatabaseUtil.getConnection()) {

            String sql = "SELECT role FROM users WHERE username=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role").toLowerCase();
                String token = role + "-token-" + System.currentTimeMillis(); // Generate a token with role prefix

                // For students, we assume username IS the memberId
                String memberIdJsonPart = "";
                if ("student".equals(role)) {
                    memberIdJsonPart = ", \"memberId\": \"" + username + "\"";
                }

                String json =
                        "{ \"success\": true, \"role\": \"" + role + "\", \"token\": \"" + token + "\"" + memberIdJsonPart + " }";

                // Distributed Logging
                DistributedClient.log(exchange.getRemoteAddress().getAddress().getHostAddress(), 
                                      "Login Success - User: " + username + " (Role: " + role + ")");

                HttpUtil.sendResponse(exchange, 200, json);
            } else {
                HttpUtil.sendError(exchange, 401, "Invalid credentials");
            }

        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(exchange, 500, "Server error");
        }
    }
}
