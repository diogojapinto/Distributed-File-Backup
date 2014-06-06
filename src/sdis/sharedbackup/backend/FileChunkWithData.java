package sdis.sharedbackup.backend;

import sdis.sharedbackup.protocols.AccessLevel;

public class FileChunkWithData extends FileChunk {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private byte[] data;
	
	public FileChunkWithData(String fileId, int chunkNo, byte[] data, AccessLevel al) {
		super(fileId, chunkNo, 0, al);
		
		this.data = data;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
}
