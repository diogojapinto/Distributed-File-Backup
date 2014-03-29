package sdis.sharedbackup.frontend;

import java.io.File;
import java.util.ArrayList;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.functionality.FileBackup;
import sdis.sharedbackup.functionality.FileRestore;
import sdis.sharedbackup.protocols.SpaceReclaiming;
import sdis.sharedbackup.protocols.FileDeletion;

public class ApplicationInterface {
	
	public static boolean DEBUGG = true;

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
		ConfigsManager.getInstance().setAvailSpace(space*1000);
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

		FileBackup.getInstance().saveFile(file);

		return true;
	}

	public boolean restoreFileByPath(String oldFilePath) {
		SharedFile file = ConfigsManager.getInstance().getFileByPath(oldFilePath);
		return restoreFileById(file.getFileId());
	}

	public boolean restoreFileById(String fileId) {
		SharedFile file = ConfigsManager.getInstance().getFileById(fileId);
		
		return FileRestore.getInstance().restoreFile(file);
	}

	public boolean deleteFile(String filepath) throws FileDoesNotExistsExeption {
		File f = new File(filepath);
		String deletedFileID = sdis.sharedbackup.utils.Encoder
				.generateBitString(f);
		if (ConfigsManager.getInstance().fileIsTracked(deletedFileID)) {
			FileDeletion.getInstance().deleteFile(deletedFileID);
			ConfigsManager.getInstance().removeSharedFile(deletedFileID);
			return true;
		} else {
			System.out.println("File is not tracked");
			return false;
		}
	}

	public boolean setNewSpace(int newSpace) {
		if (newSpace >= ConfigsManager.getInstance().getMaxBackupSize()) {
			try {
				ConfigsManager.getInstance().setAvailSpace(newSpace);
			} catch (InvalidBackupSizeException e) {
				System.out.println("The selected size is invalid");
			}
		} else {

			do {
				FileChunk chunk = ConfigsManager.getInstance()
						.getNextDispensableChunk();
				SpaceReclaiming.getInstance().reclaimSpace(chunk);
			} while (ConfigsManager.getInstance().getBackupDirActualSize() > ConfigsManager
					.getInstance().getMaxBackupSize());
		}

		return false;
	}

	public void startConfigsManager() {
		ConfigsManager.getInstance();
	}

	public boolean getDatabaseStatus() {
		return ConfigsManager.getInstance().getDatabaseStatus();
	}

	public ArrayList<String> getRestorableFiles() {
		return ConfigsManager.getInstance().getRestorableFiles();
	}
	
	public void terminate() {
		ConfigsManager.getInstance().saveDatabase();
		ConfigsManager.getInstance().terminate();
	}
}
