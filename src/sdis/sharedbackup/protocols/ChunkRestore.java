package sdis.sharedbackup.protocols;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.FileChunkWithData;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.MulticastDataRestoreListener;

public class ChunkRestore {

	private static ChunkRestore sInstance = null;

	public static final String GET_COMMAND = "GETCHUNK";
	public static final String CHUNK_COMMAND = "CHUNK";
	private static final int REQUEST_TIME_INTERVAL = 500;

	private ArrayList<FileChunkWithData> mRequestedChunks;

	public static ChunkRestore getInstance() {

		if (sInstance == null) {
			sInstance = new ChunkRestore();
		}
		return sInstance;
	}

	private ChunkRestore() {
		mRequestedChunks = new ArrayList<FileChunkWithData>();
	}

	public FileChunkWithData requestChunk(String fileId, long chunkNo) {

		FileChunkWithData retChunk = null;

		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		message += GET_COMMAND + " " + version + " " + fileId + " " + chunkNo
				+ " " + MulticastComunicator.CRLF + MulticastComunicator.CRLF;

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);

		sender.join();

		MulticastDataRestoreListener.getInstance().subscribeToChunkData(fileId,
				chunkNo);

		do {
			sender.sendMessage(message);
			try {
				Thread.sleep(REQUEST_TIME_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (mRequestedChunks) {
				for (FileChunkWithData chunk : mRequestedChunks) {
					if (chunk.getFileId().equals(fileId)
							&& chunk.getChunkNo() == chunkNo) {
						retChunk = chunk;
						mRequestedChunks.remove(chunk);
						break;
					}
				}
			}

		} while (retChunk == null);

		return retChunk;
	}

	public boolean sendChunk(FileChunk chunk) {

		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		message += CHUNK_COMMAND + " " + version + " " + chunk.getFileId()
				+ " " + chunk.getChunkNo() + " " + MulticastComunicator.CRLF
				+ MulticastComunicator.CRLF + chunk.getData();

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);

		sender.join();

		sender.sendMessage(message);

		return true;
	}

	public synchronized void addRequestedChunk(FileChunkWithData chunk) {
		mRequestedChunks.add(chunk);
	}
}
