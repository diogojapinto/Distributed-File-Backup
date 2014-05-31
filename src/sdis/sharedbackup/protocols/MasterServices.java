package sdis.sharedbackup.protocols;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by knoweat on 29/05/14.
 */
public interface MasterServices extends Remote {

    public static final String REG_ID = "mfcss_master";

    public long getMasterClock() throws RemoteException;
    public SharedDatabase getMasterDB() throws RemoteException;
    public void addFile(FileRecord record) throws RemoteException;
    public void addUser(String username, String hashedPassword, AccessLevel accessLevel) throws RemoteException;
}
