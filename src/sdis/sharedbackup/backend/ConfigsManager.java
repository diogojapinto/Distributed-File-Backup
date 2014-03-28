package sdis.sharedbackup.backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.frontend.ApplicationInterface;
import sdis.sharedbackup.utils.EnvironmentVerifier;
import sdis.sharedbackup.utils.Log;

public class ConfigsManager {

	// constants
	private static final String VERSION = "1.0";
	private static final String ENHANCEMENTS_VERSION = "1.24";
	private static final int NR_CONCURRENT_THREADS = 10;

	// static members
	private static ConfigsManager sInstance = null;

	// private members

	private boolean mCheckState;
	private InetAddress mMCaddr = null, mMDBaddr = null, mMDRaddr = null;
	private int mMCport = 0, mMDBport = 0, mMDRport = 0;
	private MulticastControlListener mMCListener;
	private MulticastDataBackupListener mMDBListener;
	private MulticastDataRestoreListener mMDRListener;
	private boolean mDatabaseLoaded = false;
	private BackupsDatabase mDatabase = null;
	private ExecutorService mExecutor = null;
	private Random mRandom;
	private boolean mIsRunning;

	private ConfigsManager() {
		mMCListener = null;
		mMDBListener = null;
		mMDRListener = null;
		mExecutor = Executors.newFixedThreadPool(NR_CONCURRENT_THREADS);
		mRandom = new Random();
		mIsRunning = true;
		mDatabaseLoaded = loadDatabase();
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

	public String getVersion() {
		return VERSION;
	}

	public String getEnhancementsVersion() {
		return ENHANCEMENTS_VERSION;
	}

	public boolean isToCheckState() {
		return mCheckState;
	}

	public boolean setMulticastAddrs(String mcAddr, int mcPort, String mdbAddr,
			int mdbPort, String mdrAddr, int mdrPort) {
		try {
			mMCaddr = InetAddress.getByName(mcAddr);
			mMDBaddr = InetAddress.getByName(mdbAddr);
			mMDRaddr = InetAddress.getByName(mdrAddr);

			mMCport = mcPort;
			mMDBport = mdbPort;
			mMDRport = mdrPort;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public InetAddress getMCAddr() {
		return mMCaddr;
	}

	public int getMCPort() {
		return mMCport;
	}

	public InetAddress getMDBAddr() {
		return mMDBaddr;
	}

	public int getMDBPort() {
		return mMDBport;
	}

	public InetAddress getMDRAddr() {
		return mMDRaddr;
	}

	public int getMDRPort() {
		return mMDRport;
	}

	public String getChunksDestination() {
		return mDatabase.getChunksDestination();
	}

	public FileChunk getSavedChunk(String fileId, int chunkNo) {
		return mDatabase.getSavedChunk(fileId, chunkNo);
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
			startupListeners();
			mExecutor.execute(new FileDeletionChecker());
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

	public SharedFile getNewSharedFileInstance(String filePath, int replication)
			throws FileTooLargeException, FileDoesNotExistsExeption {

		return mDatabase.getNewSharedFileInstance(filePath, replication);
	}

	public void removeByFileId(String fileId) {
		mDatabase.removeByFileId(fileId);
		mDatabase.saveDatabase();
	}

	public void removeSharedFile(String deletedFileID) {
		mDatabase.removeSharedFile(deletedFileID);
		mDatabase.saveDatabase();
	}

	public int getMaxBackupSize() {
		return mDatabase.getMaxBackupSize();
	}

	public SharedFile getFileById(String fileId) {
		return mDatabase.getFileById(fileId);
	}

	public SharedFile getFileByPath(String filePath) {
		return mDatabase.getFileByPath(filePath);
	}

	public void setAvailSpace(int newSpace) throws InvalidBackupSizeException {
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
		mExecutor.shutdown();

	}

	public boolean isAppRunning() {
		return mIsRunning;
	}
	
	public void addSavedChunk(FileChunk chunk) {
		mDatabase.addSavedChunk(chunk);
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

}
