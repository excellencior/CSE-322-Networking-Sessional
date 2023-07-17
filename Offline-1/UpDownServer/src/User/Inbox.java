package User;

import java.io.Serializable;
import java.util.HashMap;

public class Inbox implements Serializable {
    private HashMap<Integer, String> iTofMap; // all request_id to file map
    private HashMap<Integer, String> myiTofMap; // my request_id to file map

    public Inbox() {
        this.iTofMap = new HashMap<>();
        this.myiTofMap = new HashMap<>();
    }

    public void addToInbox(Integer requestID, String message) {
        this.iTofMap.put(requestID, message);
    }

    public void addToMyinfo(Integer requestID, String message) {
        this.myiTofMap.put(requestID, message);
    }

    public void showInbox() {
        for (Integer id: iTofMap.keySet()) {
            if (id != 0)
                System.out.println("Requested file id: " + id + " | File name: " + iTofMap.get(id));
            else
                System.out.println(iTofMap.get(id));
        }
    }

    public void clearInbox() {
        this.iTofMap.clear();
    }

    public String getFileName(Integer requestID) {
        if (iTofMap.containsKey(requestID))
            return this.iTofMap.get(requestID);
        else return "Wrong request id!";
    }
    
    public boolean isEmpty() {
        return this.iTofMap.size()==0;
    }

    public boolean isMyID(int requestID) {
        return this.myiTofMap.containsKey(requestID);
    }
}
