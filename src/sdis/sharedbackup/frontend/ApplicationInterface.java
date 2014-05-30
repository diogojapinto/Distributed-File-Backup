package sdis.sharedbackup.frontend;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;
import sdis.sharedbackup.backend.ConfigsManager.FileAlreadySaved;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.functionality.FileBackup;
import sdis.sharedbackup.functionality.FileRestore;
import sdis.sharedbackup.protocols.FileDeletion;
import sdis.sharedbackup.protocols.SpaceReclaiming;

import java.util.ArrayList;

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
    public void setAvailableDiskSpace(long space)
            throws InvalidBackupSizeException {
        ConfigsManager.getInstance().setAvailSpace(space * 10000);
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
            throws FileTooLargeException, FileDoesNotExistsExeption,
            FileAlreadySaved {
        SharedFile file = ConfigsManager.getInstance()
                .getNewSharedFileInstance(filePath, replication);

        FileBackup.getInstance().saveFile(file);

        return true;
    }

    public boolean restoreFileByPath(String oldFilePath) {
        SharedFile file = ConfigsManager.getInstance().getFileByPath(
                oldFilePath);
        return restoreFileById(file.getFileId());
    }

    public boolean restoreFileById(String fileId) {
        SharedFile file = ConfigsManager.getInstance().getFileById(fileId);

        return FileRestore.getInstance().restoreFile(file);
    }

    public boolean deleteFile(String filepath) throws FileDoesNotExistsExeption {

        String deletedFileID = ConfigsManager.getInstance().getFileIdByPath(
                filepath);
        FileDeletion.getInstance().deleteFile(deletedFileID);
        ConfigsManager.getInstance().removeSharedFile(deletedFileID);
        return true;
    }

    public boolean setNewSpace(int newSpace) {
        try {
            ConfigsManager.getInstance().setAvailSpace(newSpace * 1000);
        } catch (InvalidBackupSizeException e) {
            System.out.println("The selected size is invalid");
        }
        if (ConfigsManager.getInstance().getMaxBackupSize() < ConfigsManager
                .getInstance().getBackupDirActualSize()) {
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

    public boolean getSharedDatabaseStatus() {
        return ConfigsManager.getInstance().getDatabaseStatus();
    }

    public ArrayList<String> getRestorableFiles() {
        return ConfigsManager.getInstance().getRestorableFiles();
    }

    public ArrayList<String> getDeletableFiles() {
        return ConfigsManager.getInstance().getDeletableFiles();
    }

    public void terminate() {
        ConfigsManager.getInstance().saveDatabase();
        ConfigsManager.getInstance().terminate();
    }
}
