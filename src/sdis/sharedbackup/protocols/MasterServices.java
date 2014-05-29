package sdis.sharedbackup.protocols;

import java.rmi.Remote;

/**
 * Created by knoweat on 29/05/14.
 */
public interface MasterServices extends Remote {

    public static final String REG_ID = "mfcss_master";

    public long getMasterClock();
    public SharedDatabase getMasterDB();
}
