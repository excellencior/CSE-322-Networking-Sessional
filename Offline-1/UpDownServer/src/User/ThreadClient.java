package User;

import util.NetworkUtil;

import java.io.*;
import java.util.*;

public class ThreadClient implements Runnable {
    private Thread thr;
    private NetworkUtil networkUtil;
    private String clientName;
    private UserListRequest userListRequest;
    private Upload uploadFile;
    private ObserveUploads observeUploads;
    private String userDiskLocation;
    public ThreadClient(NetworkUtil networkUtil, String clientName, String userDiskLocation) {
        this.networkUtil = networkUtil;
        this.thr = new Thread(this);
        this.clientName = clientName;
        this.userDiskLocation = userDiskLocation;

        userListRequest = new UserListRequest(networkUtil);
        uploadFile = new Upload(networkUtil);
        observeUploads = new ObserveUploads(networkUtil);

        thr.start();
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public void run() {
        try {
            while (true) {
                System.out.println("""
                        \n
                        1. See all users
                        2. My uploaded files
                        3. All files (Public only)
                        4. Request file
                        5. View unread messages
                        6. Upload file
                        7. Logout
                        8. Show my local storage""");

                Scanner scanner = new Scanner(System.in);
                String response = scanner.nextLine();


                if (response.equalsIgnoreCase("1")) {
                    userListRequest.requestList();
                    userListRequest.printRequestedList();
                }

                else if (response.equalsIgnoreCase("2") || response.equalsIgnoreCase("3")) {
                    if (response.equalsIgnoreCase("2"))
                        observeUploads.requestFileList("my_files");
                    else observeUploads.requestFileList("all_files");

                    System.out.println("To download any file type the file id or type (\"exit\") to exit");

                    System.out.print("Type Here: ");
                    response = scanner.nextLine();
                    if (response.equalsIgnoreCase("exit") || !isNumeric(response))
                        continue;

                    int fileID = Integer.parseInt(response);

                    System.out.print("File name (save as): ");
                    response = scanner.nextLine();

                    observeUploads.downloadFile(fileID, response, userDiskLocation);
                    // -----------------------
                }

                else if (response.equalsIgnoreCase("4")) { // file request
                    response = "request_file";
                    System.out.println("To request a file please provide the file name with extension");
                    System.out.print("File name: ");
                    response += "," + scanner.nextLine();

                    Message m = new Message();
                    m.setMsgBody(response);

                    networkUtil.write(m);
                }

                else if (response.equalsIgnoreCase("5")) {
                    System.out.println("--- Inbox (Unread messages) ---");

                    Message m = new Message();
                    m.setMsgBody("my_inbox");
                    networkUtil.write(m);

                    Object o = networkUtil.read();
                    if (o instanceof Inbox) {
                        if (((Inbox) o).isEmpty()) continue;

                        ((Inbox) o).showInbox(); // Showing user-side inbox

                        System.out.println("To upload a requested file please mention the request id (to exit type \"exit\")");
                        System.out.print("Response: ");
                        response = scanner.nextLine();

                        if (response.equalsIgnoreCase("exit") || !isNumeric(response)) {
                            ((Inbox) o).clearInbox();
                            continue;
                        }

                        Integer reqID = Integer.parseInt(response);
                        String fileName = ((Inbox) o).getFileName(reqID);

                        if (fileName.equalsIgnoreCase("Wrong request id!")) {
                            System.out.println("Please provide a valid request id");
                            continue;
                        }

                        uploadFile.sendRequestedFileInfo(reqID, userDiskLocation, fileName); // send basic information about the file to the server

                        Object receive = networkUtil.read();

                        if (receive instanceof Message) {
                            if (((Message) receive).getACK().equalsIgnoreCase("error"))
                                System.out.println(((Message) receive).getMsgBody()); // warning info from the server
                            else {
                                int chunkSize = Integer.parseInt(((Message) receive).getMsgBody());
                                uploadFile.upload(chunkSize);
                                ((Inbox) o).clearInbox(); // Clearing the inbox after file upload
                            }
                        }
                        // ----------------
                    }
                }

                else if (response.equalsIgnoreCase("6")) { // upload a file
                    uploadFile.sendFileInfo(userDiskLocation); // send basic information about the file to the server

                    Object receive = networkUtil.read();

                    if (receive instanceof Message) {
                        if (((Message) receive).getACK().equalsIgnoreCase("error"))
                            System.out.println(((Message) receive).getMsgBody()); // warning info from the server
                        else {
                            int chunkSize = Integer.parseInt(((Message) receive).getMsgBody());
                            uploadFile.upload(chunkSize);
                        }
                    }
                    // File uploading done
                }

                else if (response.equalsIgnoreCase("7")) {
                    networkUtil.closeConnection();
                    break;
                }

                else if (response.equalsIgnoreCase("8")) {
                    System.out.println("\nYour storage ----- ");

                    File[] files = new File(userDiskLocation).listFiles();
                    int fileIdx = 1;
                    if (files != null) {
                        for (File file: files) {
                            System.out.println(fileIdx + ". " + file.getName() + " | " + file.length() + "-bytes");
                            fileIdx++;
                        }
                    }
                    // ----------
                }
                // ---------------------
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                networkUtil.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



