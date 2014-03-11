package com.sdis.sharedbackup.backend;

import java.util.PriorityQueue;

/*
 * Create a FileChunk for each chunk for backup
 * 
 */

public class SharedFile {
	private String mFilePath;
	private String mFileId;
	private PriorityQueue<FileChunk> mChunkList;
	private int mDesiredReplicationDegree;
	
	public SharedFile(String filePath, int desiredReplicationDegree) {
		this.mFilePath = filePath;
		this.mDesiredReplicationDegree = desiredReplicationDegree;
	}
	
	private void generateFileId() {
		// TODO
	}
	
	public String getFileId() {
		return mFileId;
	}
}
