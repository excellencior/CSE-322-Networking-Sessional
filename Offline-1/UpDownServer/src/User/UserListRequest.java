package User;

import util.NetworkUtil;

import java.io.IOException;

public class UserListRequest {
    private NetworkUtil networkUtil;
    private Object receive;

    public UserListRequest(NetworkUtil networkUtil) {
        this.networkUtil = networkUtil;
        this.receive = null;
    }

    public void requestList() throws IOException {
        Message m = new Message();
        m.setMsgBody("user_list");
        networkUtil.write(m);
    }

    public void printRequestedList() throws IOException, ClassNotFoundException {
        receive = networkUtil.read();

        System.out.println("\nAll users ------------- ");
        if (receive instanceof String[]) {
            int userCnt = 1;
            for (String user: (String[]) receive) {
                System.out.println(userCnt + ". " + user);
                userCnt++;
            }
        }

        System.out.println();
    }
}
