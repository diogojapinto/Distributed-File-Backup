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
	private long mChunkNo;
	private int mCurrentReplicationDegree;
	private int mDesiredReplicationDegree;

	private boolean isOwnMachineFile;

	public FileChunk(SharedFile parentFile, long mChunkCounter) {
		this.mParentFile = parentFile;
		this.mChunkNo = mChunkCounter;
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
			File newChunk = new File(getFilePath());

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

			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return true;
		}
	}

	// Getters
	public String getFileId() {
		return mFileId;
	}

	public long getChunkNo() {
		return mChunkNo;
	}

	public int getDesiredReplicationDeg() {
		return mCurrentReplicationDegree;
	}

	public int getCurrentReplicationDeg() {
		return mCurrentReplicationDegree;
	}

	public byte[] getData() {

		if (isOwnMachineFile) {
			if (mParentFile.exists()) {
				File file = new File(mParentFile.getFilePath());
				byte[] chunk = new byte[SharedFile.CHUNK_SIZE];
				FileInputStream in = null;

				try {
					in = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				try {
					if (in.read(chunk, SharedFile.CHUNK_SIZE * (int) mChunkNo,
							SharedFile.CHUNK_SIZE) == -1) {
						return new byte[0];
					}
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
		} else {
			File chunk = new File(getFilePath());
			FileInputStream in = null;
			try {
				in = new FileInputStream(chunk);
				byte[] buffer = new byte[(int) chunk.length()];
				in.read(buffer);
				in.close();
				return buffer;
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		}
	}

	public String getFilePath() {
		if (!isOwnMachineFile) {
			return new String(ConfigsManager.getInstance()
					.getChunksDestination()
					+ mFileId
					+ "_"
					+ String.valueOf(mChunkNo) + ".cnk");
		} else {
			return null;
		}
	}

	// Setters

	public synchronized void incCurrentReplication() {
		mCurrentReplicationDegree++;
	}
}
