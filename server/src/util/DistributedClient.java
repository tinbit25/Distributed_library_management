package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client utility for the Library Server to communicate with 
 * the Audit Server and Lock Manager.
 */
public class DistributedClient {
    private static final String HOST = "localhost";
    private static final int AUDIT_PORT = 9000;
    private static final int LOCK_PORT = 9001;
    private static final LamportClock clock = new LamportClock();

    /**
     * Send a log message to the Audit Server.
     */
    public static void log(String clientIp, String action) {
        int timestamp = clock.tick();
        new Thread(() -> {
            try (Socket socket = new Socket(HOST, AUDIT_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                // Protocol: CLOCK|NODE_ID|CLIENT_IP|ACTION
                out.println(String.format("%d|SERVER_01|%s|%s", timestamp, clientIp, action));
            } catch (Exception e) {
                System.err.println("Failed to send log to Audit Server: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Request a lock from the Lock Manager.
     */
    public static boolean acquireLock(String resourceId) {
        try (Socket socket = new Socket(HOST, LOCK_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println("ACQUIRE|" + resourceId);
            String response = in.readLine();
            return "OK".equals(response);
        } catch (Exception e) {
            System.err.println("Lock Manager Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Release a lock from the Lock Manager.
     */
    public static void releaseLock(String resourceId) {
        try (Socket socket = new Socket(HOST, LOCK_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            out.println("RELEASE|" + resourceId);
        } catch (Exception e) {
            System.err.println("Lock Manager Error: " + e.getMessage());
        }
    }
}
