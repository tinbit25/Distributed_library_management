package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private static final String CLIENT_DIR = "C:/Users/WIN10/Desktop/Distributed_library_management/client";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String uriPath = exchange.getRequestURI().getPath();
        System.out.println("Request Received: " + uriPath + " - From: " + exchange.getRemoteAddress());
        
        // Default to index.html
        if (uriPath.equals("/")) {
            uriPath = "/index.html";
        }

        // Prevent directory traversal attacks
        if (uriPath.contains("..")) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        File file = new File(CLIENT_DIR + uriPath);

        if (!file.exists() || file.isDirectory()) {
            sendError(exchange, 404, "Not Found");
            return;
        }

        String mimeType = "text/plain";
        if (uriPath.endsWith(".html")) mimeType = "text/html";
        else if (uriPath.endsWith(".css")) mimeType = "text/css";
        else if (uriPath.endsWith(".js")) mimeType = "application/javascript";

        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, file.length());

        try (OutputStream os = exchange.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
            final byte[] buffer = new byte[0x10000];
            int count = 0;
            while ((count = fs.read(buffer)) >= 0) {
                os.write(buffer, 0, count);
            }
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
