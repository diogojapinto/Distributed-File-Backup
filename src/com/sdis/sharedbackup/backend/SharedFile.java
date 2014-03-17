package com.sdis.sharedbackup.backend;

import java.util.PriorityQueue;

/*
 * Create a FileChunk for each chunk for backup
 * 
 */

public class SharedFile {
	public static final int CHUNK_SIZE = 64000;
	
	private String mFilePath;
	private String mFileId;
	private PriorityQueue<FileChunk> mChunkList;
	private int mDesiredReplicationDegree;
	
	public SharedFile(String filePath, int desiredReplicationDegree) {
		this.mFilePath = filePath;
		this.mDesiredReplicationDegree = desiredReplicationDegree;
		
		this.mFileId = generateFileId(mFilePath);
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
	
	private static String generateFileId(String filePath) {
		
		return null;
	}
}
