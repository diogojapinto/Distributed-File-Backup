package sdis.sharedbackup.utils;

import sdis.sharedbackup.backend.SharedClock;
import sdis.sharedbackup.frontend.ApplicationInterface;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;

public class Log {
    public static void log(final String messg) {
        if (ApplicationInterface.DEBUGG) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    java.util.Date date = new java.util.Date();
                    try {
                        String logMssg = null;
                        try {
                            logMssg = SharedClock.getInstance().getTimeString() + "=>" + messg;
                        } catch (SharedClock.NotSyncedException e) {
                            logMssg = new Timestamp(date.getTime()).toString() + "=>" + messg;
                        }
                        FileWriter fw = new FileWriter("log.log", true);
                        fw.write(logMssg + "\n");
                        fw.close();
                        System.out.println(logMssg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }).start();
        }
    }


}
