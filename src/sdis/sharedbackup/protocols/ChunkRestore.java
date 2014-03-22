package sdis.sharedbackup.protocols;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.FileChunkWithData;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.MulticastControlListener;
import sdis.sharedbackup.backend.SharedFile;

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
	
	public FileChunkWithData requestChunk(String fileId, int chunkNo) {
		
		FileChunkWithData retChunk = null;
		
		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		try {
			message += GET_COMMAND
					+ " "
					+ version
					+ " "
					+ fileId
					+ " "
					+ chunkNo
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE)
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE);
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);
		
		sender.join();
		
		MulticastControlListener.getInstance().subscribeToChunkData(fileId, chunkNo);

		do {
			sender.sendMessage(message);
			try {
				Thread.sleep(REQUEST_TIME_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			for (FileChunkWithData chunk : mRequestedChunks) {
				if (chunk.getFileId().equals(fileId) && chunk.getChunkNo() == chunkNo) {
					retChunk = chunk;
					mRequestedChunks.remove(chunk);
					break;
				}
			}
			
		} while (retChunk == null);
		
		return retChunk;
	}
	
	public boolean sendChunk(FileChunk chunk) {
		
		return false;
	}
	
	public synchronized void addRequestedChunk(FileChunkWithData chunk) {
		mRequestedChunks.add(chunk);
	}
}
