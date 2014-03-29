package sdis.sharedbackup.utils;

import java.io.File;

public class EnvironmentVerifier {

	private EnvironmentVerifier() {

	}

	public static long getFolderSize(String folderPath) {

		File folder = new File(folderPath);

		if (folder.isDirectory()) {

			long length = 0;
			for (File file : folder.listFiles()) {
				if (file.isFile())
					length += file.length();
				else
					length += getFolderSize(file.getAbsolutePath());
			}
			return length;
		} else {
			return -1;
		}
	}
}
