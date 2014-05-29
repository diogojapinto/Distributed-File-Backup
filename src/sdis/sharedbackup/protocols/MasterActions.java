package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.ConfigsManager;

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
}
