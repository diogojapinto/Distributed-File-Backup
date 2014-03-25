package sdis.sharedbackup.backend;

public class ChunkRecord {
	String fileId;
	int chunkNo;
	boolean isNotified;

	public ChunkRecord(String fileId, int chunkNo) {
		this.fileId = fileId;
		this.chunkNo = chunkNo;
		this.isNotified = false;
	}
}