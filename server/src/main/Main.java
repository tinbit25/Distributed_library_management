package main;

import com.sun.net.httpserver.HttpServer;
import handlers.*;
import util.DatabaseUtil;

import java.net.InetSocketAddress;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) throws Exception {
        // Test database connection at startup
        try {
            Connection testConn = DatabaseUtil.getConnection();
            System.out.println("Database connected successfully!");
            testConn.close();
        } catch (Exception e) {
            System.out.println("Database connection FAILED: " + e.getMessage());
            e.printStackTrace();
            // Optional: stop the server if DB is critical
            // System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/books", new BooksHandler());
        server.createContext("/api/members", new MembersHandler());
        server.createContext("/api/users", new UsersHandler());
        server.createContext("/api/borrow", new BorrowHandler());
        server.createContext("/api/return", new ReturnHandler());
        server.createContext("/api/reports", new ReportsHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/search/books", new SearchBooksHandler());
        server.createContext("/api/search/members", new SearchMembersHandler());
        
        // Serve static files (HTML, CSS, JS) from root
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        // Bind to all network interfaces (0.0.0.0)
        server.start();
        System.out.println("=================================================");
        System.out.println(" Server Running on PORT 8081");
        System.out.println(" Local:   http://localhost:8081/");
        System.out.println(" Network: http://[YOUR_IP_ADDRESS]:8081/");
        System.out.println("=================================================");
    }
}