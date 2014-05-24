package sdis.sharedbackup.backend;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by knoweat on 21/05/14.
 */
public class SharedClock {

    private long mSharedTime = 0;
    private long mEndSyncTime = 0;
    private boolean isSynced = false;
    private Date mDate;


    private static SharedClock sInstance = null;

    private SharedClock() {
        mDate = new Date();
    }

    public static SharedClock getInstance() {
        if (sInstance == null) {
            sInstance = new SharedClock();
        }

        return sInstance;
    }

    public long getTime() throws NotSyncedException {
        if (!isSynced) {
            throw new NotSyncedException();
        }

        long now = mDate.getTime();

        return mSharedTime + now - mEndSyncTime;

    }

    public String getTimeString() throws NotSyncedException {
        return new Timestamp(getTime()).toString();
    }

    public void updateSharedTime(long receivedTime) {
        // TODO : when receive message call this

        long startSyncTime = mDate.getTime();

        mEndSyncTime = mDate.getTime();
        mSharedTime = receivedTime + mEndSyncTime - startSyncTime;
    }


    public static class NotSyncedException extends Exception {
    }
}
