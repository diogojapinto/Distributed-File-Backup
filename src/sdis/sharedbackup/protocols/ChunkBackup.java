package sdis.sharedbackup.protocols;

import java.net.InetAddress;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.utils.Log;

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

		message += PUT_COMMAND + " " + version + " " + chunk.getFileId() + " "
				+ chunk.getChunkNo() + " " + chunk.getDesiredReplicationDeg()
				+ MulticastComunicator.CRLF + MulticastComunicator.CRLF
				+ chunk.getData();

		InetAddress multDBAddr = ConfigsManager.getInstance().getMDBAddr();
		int multDBPort = ConfigsManager.getInstance().getMDBPort();

		MulticastComunicator sender = new MulticastComunicator(multDBAddr,
				multDBPort);

		int counter = 0;

		Log.log("Sending chunk " + chunk.getChunkNo() + " of file "
				+ chunk.getFilePath());

		do {
			try {
				sender.sendMessage(message);
			} catch (HasToJoinException e1) {
				e1.printStackTrace();
			}
			try {
				Thread.sleep(PUT_TIME_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			counter++;
		} while (chunk.getDesiredReplicationDeg() > chunk
				.getCurrentReplicationDeg() && counter < MAX_RETRIES);

		if (counter == MAX_RETRIES) {

			Log.log("Did not reach necessary replication");

			return false;
		} else {

			Log.log("Sent successfully");

			return true;
		}

	}

	public boolean storeChunk(FileChunk chunk, byte[] data) {

		InetAddress multCtrlAddr = ConfigsManager.getInstance().getMCAddr();
		int multCtrlPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCtrlAddr,
				multCtrlPort);

		// save chunk in file
		chunk.saveToFile(data);

		chunk.incCurrentReplication();

		// add chunk to database
		ConfigsManager.getInstance().addSavedChunk(chunk);

		String message = null;

		message = STORED_COMMAND + " "
				+ ConfigsManager.getInstance().getVersion() + " "
				+ chunk.getFileId() + " " + String.valueOf(chunk.getChunkNo())
				+ MulticastComunicator.CRLF + MulticastComunicator.CRLF;

		try {
			sender.sendMessage(message);
		} catch (HasToJoinException e) {
			e.printStackTrace();
		}

		Log.log("Sent STORED command for chunk of file " + chunk.getFileId()
				+ " no " + chunk.getChunkNo());

		return true;
	}
}
