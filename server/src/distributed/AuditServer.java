package distributed;

import util.DatabaseUtil;
import util.LamportClock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Standalone Audit Server representing a centralized monitoring node.
 * Listens for log messages via Sockets and stores them in the DB.
 */
public class AuditServer {
    private static final int PORT = 9000;
    private static final LamportClock clock = new LamportClock();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=========================================");
            System.out.println(" Distributed Audit Server started on Port " + PORT);
            System.out.println("=========================================");

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    
                    String message = in.readLine();
                    if (message != null) {
                        // Protocol: CLOCK|NODE_ID|CLIENT_IP|ACTION
                        String[] parts = message.split("\\|");
                        if (parts.length == 4) {
                            int receivedClock = Integer.parseInt(parts[0]);
                            String nodeId = parts[1];
                            String clientIp = parts[2];
                            String action = parts[3];

                            // Update local Lamport Clock
                            clock.update(receivedClock);
                            int finalClock = clock.getValue();

                            System.out.printf("[%d] Log from Node %s: %s (Client: %s)\n", 
                                              finalClock, nodeId, action, clientIp);

                            saveToDatabase(clientIp, String.format("[%d] %s", finalClock, action));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error processing log: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveToDatabase(String ip, String action) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "INSERT INTO system_logs (client_ip, action) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, ip);
            ps.setString(2, action);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Database Error: " + e.getMessage());
        }
    }
}
