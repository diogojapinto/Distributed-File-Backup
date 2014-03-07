package com.sdis.sharedbackup.backend;

import java.util.PriorityQueue;

public class SharedFile {
	private String fileId;
	private PriorityQueue<FileChunk> chunkList;
	private int desiredReplicationDegree;
	
	public SharedFile(String fileId, int desiredReplicationDegree) {
		this.fileId = fileId;
		this.desiredReplicationDegree = desiredReplicationDegree;
	}
	
}
