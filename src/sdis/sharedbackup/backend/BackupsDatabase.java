package sdis.sharedbackup.backend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidChunkException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;

public class BackupsDatabase implements Serializable {

	private String mBackupFolder;
	private int maxBackupSize; // stored in KB
	private Map<String, SharedFile> mSharedFiles;
	private ArrayList<FileChunk> mSavedChunks;
	private boolean mIsInitialized;

	public BackupsDatabase() {
		mIsInitialized = false;
		maxBackupSize = 0;
		mBackupFolder = "";
		mSharedFiles = new HashMap<String, SharedFile>();
		mSavedChunks = new ArrayList<FileChunk>();
	}

	public int getMaxBackupSize() {
		return maxBackupSize;
	}

	public void removeSharedFile(String fileID) {
		mSharedFiles.remove(fileID);
	}

	public void setAvailSpace(int space) throws InvalidBackupSizeException {
		if (space <= 0) {
			throw new InvalidBackupSizeException();
		}

		maxBackupSize = space;

		checkInitialization();
	}

	public String getChunksDestination() {
		return mBackupFolder;
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

	private void checkInitialization() {
		if (!mBackupFolder.equals("") && maxBackupSize != 0) {
			mIsInitialized = true;
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
	
	private void saveDatabase() {
		try {
			FileOutputStream fileOut = new FileOutputStream(".database.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
			System.out.println("Database Saved");
		} catch (IOException i) {
			System.out.println("Could not save database");
		}
	}
}