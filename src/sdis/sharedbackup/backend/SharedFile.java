package sdis.sharedbackup.backend;

import java.io.File;
import java.util.ArrayList;
import java.util.PriorityQueue;

import sdis.sharedbackup.backend.ConfigsManager.InvalidChunkException;
import sdis.sharedbackup.utils.Encoder;

/*
 * Create a FileChunk for each chunk for backup
 * 
 */

public class SharedFile {
	public static final int CHUNK_SIZE = 64000;
	public static final int MAX_NR_CHUNKS = 1000000;
	public static final int MAX_FILE_SIZE = CHUNK_SIZE * (MAX_NR_CHUNKS - 1);
	private String mFilePath;
	private String mFileId;
	private ArrayList<FileChunk> mChunkList;
	private int mDesiredReplicationDegree;
	private long mChunkCounter;

	public SharedFile(String filePath, int desiredReplicationDegree)
			throws FileTooLargeException, FileDoesNotExistsExeption {
		mFilePath = filePath;

		verifyDataIntegrity();

		mDesiredReplicationDegree = desiredReplicationDegree;
		mFileId = Encoder.generateBitString(new File(mFilePath));
		mChunkCounter = 0;
		mChunkList = new ArrayList<FileChunk>();
				
		// generate the chunks for this file
		generateChunks();
	}

	// Getters
	
	public ArrayList<FileChunk> getChunkList() {
		return mChunkList;
	}

	public String getFilePath() {
		return mFilePath;
	}

	public String getFileId() {
		return mFileId;
	}

	public int getDesiredReplication() {
		return mDesiredReplicationDegree;
	}

	public void incChunkReplication(int chunkNo) throws InvalidChunkException {

		FileChunk chunk = null;

		try {
			chunk = mChunkList.get(chunkNo);
		} catch (IndexOutOfBoundsException e) {
			throw new ConfigsManager.InvalidChunkException();
		}

		chunk.incCurrentReplication();
	}

	private void verifyDataIntegrity() throws FileTooLargeException, FileDoesNotExistsExeption {

		// verify if file exists

		if (!new File(mFilePath).exists()) {
			throw new FileDoesNotExistsExeption();
		}

		// verify if file size is valid

		if (getFileSize() > MAX_FILE_SIZE) {
			throw new FileTooLargeException();
		}
	}
	
	public boolean exists() {
		return new File(mFilePath).exists();
	}

	public long getFileSize() {
		File thisFile = new File(getFilePath());
		return thisFile.length();
	}

	private void generateChunks() {
		
		long fileSize = getFileSize();
		
		for (int i = 0; i < fileSize; i += CHUNK_SIZE) {
			mChunkList.add(new FileChunk(this, mChunkCounter++));
		}
		
		// verify if there is the need to add the last empty chunk
		if (fileSize % fileSize == 0) {
			mChunkList.add(new FileChunk(this, mChunkCounter++));
		}
	}

	public class FileTooLargeException extends Exception {
	}

	public class FileDoesNotExistsExeption extends Exception {
	}
}
