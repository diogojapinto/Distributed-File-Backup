package com.sdis.sharedbackup.backend;

public class FileChunk {
	private int chunkNo;
	private String fileId;
	private String filePath;
	private int desiredReplicationDegree;
	private int currentReplicationDegree;
	private byte[] data;
	
	public FileChunk(String fileId, int chunkNo, int desiredReplicationDegree, byte[] data) {
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.desiredReplicationDegree = desiredReplicationDegree;
		this.data = data.clone();		
	}
	
	private boolean saveToFile(byte[] data) {
		// TODO: save data to file according to chunkNo in fileId folder
		return false;
	}
	
	// Getters
	public String getFileId() {
		return fileId;
	}
	
	public int getChunkNo() {
		return chunkNo;
	}
	
	public int getDesiredReplicationDeg() {
		return currentReplicationDegree;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	// Setters
}
