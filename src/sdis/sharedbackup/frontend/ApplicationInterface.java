package sdis.sharedbackup.frontend;

import java.io.File;


public class ApplicationInterface {
	
	private static ApplicationInterface instance = null;
	
	private ApplicationInterface () {
		
	}
	
	public static ApplicationInterface getInstance() {
		if (instance == null) {
			instance = new ApplicationInterface();
		}
		return instance;
	}

}
