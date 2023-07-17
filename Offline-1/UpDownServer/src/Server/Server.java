package Server;

import User.Inbox;
import util.NetworkUtil;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {

    private ServerSocket serverSocket;
    public HashMap<String, NetworkUtil> clientMap;
    public HashMap<Integer, String> fileMap;
    public HashMap<String, Integer> fileMapInverse;
    public HashMap<String, Inbox> clientInboxMap;
    public static int fileID = 1, requestID = 1;
    private String serverDiskLocation = "Server-Disk/";

    Server() {
        clientMap = new HashMap<>();
        fileMap = new HashMap<>();
        fileMapInverse = new HashMap<>();
        clientInboxMap = new HashMap<>();
        try {
            serverSocket = new ServerSocket(33333);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                serve(clientSocket);
            }
        } catch (Exception e) {
            System.out.println("Server starts:" + e);
        }
    }

    public void serve(Socket clientSocket) throws IOException, ClassNotFoundException {
        NetworkUtil networkUtil = new NetworkUtil(clientSocket);
        String clientName = (String) networkUtil.read();

        if (clientMap.containsKey(clientName)) { // Same username login results in client disconnection
            networkUtil.write("Login Denied! Multiple login attempt.");

            networkUtil.closeConnection(); // Disconnect the client using client's socket
        }

        else {
            networkUtil.write("Login successful.");
            // When a client connects to a server, a new directory by that client's name is created
            new File(serverDiskLocation + clientName).mkdir();
            new File(serverDiskLocation + clientName + "/public").mkdir();
            new File(serverDiskLocation + clientName + "/private").mkdir();

            clientMap.put(clientName, networkUtil); // Online clients

            if (!clientInboxMap.containsKey(clientName)) {
                clientInboxMap.put(clientName, new Inbox());
            }
            new ThreadServer(clientMap, networkUtil, serverDiskLocation, fileMap, fileMapInverse, clientInboxMap, clientName);
        }


    }

    public static void main(String args[]) {
        new Server();
    }
}
