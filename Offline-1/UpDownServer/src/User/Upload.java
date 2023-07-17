package User;

import util.NetworkUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Upload {
    private NetworkUtil networkUtil;
    private String filePath;

    public Upload(NetworkUtil networkUtil) {
        this.networkUtil = networkUtil;
    }

    public void upload(int chunkSize) throws IOException, ClassNotFoundException {
        System.out.println("Maximum allowable chunk size is " + chunkSize + " byte");

        FileInputStream fis = new FileInputStream(filePath);
        int fileSize = fis.available(); // estimated file size

        byte []b;
        Message byteMessage = new Message();

        int i, bound = fileSize/chunkSize, timeout = 0;
        int chunkCount = 0;
        for (i=0; i<bound; i++) {
            b = new byte[chunkSize];
            fis.read(b, 0, chunkSize);
            byteMessage.setBytes(b);
            networkUtil.write(byteMessage);

            String ack;
            try {
                ack = (String) networkUtil.read();
                if (ack.equalsIgnoreCase("received"))
                    chunkCount++;
            } catch (SocketTimeoutException e) {
                System.out.println("Reception timeout");
                byteMessage.setMsgBody("timeout");
                networkUtil.write(byteMessage);
                return; // terminating transmission
            }

        }
        bound = fileSize % chunkSize;

        if (bound!=0 && timeout==0) {
            b = new byte[bound];
            fis.read(b, 0, bound);
            byteMessage.setMsgBody("last_chunk");
            byteMessage.setBytes(b);
            networkUtil.write(byteMessage);

            String ack;
            try {
                ack = (String) networkUtil.read();
            } catch (SocketTimeoutException e) {
                System.out.println("Reception timeout");
                byteMessage.setMsgBody("timeout");
                networkUtil.write(byteMessage);
                return; // terminating transmission
            }
            if (ack.equalsIgnoreCase("Upload_successful")) {
                System.out.println(ack);
                chunkCount++;
            }
            else System.out.println(ack);
            System.out.println("Total chunks sent: " + chunkCount);
        }
    }

    public void sendFileInfo(String userDiskLocation) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String response;

        System.out.println("--------------------------- Files ------------------------------");
        File[] files = new File(userDiskLocation).listFiles();
        int fileIdx = 1;
        if (files != null) {
            for (File file: files) {
                System.out.println(fileIdx + ". " + file.getName() + " | " + file.length() + "-bytes");
                fileIdx++;
            }
        }
        System.out.print("Upload file no: ");
        fileIdx = scanner.nextInt() - 1;

        this.filePath = files[fileIdx].getPath(); // saving the file path

        System.out.print("Enter file size: ");
        int fileSize = scanner.nextInt();

        System.out.println("File name: " + files[fileIdx].getName());
        System.out.println("File size: " + fileSize + "-bytes");

        System.out.print("Set file privacy: ");
        String filePrivacy = scanner.next();

        response = "upload";
        response += "," + files[fileIdx].getName();
        response += "," + fileSize;

        Message m = new Message();
        m.setMsgBody(response);
        m.setFilePrivacy(filePrivacy);

        networkUtil.write(m);
    }

    public void sendRequestedFileInfo(int reqID, String userDiskLocation, String fileName) throws IOException {
        Scanner scanner = new Scanner(System.in);

        File file = new File(userDiskLocation + "/" + fileName);

        if (!file.exists()) {
            System.out.println("You currently don't have this file in your possession");
            return;
        }

        System.out.println("File name: " + file.getName());
        System.out.println("File size: " + file.length() + "-bytes");

        System.out.print("Enter file size: ");
        int fileSize = scanner.nextInt();

        System.out.println("Set file privacy: public (default)");
        this.filePath = file.getPath(); // required for uploading

        String response = "upload";
        response += "," + file.getName();
        response += "," + fileSize;
        response += ",requestedFileUpload"; // For server to ack the requester after file availability

        Message m = new Message();
        m.setMsgBody(response);
        m.setFilePrivacy("public");
        m.setRequestID(reqID);

        networkUtil.write(m);
    }
}
