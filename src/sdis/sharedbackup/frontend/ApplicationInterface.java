package sdis.sharedbackup.frontend;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.SharedFile;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.protocols.ChunkBackup;

public class ApplicationInterface {

	private static ApplicationInterface instance = null;

	private ApplicationInterface() {
	}

	public static ApplicationInterface getInstance() {
		if (instance == null) {
			instance = new ApplicationInterface();
		}
		return instance;
	}

	/*
	 * Functions responsible for initialising the service
	 */
	public void setAvailableDiskSpace(int space)
			throws InvalidBackupSizeException {
		ConfigsManager.getInstance().setAvailSpace(space);
	}

	public void setDestinationDirectory(String dirPath)
			throws InvalidFolderException {
		ConfigsManager.getInstance().setBackupsDestination(dirPath);
	}

	/*
	 * Function to be called after proper initialization
	 */
	public void startupService() throws ConfigurationsNotInitializedException {
		ConfigsManager.getInstance().init();
	}

	/*
	 * Provides the service of backup. Returns the backed-up file id.
	 */
	public boolean backupFile(String filePath, int replication)
			throws FileTooLargeException, FileDoesNotExistsExeption {
		SharedFile file = ConfigsManager.getInstance()
				.getNewSharedFileInstance(filePath, replication);

		ChunkBackup.getInstance().saveFile(file);

		return true;
	}

	public boolean restoreFile(String fileId) {
		return false;
	}

	public boolean deleteFile() {
		return false;
	}

	public boolean freeSpace() {
		return false;
	}

}
