package sdis.sharedbackup.utils;

import java.sql.Timestamp;

import sdis.sharedbackup.frontend.ApplicationInterface;

public class Log {
	public static void log(String messg) {
		if (ApplicationInterface.DEBUGG) {
			java.util.Date date = new java.util.Date();
			System.out.println(new Timestamp(date.getTime()) + "=>" + messg);
		}
	}
}
