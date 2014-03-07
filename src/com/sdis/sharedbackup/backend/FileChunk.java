package com.sdis.sharedbackup.backend;

public class FileChunk {
	private int chunkNo;
	private String fileId;
	private String filePath;
	private int desiredReplicationDegree;
	private int currentReplicationDegree;
	
	public FileChunk(int chunkNo, int desiredReplicationDegree, byte[] data) {
		this.chunkNo = chunkNo;
		this.desiredReplicationDegree = desiredReplicationDegree;
	}
	
	private boolean saveToFile(byte[] data) {
		// TODO: save data to file according to chunkNo in fileId folder
		return false;
	}
}
