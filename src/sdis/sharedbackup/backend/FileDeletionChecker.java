package sdis.sharedbackup.backend;

import java.util.ArrayList;

import sdis.sharedbackup.protocols.FileDeletion;

public class FileDeletionChecker implements Runnable {

	private static final int ONE_MINUTE = 60000;
	private static final int HALF_SECOND = 500;

	@Override
	public void run() {
		try {
			while (true) {
				ArrayList<String> deletedFiles = ConfigsManager.getInstance()
						.getDeletedFiles();

				for (String fileId : deletedFiles) {
					FileDeletion.getInstance().deleteFile(fileId);
					Thread.sleep(HALF_SECOND);
				}

				Thread.sleep(ONE_MINUTE);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
