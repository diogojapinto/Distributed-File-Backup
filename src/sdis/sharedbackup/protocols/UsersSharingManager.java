package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.MulticastCommunicator;
import sdis.sharedbackup.utils.Log;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

/**
 * Created by knoweat on 31/05/14.
 */
public class UsersSharingManager {
    public static final String ADD_FILE_CMD = "ADD_USER";

    private static UsersSharingManager instance = null;

    private UsersSharingManager() {
    }

    public static UsersSharingManager getInstance() {
        if (instance == null) {
            instance = new UsersSharingManager();
        }
        return instance;
    }

    // ADD FILE <hash> <file-name> <access-level>
    public boolean addFileToSharedDB(FileRecord record) {

        InetAddress multCtrlAddr = ConfigsManager.getInstance().getMCAddr();
        int multCtrlPort = ConfigsManager.getInstance().getMCPort();

        MulticastCommunicator sender = new MulticastCommunicator(multCtrlAddr,
                multCtrlPort);

        String message;

        message = ADD_FILE_CMD + " " + record.getHash() + " " + record.getFileName() + " " + record.getAccessLevel()
                .getId() + " " + MulticastCommunicator.CRLF + MulticastCommunicator.CRLF;

        try {
            sender.sendMessage(message
                    .getBytes(MulticastCommunicator.ASCII_CODE));
        } catch (MulticastCommunicator.HasToJoinException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Log.log("Sent ADD_FILE command for file " + record.getFileName()
                + " with hash " + record.getHash());

        return true;
    }
}
