package sdis.sharedbackup.backend;

public class FileChunkWithData extends FileChunk {
	
	private byte[] data;
	
	public FileChunkWithData(String fileId, int chunkNo, byte[] data) {
		super(fileId, chunkNo, 0);
		
		this.data = data;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
}
