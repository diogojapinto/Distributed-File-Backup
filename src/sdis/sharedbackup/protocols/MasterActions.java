package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.User;

import java.rmi.RemoteException;

/**
 * Created by knoweat on 29/05/14.
 */
public class MasterActions implements MasterServices {

    @Override
    public long getMasterClock() {
        return ConfigsManager.getInstance().getUpTime();
    }

    @Override
    public SharedDatabase getMasterDB() {
        return ConfigsManager.getInstance().getSDatabase();
    }

    @Override
    public void addFile(FileRecord record) throws RemoteException {
        boolean isNew = ConfigsManager.getInstance().getSDatabase().addFile(record);
        if (isNew) {
            FilesSharingManager.getInstance().addFileToSharedDB(record);
        }
    }

    @Override
    public void addUser(String username, String hashedPassword, AccessLevel accessLevel) throws RemoteException {
        User user = new User(username, hashedPassword, accessLevel);
        ConfigsManager.getInstance().getSDatabase().addUser(user);
        UsersSharingManager.getInstance().addUserToSharedDB(user);
    }
}
