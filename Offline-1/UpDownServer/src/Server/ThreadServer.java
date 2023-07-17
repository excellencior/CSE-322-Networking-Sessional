package Server;

import User.Inbox;
import User.Message;
import util.NetworkUtil;

import java.io.*;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Random;

public class ThreadServer implements Runnable {
    private Thread thr;
    private NetworkUtil networkUtil;
    private String serverDiskLocation;
    private HashMap<String, NetworkUtil> clientMap;
    private HashMap<Integer, String> fileMap; // map between fileID and file_location
    private HashMap<String, Integer> fileMapInverse; // does what the name suggests
    private HashMap<String, Inbox> clientInboxMap;
    private String connectedClientName;

    private int MAX_BUFFER_SIZE = 10000; // in byte
    private int MIN_CHUNK_SIZE = 50;
    private int MAX_CHUNK_SIZE = 150;
    private int USED_MEM = 0;

    public ThreadServer(HashMap<String, NetworkUtil> map, NetworkUtil networkUtil, String serverDiskLocation, HashMap<Integer, String> fileMap, HashMap<String, Integer> fileMapInverse, HashMap<String, Inbox> clientInboxMap, String connectedClientName) {
        this.clientMap = map;
        this.connectedClientName = connectedClientName;
        this.networkUtil = networkUtil;
        this.serverDiskLocation = serverDiskLocation;
        this.fileMap = fileMap;
        this.fileMapInverse = fileMapInverse;
        this.clientInboxMap = clientInboxMap;
        this.thr = new Thread(this);
        thr.start();
    }

    private void fileSend(String fileLocation) throws IOException {
        int fileSize = (int) new File(fileLocation).length(), chunkSize = MAX_CHUNK_SIZE;
        FileInputStream fis = new FileInputStream(fileLocation);

        byte[] b;
        System.out.println("File size to download: " + fileSize);
        Message byteMessage = new Message();

        // ----------------------------------- byte by byte file sending
        int i, bound = fileSize/chunkSize;
        for (i=0; i<bound; i++) {
            b = new byte[chunkSize];
            fis.read(b, 0, chunkSize);
            byteMessage.setBytes(b);

            if ((fileSize%chunkSize==0) && (i == (bound-1)))
                byteMessage.setACK("completed");

            networkUtil.write(byteMessage);
        }
        bound = fileSize % chunkSize; // last remaining bytes
        if (bound!=0) {
            b = new byte[bound];
            fis.read(b, 0, bound);
            byteMessage.setBytes(b);
            byteMessage.setACK("completed");
            networkUtil.write(byteMessage);
        }
    }

    private void sendFileListOf(String clientName, String type) throws IOException {
        File[] files = new File(serverDiskLocation + clientName + "/public").listFiles();

        Message m = new Message();
        for (int i=0; i<files.length; i++) {
            m.setMsgBody(files[i].getName());
            String fileDirName = serverDiskLocation + clientName + "/public/" + files[i].getName();

            if (fileMapInverse.containsKey(fileDirName)) {
                m.setFileID(fileMapInverse.get(fileDirName));
                m.setFilePrivacy("public");
            }
            System.out.println(fileDirName + " " + m.getFileID());

            networkUtil.write(m);
        }

        if (type.equalsIgnoreCase("all_files"))
            return;

        files = new File(serverDiskLocation + clientName + "/private").listFiles();
        for (int i=0; i<files.length; i++) {
            m.setMsgBody(files[i].getName());
            String fileDirName = serverDiskLocation + clientName + "/private/" + files[i].getName();

            if (fileMapInverse.containsKey(fileDirName)) {
                m.setFileID(fileMapInverse.get(fileDirName));
                m.setFilePrivacy("private");
            }

            networkUtil.write(m);
        }
    }

