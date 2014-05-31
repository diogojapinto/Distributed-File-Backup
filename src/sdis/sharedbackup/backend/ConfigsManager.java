package sdis.sharedbackup.backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.protocols.AccessLevel;
import sdis.sharedbackup.protocols.Election;
import sdis.sharedbackup.protocols.SharedClock;
import sdis.sharedbackup.protocols.SharedDatabase;
import sdis.sharedbackup.utils.EnvironmentVerifier;
import sdis.sharedbackup.utils.Log;

public class ConfigsManager {

	// constants
	private static final int NR_CONCURRENT_THREADS = 12;

	// static members
	private static ConfigsManager sInstance = null;

	// private members

	private boolean mCheckState;
	private MulticastControlListener mMCListener;
	private MulticastDataBackupListener mMDBListener;
	private MulticastDataRestoreListener mMDRListener;
	private boolean mDatabaseLoaded = false, sDatabaseLoaded = false;
	private BackupsDatabase mDatabase = null;
    private SharedDatabase sDatabase = null;
	private ExecutorService mExecutor = null;
	private Random mRandom;
	private boolean mIsRunning;
    private long beginningTime;
    private User user;

	private ConfigsManager() {
		mMCListener = null;
		mMDBListener = null;
		mMDRListener = null;
		mExecutor = Executors.newFixedThreadPool(NR_CONCURRENT_THREADS);
		mRandom = new Random();
		mIsRunning = true;
		mDatabaseLoaded = loadDatabase();
        sDatabaseLoaded = loadSharedDatabase();
	}

	public static ConfigsManager getInstance() {
		if (sInstance == null) {
			sInstance = new ConfigsManager();
		}
		return sInstance;
	}

	public boolean getDatabaseStatus() {
		return mDatabaseLoaded;
	}

