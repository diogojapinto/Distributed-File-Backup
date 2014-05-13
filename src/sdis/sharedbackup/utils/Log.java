package sdis.sharedbackup.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;

import sdis.sharedbackup.frontend.ApplicationInterface;

public class Log {
	public static void log(final String messg) {
		if (ApplicationInterface.DEBUGG) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					java.util.Date date = new java.util.Date();
					try {
						FileWriter fw = new FileWriter("log.log", true);
						fw.write(new Timestamp(date.getTime()) + "=>" + messg);
						System.out.println(new Timestamp(date.getTime()) + "=>"
								+ messg);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}).start();
		}
	}
}
