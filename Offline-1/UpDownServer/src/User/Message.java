package User;

import java.io.Serializable;

public class Message implements Serializable {
    private String from;
    private String msgBody;
    private String ACK;
    private byte[] bytes;
    private String filePrivacy;
    private int fileID;
    private int requestID;
    public Message() {
        this.msgBody = "";
        this.filePrivacy = "";
        this.ACK = "";
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMsgBody() {
        return msgBody;
    }

    public void setMsgBody(String msgBody) {
        this.msgBody = msgBody;
    }

    public void setACK(String ACK) {
        this.ACK = ACK;
    }

    public String getACK() {
        return this.ACK;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getFilePrivacy() {
        return filePrivacy;
    }

    public void setFilePrivacy(String filePrivacy) {
        this.filePrivacy = filePrivacy;
    }

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public int getRequestID() {
        return requestID;
    }

    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }
}