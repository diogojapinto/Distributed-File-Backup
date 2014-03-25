package sdis.sharedbackup.utils;

import java.io.File;

public class EnvironmentVerifier {

	private EnvironmentVerifier() {

	}

	public static long getFolderSize(String folderPath) {

		File folder = new File(folderPath);

		if (folder.isDirectory()) {
			return folder.getTotalSpace();
		}

		return -1;
	}
}
