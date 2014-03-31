package sdis.sharedbackup.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import sdis.sharedbackup.utils.Log;

public class FileChunk implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1;

	public static int MAX_CHUNK_SIZE = 64000;

	private SharedFile mParentFile;
	private String mFileId;
	private long mChunkNo;
	private int mCurrentReplicationDegree;
	private int mDesiredReplicationDegree;

	private boolean isOwnMachineFile;

	public FileChunk(SharedFile parentFile, long chunkNo) {
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
			FileOutputStream out = null;

			try {
				out = new FileOutputStream(getFilePath());
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

	public boolean removeData() {
		if (!isOwnMachineFile) {
			File file = new File(getFilePath());

			return file.delete();

		} else {
			return false;
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
		return mDesiredReplicationDegree;
	}

	public int getCurrentReplicationDeg() {
		return mCurrentReplicationDegree;
	}

	public byte[] getData() {

		if (isOwnMachineFile) {
			if (mParentFile.exists()) {
				File file = new File(mParentFile.getFilePath());

				int offset = (int) (SharedFile.CHUNK_SIZE * mChunkNo);

				int chunkSize = (int) Math.min(SharedFile.CHUNK_SIZE,
						mParentFile.getFileSize() - offset);

				byte[] chunk = new byte[chunkSize];
				FileInputStream in = null;

				try {
					in = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				try {
					in.skip(offset);
					
					in.read(chunk, 0, chunkSize);

					Log.log("Lenght chunk" + chunkSize);
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
				int i = in.read(buffer);
				Log.log("Chunk has " + i + " size");
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
			return mParentFile.getFilePath();
		}
	}

	// Setters

	public synchronized int incCurrentReplication() {
		return ++mCurrentReplicationDegree;
	}

	public synchronized int decCurrentReplication() {
		return --mCurrentReplicationDegree;
	}
}
