package sdis.sharedbackup.protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.SharedFile;

public class ChunkBackup {

	public static final String PUT_COMMAND = "PUTCHUNK";
	public static final String STORED_COMMAND = "STORED";
	private static final int PUT_TIME_INTERVAL = 500;
	private static final int MAX_RETRIES = 5;

	private static ChunkBackup sInstance = null;

	public static ChunkBackup getInstance() {

		if (sInstance == null) {
			sInstance = new ChunkBackup();
		}
		return sInstance;
	}

	private ChunkBackup() {
	}

	public boolean putChunk(FileChunk chunk) {
		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		try {
			message += PUT_COMMAND
					+ " "
					+ version
					+ " "
					+ chunk.getFileId()
					+ " "
					+ chunk.getChunkNo()
					+ " "
					+ chunk.getDesiredReplicationDeg()
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE)
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE) + " "
					+ chunk.getData();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		InetAddress multDBAddr = ConfigsManager.getInstance().getMDBAddr();
		int multDBPort = ConfigsManager.getInstance().getMDBPort();

		MulticastComunicator sender = new MulticastComunicator(multDBAddr,
				multDBPort);
		sender.join();
		
		int counter = 0;
		
		do {
			sender.sendMessage(message);
			try {
				Thread.sleep(PUT_TIME_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			counter++;
		} while (chunk.getDesiredReplicationDeg() > chunk
				.getCurrentReplicationDeg() || counter < MAX_RETRIES);

		return true;

	}

	public boolean storeChunk(FileChunk chunk, byte[] data) {

		InetAddress multCtrlAddr = ConfigsManager.getInstance().getMCAddr();
		int multCtrlPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCtrlAddr,
				multCtrlPort);
		sender.join();

		chunk.saveToFile(data);

		String message = null;
		try {
			message = STORED_COMMAND
					+ " "
					+ ConfigsManager.getInstance().getVersion()
					+ " "
					+ chunk.getFileId()
					+ " "
					+ String.valueOf(chunk.getChunkNo())
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE)
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE);
		} catch (UnsupportedEncodingException e2) {
			e2.printStackTrace();
		}

		sender.sendMessage(message);

		return true;
	}
}
