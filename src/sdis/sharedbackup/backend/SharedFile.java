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

	private String mFilePath;
	private String mFileId;
	private ArrayList<FileChunk> mChunkList;
	private int mDesiredReplicationDegree;
	private int mChunkCounter;

	public SharedFile(String filePath, int desiredReplicationDegree) {
		mFilePath = filePath;
		mDesiredReplicationDegree = desiredReplicationDegree;
		mFileId = Encoder.generateBitString(new File(mFilePath));
		mChunkCounter = 0;
		
		// TODO: generate chunks
	}

	// Getters

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
		} catch(IndexOutOfBoundsException e) {
			throw new ConfigsManager.InvalidChunkException();
		}
		
		chunk.incCurrentReplication();
	}
	
	public long getFileSize() {
		File thisFile = new File(getFilePath());
		return thisFile.length();
	}
}
