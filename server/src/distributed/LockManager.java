package distributed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standalone Lock Manager implementing Centralized Mutual Exclusion.
 */
public class LockManager {
    private static final int PORT = 9001;
    // Map of resourceId -> isLocked
    private static final ConcurrentHashMap<String, Boolean> locks = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=========================================");
            System.out.println(" Distributed Lock Manager started on Port " + PORT);
            System.out.println("=========================================");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleRequest(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            String line = in.readLine();
            if (line == null) return;

            // Protocol: ACQUIRE|id  or RELEASE|id
            String[] parts = line.split("\\|");
            String action = parts[0];
            String resourceId = parts[1];

            if ("ACQUIRE".equals(action)) {
                // Return true if lock was acquired (not already held)
                Boolean alreadyLocked = locks.putIfAbsent(resourceId, true);
                if (alreadyLocked == null) {
                    System.out.println("Lock ACQUIRED for resource: " + resourceId);
                    out.println("OK");
                } else {
                    System.out.println("Lock DENIED for resource: " + resourceId + " (Busy)");
                    out.println("BUSY");
                }
            } else if ("RELEASE".equals(action)) {
                locks.remove(resourceId);
                System.out.println("Lock RELEASED for resource: " + resourceId);
                out.println("OK");
            }

        } catch (Exception e) {
            System.err.println("Lock Manager Error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
}
