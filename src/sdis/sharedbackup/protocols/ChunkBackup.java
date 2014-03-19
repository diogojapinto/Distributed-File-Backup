package sdis.sharedbackup.protocols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Random;
import java.util.Scanner;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.SharedFile;

public class ChunkBackup {

	public static final String PUT_COMMAND = "PUTCHUNK";
	public static final String STORED_COMMAND = "STORED";
	private static final int MAX_WAIT_TIME = 401;
	private Random rand;

	private static ChunkBackup sInstance = null;

	public static ChunkBackup getInstance() {

		if (sInstance == null) {
			sInstance = new ChunkBackup();
		}
		return sInstance;
	}

	private ChunkBackup() {
		rand = new Random();
	}
	
	public boolean saveFile(SharedFile file) {
		// TODO call putChunk for each chunk in SharedFile
		return true;
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

		sender.sendMessage(message);

		return true;

	}

	public boolean storeChunks(String fileId, int chunkNo, int desiredReplication, byte[] data) {

		InetAddress multCtrlAddr = ConfigsManager.getInstance().getMCAddr();
		int multCtrlPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCtrlAddr,
				multCtrlPort);
		sender.join();

		FileChunk chunk = ConfigsManager.getInstance().getNewChunkForSaving(fileId, chunkNo, desiredReplication);
		chunk.saveToFile(data);

		String message = null;
		try {
			message = STORED_COMMAND
					+ " "
					+ ConfigsManager.getInstance().getVersion()
					+ " "
					+ fileId
					+ " "
					+ String.valueOf(chunkNo)
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE)
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE);
		} catch (UnsupportedEncodingException e2) {
			e2.printStackTrace();
		}

		try {
			Thread.sleep(rand.nextInt(MAX_WAIT_TIME));
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			return false;
		}

		sender.sendMessage(message);

		return true;
	}
}