	private boolean loadDatabase() {
		try {
			FileInputStream fileIn = new FileInputStream(BackupsDatabase.FILE);
			ObjectInputStream in = new ObjectInputStream(fileIn);

			try {
				mDatabase = (BackupsDatabase) in.readObject();
			} catch (ClassNotFoundException e) {

				Log.log("Error while reading from saved database. Starting fresh");

				mDatabase = new BackupsDatabase();
			}

			Log.log("Loaded database");

			in.close();
			fileIn.close();

		} catch (FileNotFoundException e) {

			Log.log("Fresh database");

			mDatabase = new BackupsDatabase();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

    private boolean loadSharedDatabase() {
        try {
            FileInputStream fileIn = new FileInputStream(SharedDatabase.FILE);
            ObjectInputStream in = new ObjectInputStream(fileIn);

            try {
                sDatabase = (SharedDatabase) in.readObject();
            } catch (ClassNotFoundException e) {

                Log.log("Error while reading from saved shared database. Starting fresh");

                sDatabase = new SharedDatabase();
            }

            Log.log("Loaded shared database");

            in.close();
            fileIn.close();

        } catch (FileNotFoundException e) {

            Log.log("Fresh shared database");

            sDatabase = new SharedDatabase();
            return false;
        } catch (IOException e) {
            System.out.println("pig");
            e.printStackTrace();
        }
        return true;
    }

    public SharedDatabase getSDatabase() { return sDatabase; }

	public boolean isToCheckState() {
		return mCheckState;
	}

	public boolean setMulticastAddrs(String mcAddr, int mcPort, String mdbAddr,
			int mdbPort, String mdrAddr, int mdrPort) {
		return mDatabase.setMulticastAddrs(mcAddr, mcPort, mdbAddr, mdbPort, mdrAddr, mdrPort);
	}

	public InetAddress getMCAddr() {
		return mDatabase.getMCAddr();
	}

	public int getMCPort() {
		return mDatabase.getMCPort();
	}

	public InetAddress getMDBAddr() {
		return mDatabase.getMDBAddr();
	}

	public int getMDBPort() {
		return mDatabase.getMDBPort();
	}

	public InetAddress getMDRAddr() {
		return mDatabase.getMDRAddr();
	}

	public int getMDRPort() {
		return mDatabase.getMDRPort();
	}

	public String getChunksDestination() {
		return mDatabase.getChunksDestination();
	}

	public FileChunk getSavedChunk(String fileId, int chunkNo) {
		return mDatabase.getSavedChunk(fileId, chunkNo);
	}

	public boolean isMyFile(String fileId) {
		return mDatabase.isMyFile(fileId);
	}

	public FileChunk getNextDispensableChunk() {
		ArrayList<FileChunk> savedChunks = mDatabase.getSavedChunks();

		for (FileChunk chunk : savedChunks) {
			if (chunk.getCurrentReplicationDeg() > chunk
					.getDesiredReplicationDeg()) {
				return chunk;
			}
		}

		// else there is no chunk with replication degree higher than desired

		FileChunk retChunk = null;

		do {
			retChunk = savedChunks.get(mRandom.nextInt(savedChunks.size()));
		} while (retChunk.getCurrentReplicationDeg() <= 0);

		return retChunk;
	}

	public long getBackupDirActualSize() {
		return EnvironmentVerifier.getFolderSize(mDatabase
				.getChunksDestination());
	}

	public Executor getExecutor() {
		return mExecutor;
	}

	// Setters

	public void setBackupsDestination(String dirPath)
			throws InvalidFolderException {
		mDatabase.setBackupsDestination(dirPath);
	}

	// Others

	public void init() throws ConfigurationsNotInitializedException {
		if (mDatabase.isInitialized()) {
            Election.getInstance().sendStartup();
		} else {
			throw new ConfigurationsNotInitializedException();
		}
		mDatabase.saveDatabase();
	}

    public void enterMainStage() throws ConfigurationsNotInitializedException {
        if (mDatabase.isInitialized()) {
            mMCListener = null;
            mMDBListener = null;
            mMDRListener = null;
            mExecutor.shutdownNow();

            mExecutor = Executors.newFixedThreadPool(NR_CONCURRENT_THREADS);
            startupListeners();
            mExecutor.execute(new FileDeletionChecker());
            Date d = new Date();
            beginningTime = d.getTime();
            SharedClock.getInstance();
            sDatabase.createNameSpace(getChunksDestination());
        } else {
            throw new ConfigurationsNotInitializedException();
        }
        mDatabase.saveDatabase();
    }

	private void startupListeners() {
		if (mMCListener == null) {
			mMCListener = MulticastControlListener.getInstance();
			mExecutor.execute(mMCListener);
		}
		if (mMDBListener == null) {
			mMDBListener = MulticastDataBackupListener.getInstance();
			mExecutor.execute(mMDBListener);
		}
		if (mMDRListener == null) {
			mMDRListener = MulticastDataRestoreListener.getInstance();
			mExecutor.execute(mMDRListener);
		}
	}

	public SharedFile getNewSharedFileInstance(String filePath, int replication, AccessLevel al)
			throws FileTooLargeException, FileDoesNotExistsExeption,
			FileAlreadySaved {

		return mDatabase.getNewSharedFileInstance(filePath, replication, al);
	}

	public void removeSharedFile(String deletedFileID) {
		mDatabase.removeSharedFile(deletedFileID);
		mDatabase.saveDatabase();
	}

	public long getMaxBackupSize() {
		return mDatabase.getMaxBackupSize();
	}

	public SharedFile getFileById(String fileId) {
		return mDatabase.getFileById(fileId);
	}

	public SharedFile getFileByPath(String filePath) {
		return mDatabase.getFileByPath(filePath);
	}

	public void setAvailSpace(long newSpace) throws InvalidBackupSizeException {
		mDatabase.setAvailSpace(newSpace);
	}

	public void incChunkReplication(String fileId, int chunkNo)
			throws InvalidChunkException {
		mDatabase.incChunkReplication(fileId, chunkNo);
		mDatabase.saveDatabase();
	}

	public boolean deleteChunk(ChunkRecord record) {
		boolean state = mDatabase.removeSingleChunk(record);
		mDatabase.saveDatabase();

		return state;
	}

	public boolean fileIsTracked(String fileId) {
		return mDatabase.fileIsTracked(fileId);
	}

	public ArrayList<String> getRestorableFiles() {
		return mDatabase.getFilesDeletedFromFileSystem();
	}

	public ArrayList<String> getDeletedFiles() {
		return mDatabase.getDeletedFilesIds();
	}

	public void decDeletedFileReplication(String fileId) {

		mDatabase.decDeletedFileCount(fileId);
		mDatabase.saveDatabase();
	}

	public void saveDatabase() {
		mDatabase.saveDatabase();
	}

	public void terminate() {
		mIsRunning = false;
		mExecutor.shutdownNow();
	}

	public boolean isAppRunning() {
		return mIsRunning;
	}

	public void addSavedChunk(FileChunk chunk) {
		mDatabase.addSavedChunk(chunk);
		saveDatabase();
	}

	public ArrayList<String> getDeletableFiles() {
		return mDatabase.getDeletableFiles();
	}

	public String getFileIdByPath(String path) {
		return mDatabase.getFileIdByPath(path);
	}

	public boolean removeChunksOfFile(String fileId) {
		boolean ret = mDatabase.deleteChunksOfFile(fileId);
		saveDatabase();
		return ret;
	}

    public void setInterface(String selectedInterface) throws SocketException {
        mDatabase.setInterface(selectedInterface);
    }

    public InetAddress getInterface() {
        try {
            return mDatabase.getInterface();
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getInterfaceName() {
        return mDatabase.getInterfaceName();
    }

    public long getUpTime() {
        Date d = new Date();
        return d.getTime() - beginningTime;
    }

    public boolean login(String username, String password) {
        return (user = sDatabase.login(username, password)) != null;
    }

    public User getUser() {
        return user;
    }

    /*
         * Exceptions
         */
	public static class ConfigurationsNotInitializedException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}

	public static class InvalidFolderException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}

	public static class InvalidBackupSizeException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}

	public static class InvalidChunkException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}

	public static class FileAlreadySaved extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}

}
