package sdis.sharedbackup.backend;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;


public class ConfigsManager implements Serializable {

	// constants
	private static final String VERSION = "1.0";

	// static members
	private static ConfigsManager sInstance = null;

	// private members
	private boolean mCheckState;
	private InetAddress mMCaddr = null, mMDBaddr = null, mMDRaddr = null;
	private int mMCport = 0, mMDBport = 0, mMDRport = 0;
	private String mBackupFolder;
	private int maxBackupSize; // stored in KB
	private Map<String, SharedFile> mySharedFiles;
	private ArrayList<FileChunk> savedChunks;
	private boolean mIsInitialized;
	private MulticastControlListener mMCListener;
	private MulticastDataBackupListener mMDBListener;
	private MulticastDataRestoreListener mMDRListener;

	private ConfigsManager() {
		maxBackupSize = 0;
		mBackupFolder = "";
		mIsInitialized = false;
		mMCListener = null;
		mMDBListener = null;
		mMDRListener = null;
	}

	public static ConfigsManager getInstance() {
		if (sInstance == null) {
			sInstance = new ConfigsManager();
		}
		return sInstance;
	}

	// map saving the filepath on this machine and respective SharedFile object
	Map<String, SharedFile> myFiles;

	// map saving the fileId of a shared file and respective SharedFile object
	Map<String, SharedFile> sharedFile;

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
		return mBackupFolder;
	}

	// Setters

	public void setAvailSpace(int space) {
		maxBackupSize = space;

		checkInitialization();
	}

	public void setBackupsDestination(String dest)
			throws InvalidFolderException {
		// TODO
		checkInitialization();
	}

	// Others

	private void checkInitialization() {
		if (!mBackupFolder.equals("") && maxBackupSize != 0) {
			mIsInitialized = true;
		}
	}

	public void init() throws ConfigurationsNotInitializedException {
		if (mIsInitialized) {
			startupListeners();
		} else {
			throw new ConfigurationsNotInitializedException();
		}
	}

	private void startupListeners() {
		if (mMCListener == null) {
			mMCListener = new MulticastControlListener();
			new Thread(mMCListener).start();
		}
		if (mMDBListener == null) {
			mMDBListener = new MulticastDataBackupListener();
			new Thread(mMDBListener).start();
		}
		if (mMDRListener == null) {
			mMDRListener = new MulticastDataRestoreListener();
			new Thread(mMDRListener).start();
		}
	}

	public void incChunkReplication(String fileId, int chunkNo)
			throws InvalidChunkException {
		SharedFile file = mySharedFiles.get(fileId);
		if (file != null) {
			file.incChunkReplication(chunkNo);
		} else { 
			throw new InvalidChunkException();
		}
	}

	public static class ConfigurationsNotInitializedException extends Exception {
	}

	public static class InvalidFolderException extends Exception {
	}

	public static class InvalidChunkException extends Exception {
	}
}