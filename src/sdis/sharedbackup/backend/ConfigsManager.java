package sdis.sharedbackup.backend;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.imageio.stream.FileImageInputStream;

import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;

public class ConfigsManager {

	// constants
	private static final String VERSION = "1.0";

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

	private ConfigsManager() {
		mMCListener = null;
		mMDBListener = null;
		mMDRListener = null;
	}

	public static ConfigsManager getInstance() {
		if (sInstance == null) {
			sInstance = new ConfigsManager();
			sInstance.mDatabaseLoaded = sInstance.loadDatabase();
		}
		return sInstance;
	}
	
	public boolean getDatabaseStatus(){
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

	// Setters
	
	public void setBackupsDestination(String dirPath) throws InvalidFolderException {
	database.setBackupsDestination(dirPath);		
	}

	// Others

	public void init() throws ConfigurationsNotInitializedException {
		if (mIsInitialized) {
			startupListeners();
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

	public SharedFile getNewSharedFileInstance(String filePath, int replication) throws FileTooLargeException, FileDoesNotExistsExeption {
		
		return database.getNewSharedFileInstance(filePath, replication);
	}

	public void removeSharedFile(String deletedFileID) {
		database.removeSharedFile(deletedFileID);		
	}

	public int getMaxBackupSize() {
		return database.getMaxBackupSize();
	}

	public void setAvailSpace(int newSpace) throws InvalidBackupSizeException {
		database.setAvailSpace(newSpace);
	}

	public void incChunkReplication(String fileId, int chunkNo) throws InvalidChunkException {
		database.incChunkReplication(fileId, chunkNo);
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