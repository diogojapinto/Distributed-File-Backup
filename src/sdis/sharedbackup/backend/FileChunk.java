package sdis.sharedbackup.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileChunk {

	public static int MAX_CHUNK_SIZE = 64000;

	private SharedFile mParentFile;
	private String mFileId;
	private int mChunkNo;
	private int mCurrentReplicationDegree;
	private int mDesiredReplicationDegree;

	private boolean isOwnMachineFile;

	public FileChunk(SharedFile parentFile, int chunkNo) {
		this.mParentFile = parentFile;
		this.mChunkNo = chunkNo;
		this.mFileId = mParentFile.getFileId();
		this.mCurrentReplicationDegree = 0;
		this.mDesiredReplicationDegree = mParentFile.getDesiredReplication();
		isOwnMachineFile = true;
	}

	public FileChunk(String fileId, int chunkNo, int desiredReplication) {
		this.mParentFile = null;
		this.mChunkNo = chunkNo;
		this.mFileId = fileId;
		this.mCurrentReplicationDegree = 0;
		this.mDesiredReplicationDegree = desiredReplication;
		isOwnMachineFile = false;
	}

	public boolean saveToFile(byte[] data) {
		if (isOwnMachineFile) {
			return false;
		} else {
			File newChunk = new File(ConfigsManager.getInstance()
					.getChunksDestination()
					+ mFileId
					+ "_"
					+ String.valueOf(mChunkNo) + ".cnk");

			FileOutputStream out = null;

			try {
				out = new FileOutputStream(newChunk);
			} catch (FileNotFoundException e) {
				System.err.println("Error outputing to new Chunk");
				e.printStackTrace();
				return false;
			}

			try {
				out.write(data);
			} catch (IOException e) {
				System.err.println("Error outputing to new Chunk");
				e.printStackTrace();
				try {
					out.close();
				} catch (IOException e1) {
					e.printStackTrace();
				}
				return false;
			}
			return true;
		}
	}

	// Getters
	public String getFileId() {
		return mFileId;
	}

	public int getChunkNo() {
		return mChunkNo;
	}

	public int getDesiredReplicationDeg() {
		return mCurrentReplicationDegree;
	}

	public byte[] getData() {
		if (isOwnMachineFile) {
			File file = new File(mParentFile.getFilePath());
			byte[] chunk = new byte[SharedFile.CHUNK_SIZE];

			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			try {
				in.read(chunk, SharedFile.CHUNK_SIZE * mChunkNo,
						SharedFile.CHUNK_SIZE);
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return chunk;
		} else {
			return null;
		}
	}

	// Setters

	public void incCurrentReplication() {
		mCurrentReplicationDegree++;
	}
}