    public void run() {
        try {
            while (true) {
                Object o = networkUtil.read(); // from clients

                if (o instanceof Message) {
                    System.out.println("Roaming");

                    Message message = new Message();
                    message.setFrom("Server");
                    // ------------------------------------------------- //

                    if (((Message) o).getMsgBody().equalsIgnoreCase("user_list")) {
                        System.out.println("All user list --");

                        File file = new File(serverDiskLocation);
                        String[] allUser = file.list(); // files or directories

                        for (int i=0; i<allUser.length; i++) {
                            if (clientMap.containsKey(allUser[i])) {
                                allUser[i] =  allUser[i] + " | Active";
                            }
                            else allUser[i] =  allUser[i] + " | Idle";
                        }

                        networkUtil.write(allUser);
                    }
                    else if (((Message) o).getMsgBody().substring(0, 6).equalsIgnoreCase("upload")) {
                        String[] fileInfo = ((Message) o).getMsgBody().split(",");
                        String fileName = fileInfo[1];
                        int fileSize = Integer.parseInt(fileInfo[2]);
                        String filePrivacy = ((Message) o).getFilePrivacy().toLowerCase();

                        if ((USED_MEM+fileSize) > MAX_BUFFER_SIZE) {
                            message.setMsgBody("Storage full, cannot upload file of size more than " + (MAX_BUFFER_SIZE-USED_MEM) + " byte\n");
                            message.setACK("error");
                            networkUtil.write(message);
                        }
                        else {
                            int chunkSize = (new Random().nextInt(MAX_CHUNK_SIZE-MIN_CHUNK_SIZE)) + MIN_CHUNK_SIZE; // chunk size generation - Random

                            String mapFileName = serverDiskLocation+connectedClientName+"/"+filePrivacy+"/"+fileName;
                            fileMap.put(Server.fileID, mapFileName);
                            fileMapInverse.put(mapFileName, Server.fileID);
                            Server.fileID++;

                            message.setMsgBody(chunkSize + ""); // <------------------
                            message.setACK("successful");
                            networkUtil.write(message); // send the upload the chunk size

                            //server receiving the file in chunks
                            byte []b = null;
                            Object byteMessage = null;
                            String fileStoreLocation = null;

                            if (filePrivacy.equalsIgnoreCase("public")) {
                                fileStoreLocation = serverDiskLocation + connectedClientName + "/public/" + fileName;
                            }
                            else if (filePrivacy.equalsIgnoreCase("private")) {
                                fileStoreLocation = serverDiskLocation + connectedClientName + "/private/" + fileName;
                            }
                            
                            FileOutputStream fos = new FileOutputStream(fileStoreLocation);
                            File helper = new File(fileStoreLocation);

                            System.out.println("Filesize: " + fileSize + ", chunkSize: " + chunkSize);

                            int i = 0;
                            int history_mem = USED_MEM;
                            while(true) {
                                byteMessage = networkUtil.read();
                                if (byteMessage instanceof Message) {
                                    b = ((Message) byteMessage).getBytes();
                                    i += b.length;
                                    System.out.println("Received: " + i);

                                    if (((Message) byteMessage).getMsgBody().equalsIgnoreCase("timeout")) {
                                        int id = fileMapInverse.get(mapFileName);
                                        fileMap.remove(id);
                                        fileMapInverse.remove(mapFileName);

                                        fos.close();
                                        if (helper.delete()) {
                                            System.out.println("Byte stream acknowledge timeout");
                                            System.out.println("File removed");
                                            USED_MEM = history_mem;
                                        }
                                        break;
                                    }
                                    else if (((Message) byteMessage).getMsgBody().equalsIgnoreCase("last_chunk")) {
                                        if (i == fileSize) { // check if the file received has the aforementioned size
                                            System.out.println("File fully received");
                                            networkUtil.write("Upload_successful");

                                            fos.write(b, 0, b.length); // write the last chunk in the server directory
                                            USED_MEM += i; // Update the used space count
                                            break;
                                        }
                                        else {
                                            networkUtil.write("Error. File size mismatch");
                                            int id = fileMapInverse.get(mapFileName);
                                            fileMap.remove(id);
                                            fileMapInverse.remove(mapFileName);

                                            fos.close();
                                            if (helper.delete()) {
                                                System.out.println("Error. File size mismatch");//delete the file
                                                System.out.println("File removed!");
                                                USED_MEM = history_mem;
                                            }
                                            break;
                                        }
                                    }

                                    fos.write(b, 0, b.length); // write the file in the server directory

                                    networkUtil.write("received"); // acknowledgement
                                }
                            }

                            if (fileInfo.length == 4) {
                                String uploadType = fileInfo[3];
                                if (uploadType.equals("requestedFileUpload")) {
                                    int reqID = ((Message) o).getRequestID(); // concat

                                    for (String name : clientInboxMap.keySet()) {
                                        if (clientInboxMap.get(name).isMyID(reqID)) {
                                            clientInboxMap.get(name).addToInbox(0, "# Your requested file is uploaded by \"" + connectedClientName + "\" | Request ID: " + reqID);
                                            break;
                                        }
                                    }
                                    // ----------
                                }
                            }
                            // ------------
                        }
                    }
                    else if (((Message) o).getMsgBody().equalsIgnoreCase("my_files")) {
                        sendFileListOf(connectedClientName, "my_files");
                        networkUtil.write("completed");
                    }

                    else if (((Message) o).getMsgBody().equals("all_files")) { // send the list of all public files
                        String[] clients = new File(serverDiskLocation).list();

                        for (String client : clients) {
                            sendFileListOf(client, "all_files");
                        }
                        networkUtil.write("completed");
                        // -------------------------------------
                    }
                    else if (((Message) o).getMsgBody().equalsIgnoreCase("download_file")) {
                        int fileID = ((Message) o).getFileID(); // file id sent from the client

                        String fileLocation = fileMap.get(fileID);

                        fileSend(fileLocation);
                    }

                    else if (((Message) o).getMsgBody().split(",")[0].equalsIgnoreCase("request_file")) {
                        String filename = ((Message) o).getMsgBody().split(",")[1]; // filename is here

                        clientInboxMap.get(connectedClientName).addToMyinfo(Server.requestID, filename);
                        for (String name: clientInboxMap.keySet()) { // broadcasting to every connected client
                            if (!connectedClientName.equalsIgnoreCase(name))
                                clientInboxMap.get(name).addToInbox(Server.requestID, filename);
                        }

                        Server.requestID++;
                    }

                    else if (((Message) o).getMsgBody().equalsIgnoreCase("my_inbox")) {
                        Inbox myInbox = clientInboxMap.get(connectedClientName);
                        networkUtil.write(myInbox);
                    }

                }
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                clientMap.remove(connectedClientName);
                System.out.println("Client logged out");
            }
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



