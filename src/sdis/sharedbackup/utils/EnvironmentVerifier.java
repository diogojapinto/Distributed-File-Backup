package sdis.sharedbackup.utils;

import java.io.File;

public class EnvironmentVerifier {
	
	private EnvironmentVerifier () {
		
	}

	public static boolean isValidFile(String filePath) {

		File validFile = new File(filePath);
		return validFile.exists();
	}
}
