package main;

import com.sun.net.httpserver.HttpServer;
import handlers.*;
import util.DatabaseUtil;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        int PORT = 8081;
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // -----------------------------
        // Serve static files from 'client' folder
        // -----------------------------
       server.createContext("/", new StaticFileHandler());

        // -----------------------------
        // API Endpoints
        // -----------------------------
      server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/books", new BooksHandler());
        server.createContext("/api/borrow", new BorrowHandler());
        server.createContext("/api/return", new ReturnHandler());
        server.createContext("/api/members", new MembersHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/reports", new ReportsHandler());
       server.createContext("/api/search/books", new SearchBooksHandler());
        server.createContext("/api/searchMembers", new SearchMembersHandler());
        server.createContext("/api/users", new UsersHandler());

        // Start server
        server.setExecutor(null); // default executor
        server.start();

        // Test DB connection
        try {
            DatabaseUtil.getConnection();
            System.out.println("Database connected successfully!");
        } catch (Exception e) {
            System.out.println("Database connection FAILED: " + e.getMessage());
        }

        System.out.println("=================================================");
        System.out.println(" Server Running on PORT " + PORT);
        System.out.println(" Local:   http://localhost:" + PORT + "/");
        System.out.println(" Network: http://[YOUR_IP_ADDRESS]:" + PORT + "/");
        System.out.println("=================================================");
    }
}
