package sdis.sharedbackup.backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.stream.FileImageInputStream;

import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.utils.EnvironmentVerifier;

public class ConfigsManager {

	// constants
	private static final String VERSION = "1.0";
	private static final String ENHANCEMENTS_VERSION = "1.24";

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
	private boolean mIsInitialized = false;
	private BackupsDatabase database = null;

	private Random random;

	private ConfigsManager() {
		mMCListener = null;
		mMDBListener = null;
		mMDRListener = null;
		random = new Random();
	}

	public static ConfigsManager getInstance() {
		if (sInstance == null) {
			sInstance = new ConfigsManager();
			sInstance.mDatabaseLoaded = sInstance.loadDatabase();
		}
		return sInstance;
	}

	public boolean getDatabaseStatus() {
		return mDatabaseLoaded;
	}

	private boolean loadDatabase() {
		try {
			FileInputStream fileIn = new FileInputStream(".database.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			database = (BackupsDatabase) in.readObject();
			in.close();
			fileIn.close();

		} catch (FileNotFoundException e) {
			database = new BackupsDatabase();
			return false;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
		return database.getChunksDestination();
	}

	public FileChunk getSavedChunk(String fileId, int chunkNo) {
		return database.getSavedChunk(fileId, chunkNo);
	}

	public FileChunk getNextDispensableChunk() {
		ArrayList<FileChunk> savedChunks = database.getSavedChunks();

		for (FileChunk chunk : savedChunks) {
			if (chunk.getCurrentReplicationDeg() > chunk
					.getDesiredReplicationDeg()) {
				return chunk;
			}
		}

		// else there is no chunk with replication degree higher than desired

		FileChunk retChunk = null;

		do {
			retChunk = savedChunks.get(random.nextInt(savedChunks.size()));
		} while (retChunk.getCurrentReplicationDeg() <= 0);

		return retChunk;
	}

	public long getBackupDirActualSize() {
		return EnvironmentVerifier.getFolderSize(database
				.getChunksDestination());
	}

	// Setters

	public void setBackupsDestination(String dirPath)
			throws InvalidFolderException {
		database.setBackupsDestination(dirPath);
	}

	// Others

	public void init() throws ConfigurationsNotInitializedException {
		if (mIsInitialized) {
			startupListeners();
			new Thread(new FileDeletionChecker()).start();
		} else {
			throw new ConfigurationsNotInitializedException();
		}
	}

	private void startupListeners() {
		if (mMCListener == null) {
			mMCListener = MulticastControlListener.getInstance();
			new Thread(mMCListener).start();
		}
		if (mMDBListener == null) {
			mMDBListener = MulticastDataBackupListener.getInstance();
			new Thread(mMDBListener).start();
		}
		if (mMDRListener == null) {
			mMDRListener = MulticastDataRestoreListener.getInstance();
			new Thread(mMDRListener).start();
		}
	}

	public SharedFile getNewSharedFileInstance(String filePath, int replication)
			throws FileTooLargeException, FileDoesNotExistsExeption {

		return database.getNewSharedFileInstance(filePath, replication);
	}

	public void removeByFileId(String fileId) {
		database.removeByFileId(fileId);
		database.saveDatabase();
	}

	public void removeSharedFile(String deletedFileID) {
		database.removeSharedFile(deletedFileID);
		database.saveDatabase();
	}

	public int getMaxBackupSize() {
		return database.getMaxBackupSize();
	}

	public void setAvailSpace(int newSpace) throws InvalidBackupSizeException {
		database.setAvailSpace(newSpace);
		database.saveDatabase();
	}

	public void incChunkReplication(String fileId, int chunkNo)
			throws InvalidChunkException {
		database.incChunkReplication(fileId, chunkNo);
		database.saveDatabase();
	}

	public boolean deleteChunk(ChunkRecord record) {
		database.saveDatabase();
		return database.removeSingleChunk(record);
	}

	public boolean fileIsTracked(String fileId) {
		return database.fileIsTracked(fileId);
	}

	public ArrayList<String> getDeletedFiles() {
		return database.getDeletedFilesIds();
	}

	public void decDeletedFileReplication(String fileId) {
		database.decDeletedFileCount(fileId);
		database.saveDatabase();
	}

	private void saveDatabase(){
		database.saveDatabase();
	}
	/*
	 * Exceptions
	 */
	public static class ConfigurationsNotInitializedException extends Exception {
	}

	public static class InvalidFolderException extends Exception {
	}

	public static class InvalidBackupSizeException extends Exception {
	}

	public static class InvalidChunkException extends Exception {
	}

}