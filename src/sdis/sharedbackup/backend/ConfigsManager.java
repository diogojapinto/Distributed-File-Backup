package sdis.sharedbackup.backend;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	private String mBackupFolder;
	private int maxBackupSize; // stored in KB
	private Map<String, SharedFile> mSharedFiles;
	private ArrayList<FileChunk> mSavedChunks;
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

		mSharedFiles = new HashMap<String, SharedFile>();
		mSavedChunks = new ArrayList<FileChunk>();
	}

	public static ConfigsManager getInstance() {
		if (sInstance == null) {
			sInstance = new ConfigsManager();
		}
		return sInstance;
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
		return mBackupFolder;
	}

	// Setters

	public void setAvailSpace(int space) throws InvalidBackupSizeException {
		if (space <= 0) {
			throw new InvalidBackupSizeException();
		}

		maxBackupSize = space;

		checkInitialization();
	}

	public void setBackupsDestination(String dest)
			throws InvalidFolderException {

		File destination = new File(dest);

		if (destination.isDirectory()) {
			mBackupFolder = destination.getAbsolutePath();
			mBackupFolder += new String("\\");
			checkInitialization();
		} else {
			throw new InvalidFolderException();
		}
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

	public void incChunkReplication(String fileId, int chunkNo)
			throws InvalidChunkException {
		SharedFile file = mSharedFiles.get(fileId);
		if (file != null) {
			// This is the owner machine of chunk's parent file
			file.incChunkReplication(chunkNo);
		} else {
			// It is a chunk saved in a backup operation
			FileChunk chunk = null;
			int nrSavedChunks = mSavedChunks.size();
			for (int i = 0; i < nrSavedChunks; i++) {
				chunk = mSavedChunks.get(i);
				if (chunk.getFileId().equals(fileId)
						&& chunk.getChunkNo() == chunkNo) {
					break;
				}
			}

			if (chunk != null) {
				chunk.incCurrentReplication();

			} else {
				throw new InvalidChunkException();
			}
		}
	}

	/*
	 * returns a chunk for saving in this computer
	 */

	public FileChunk getNewChunkForSavingInstance(String fileId, int chunkNo,
			int desiredReplication) {
		FileChunk chunk = new FileChunk(fileId, chunkNo, desiredReplication);
		mSavedChunks.add(chunk);
		return chunk;
	}

	public SharedFile getNewSharedFileInstance(String filePath,
			int replicationDegree) throws FileTooLargeException,
			FileDoesNotExistsExeption {
		SharedFile file = new SharedFile(filePath, replicationDegree);
		mSharedFiles.put(file.getFileId(), file);
		return file;
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