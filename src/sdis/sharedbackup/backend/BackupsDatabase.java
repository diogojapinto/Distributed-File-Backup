package sdis.sharedbackup.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.omg.CORBA.Environment;

import com.sun.xml.internal.messaging.saaj.soap.Envelope;

import sdis.sharedbackup.backend.ConfigsManager.FileAlreadySaved;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidChunkException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.utils.EnvironmentVerifier;
import sdis.sharedbackup.utils.Log;

public class BackupsDatabase implements Serializable {

	public static final String FILE = ".database.ser";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1;

    private InetAddress communicationInterface = null;
	private InetAddress mMCaddr = null, mMDBaddr = null, mMDRaddr = null;
	private int mMCport = 0, mMDBport = 0, mMDRport = 0;
	private String mBackupFolder;
	private long maxBackupSize; // stored in B
	private Map<String, SharedFile> mSharedFiles; // my shared files
	private ArrayList<FileChunk> mSavedChunks; // chunks from other users
	private boolean mIsInitialized;
	private Map<String, Integer> mDeletedFiles;

	public BackupsDatabase() {
		mIsInitialized = false;
		maxBackupSize = 0;
		mBackupFolder = "";
		mSharedFiles = new HashMap<String, SharedFile>();
		mSavedChunks = new ArrayList<FileChunk>();
		mDeletedFiles = new HashMap<String, Integer>();
	}

	public long getMaxBackupSize() {
		return maxBackupSize;
	}

	public void removeSharedFile(String fileID) {
		mDeletedFiles.put(fileID, mSharedFiles.get(fileID)
				.getDesiredReplication());
		mSharedFiles.remove(fileID);
	}

	public synchronized void setAvailSpace(long space)
			throws InvalidBackupSizeException {
		if (space <= 0) {
			throw new InvalidBackupSizeException();
		}

		maxBackupSize = space;

		checkInitialization();
	}

	public String getChunksDestination() {
		return mBackupFolder;
	}

	public synchronized void setBackupsDestination(String dest)
			throws InvalidFolderException {

		File destination = new File(dest);
		if (!destination.exists()) {

			destination.mkdir();
			mBackupFolder = destination.getAbsolutePath();
			String os = System.getProperty("os.name");

			if (os.contains("Windows")) {
				mBackupFolder += "\\";
			} else {
				mBackupFolder += "/";
			}

			checkInitialization();

		} else if (destination.isDirectory()) {
			mBackupFolder = destination.getAbsolutePath();
			if (EnvironmentVerifier.getFolderSize(mBackupFolder) > 0) {
				throw new InvalidFolderException();
			}
			String os = System.getProperty("os.name");

			if (os.contains("Windows")) {
				mBackupFolder += "\\";
			} else {
				mBackupFolder += "/";
			}

			checkInitialization();
		} else {
			throw new InvalidFolderException();
		}
	}

	private void checkInitialization() {
		if (!mBackupFolder.equals("") && maxBackupSize != 0) {
			mIsInitialized = true;
			saveDatabase();
		}
	}

	public boolean isInitialized() {
		return mIsInitialized;
	}

