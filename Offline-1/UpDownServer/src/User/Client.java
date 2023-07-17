package User;

import util.NetworkUtil;

import java.io.File;
import java.util.Scanner;

public class Client {
    private String userDiskLocation = "User-Disk/";
    public Client(String serverAddress, int serverPort) {
        try {
            System.out.print("Enter username: ");

            Scanner scanner = new Scanner(System.in);
            String clientName = scanner.nextLine();

            NetworkUtil networkUtil = new NetworkUtil(serverAddress, serverPort);
            networkUtil.write(clientName);

            String serverACK = (String) networkUtil.read(); // waits for server acknowledgement

            System.out.println(serverACK);

            if (!serverACK.equalsIgnoreCase("Login Denied! Multiple login attempt.")) {
                userDiskLocation = userDiskLocation + clientName;
                new File(userDiskLocation).mkdir();
                new ThreadClient(networkUtil, clientName, userDiskLocation);
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) {
        String serverAddress = "127.0.0.1";
        int serverPort = 33333;
        new Client(serverAddress, serverPort);
    }
}


