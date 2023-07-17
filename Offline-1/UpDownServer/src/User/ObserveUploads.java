package User;

import util.NetworkUtil;

import java.io.FileOutputStream;
import java.io.IOException;

public class ObserveUploads {
    private NetworkUtil networkUtil;

    public ObserveUploads(NetworkUtil networkUtil) {
        this.networkUtil = networkUtil;
    }

    public void requestFileList(String fileDesc) throws IOException, ClassNotFoundException {
        Message m = new Message();
        m.setMsgBody(fileDesc);
        networkUtil.write(m);

        System.out.println("\n" + fileDesc + "------");
        Object o = networkUtil.read();
//        if (o instanceof String) System.out.println(o);
        while (o instanceof Message)
        {
            System.out.println("File Name: " + ((Message) o).getMsgBody() + " | File ID: " + ((Message) o).getFileID() + " | " + ((Message) o).getFilePrivacy());
            o = networkUtil.read(); // keep reading
        }
        System.out.println();
    }

    public void downloadFile(int fileID, String filename, String userDiskLocation) throws IOException, ClassNotFoundException {
        Message m = new Message();
        m.setMsgBody("download_file");
        m.setFileID(fileID);
        networkUtil.write(m);
        // Send the fileID to the server

        byte[] b;
        FileOutputStream fos = new FileOutputStream(userDiskLocation + "/" + filename);

        while (true) {
            Object getFile = networkUtil.read();
            if (getFile instanceof Message) {
                b = ((Message) getFile).getBytes();

                fos.write(b, 0, b.length);

                if (((Message) getFile).getACK().equalsIgnoreCase("completed")){
                    System.out.println("File downloaded");
                    break;
                }

            }
        }
    }


}
