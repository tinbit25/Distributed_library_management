package util;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class HttpUtil {

    public static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            return br.lines().reduce("", (a, b) -> a + b);
        }
    }

    public static Map<String, String> parseParams(String body) {
        Map<String, String> map = new HashMap<>();
        if (body != null && !body.isEmpty()) {
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    try {
                        map.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        map.put(kv[0], kv[1]); // fallback
                    }
                } else if (kv.length == 1) {
                    map.put(kv[0], "");
                }
            }
        }
        return map;
    }

    public static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "86400");
    }

    public static void handleOptions(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(200, -1);
    }

    public static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        addCorsHeaders(exchange);
        String jsonResponse = response == null || response.isEmpty() ? "{}" : response;
        byte[] bytes = jsonResponse.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int code, String message) throws IOException {
        addCorsHeaders(exchange);
        String json = "{\"success\":false,\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int code) throws IOException {
        sendError(exchange, code, "Internal server error");
    }
}