	public synchronized void incChunkReplication(String fileId, int chunkNo)
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
		synchronized (mSavedChunks) {
			mSavedChunks.add(chunk);
		}
		return chunk;
	}

	public SharedFile getNewSharedFileInstance(String filePath,
			int replicationDegree) throws FileTooLargeException,
			FileDoesNotExistsExeption, FileAlreadySaved {
		SharedFile file = new SharedFile(filePath, replicationDegree);
		if (mSharedFiles.containsKey(file.getFileId())) {
			throw new ConfigsManager.FileAlreadySaved();
		}
		mSharedFiles.put(file.getFileId(), file);
		return file;
	}

	public void saveDatabase() {
		try {
			FileOutputStream fileOut = new FileOutputStream(".database.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
			
			Log.log("Database Saved");
		} catch (IOException i) {
			Log.log("Could not save database");
		}
	}

	public FileChunk getSavedChunk(String fileId, int chunkNo) {

		FileChunk retChunk = null;

		synchronized (mSavedChunks) {
			for (FileChunk chunk : mSavedChunks) {
				if (chunk.getFileId().equals(fileId)
						&& chunk.getChunkNo() == chunkNo) {
					retChunk = chunk;
					break;
				}
			}
		}

		if (retChunk == null) {
			for (SharedFile file : mSharedFiles.values()) {
				if (file.getFileId().equals(fileId)) {
					// I have the chunk in my own file
					retChunk = file.getChunkList().get(chunkNo);
					break;
				}
			}
		}

		return retChunk;
	}

	public boolean removeSingleChunk(ChunkRecord record) {
		synchronized (mSavedChunks) {
			for (FileChunk chunk : mSavedChunks) {
				if (chunk.getFileId().equals(record.fileId)
						&& chunk.getChunkNo() == record.chunkNo) {
					mSavedChunks.remove(chunk);
					if (!chunk.removeData()) {
						return false;
					} else {
						return true;
					}
				}
			}
		}

		return false;
	}

	public ArrayList<FileChunk> getSavedChunks() {
		return mSavedChunks;
	}

	public boolean fileIsTracked(String fileId) {

		return mSharedFiles.containsKey(fileId);
	}

	public synchronized ArrayList<String> getDeletedFilesIds() {
		ArrayList<String> filesIds = new ArrayList<String>();
		for (String key : mDeletedFiles.keySet()) {
			filesIds.add(key);
		}

		return filesIds;
	}

	public synchronized ArrayList<String> getFilesDeletedFromFileSystem() {

		ArrayList<String> delFiles = new ArrayList<String>();
		for (SharedFile file : mSharedFiles.values()) {
			File f = new File(file.getFilePath());
			if (!f.exists()) {
				delFiles.add(file.getFilePath());
			}
		}

		return delFiles;
	}

	public synchronized void decDeletedFileCount(String fileId) {
		Integer currReplication = mDeletedFiles.get(fileId);
		if (currReplication != null) {
			int newReplication = mDeletedFiles.get(fileId) - 1;
			if (newReplication <= 0) {
				mDeletedFiles.remove(fileId);
			} else {
				synchronized (mDeletedFiles) {
					mDeletedFiles.put(fileId, newReplication);
				}
			}
		}
	}

	public SharedFile getFileByPath(String filePath) {
		for (SharedFile file : mSharedFiles.values()) {
			if (file.getFilePath().equals(filePath)) {
				return file;
			}
		}
		return null;
	}

	public SharedFile getFileById(String fileId) {
		return mSharedFiles.get(fileId);
	}

	public void addSavedChunk(FileChunk chunk) {
		synchronized (mSavedChunks) {
			mSavedChunks.add(chunk);
		}
		Log.log("Saved a CHUNK " + chunk.getFileId() + " " + chunk.getChunkNo());
	}

	public boolean isMyFile(String fileId) {
		if (mSharedFiles.containsKey(fileId)) {
			return true;
		} else {
			return false;
		}
	}

	public ArrayList<String> getDeletableFiles() {
		ArrayList<String> retFiles = new ArrayList<String>();

		for (SharedFile file : mSharedFiles.values()) {
			retFiles.add(file.getFilePath());
		}

		return retFiles;
	}

	public String getFileIdByPath(String path) {
		for (SharedFile file : mSharedFiles.values()) {
			if (path.equals(file.getFilePath())) {
				return file.getFileId();
			}
		}
		return null;
	}

	public boolean deleteChunksOfFile(String fileId) {
		boolean foundChunk = false;
		synchronized (mSavedChunks) {
			for (FileChunk chunk : mSavedChunks) {
				if (chunk.getFileId().equals(fileId)) {
					chunk.removeData();
					// mSavedChunks.remove(chunk);
					foundChunk = true;
				}
			}
		}

		return foundChunk;
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

    public InetAddress getInterface() {
        return communicationInterface;
    }

    public void setInterface(String intrfc) throws SocketException {
        communicationInterface = NetworkInterface.getByName(intrfc).getInetAddresses().nextElement();
    }
}
