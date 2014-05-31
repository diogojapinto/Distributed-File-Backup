package sdis.sharedbackup.protocols;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.MulticastCommunicator;
import sdis.sharedbackup.backend.MulticastCommunicator.HasToJoinException;
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

		String header = "";

        AccessLevel al = chunk.getAccessLevel();
        String alId;
        if (al == null) {
            alId = "none";
        } else {
            alId = al.getId();
        }

		header += PUT_COMMAND + " " + alId + " " + chunk.getFileId() + " "
				+ chunk.getChunkNo() + " " + chunk.getDesiredReplicationDeg()
				+ MulticastCommunicator.CRLF + chunk.getCurrentReplicationDeg()
				+ MulticastCommunicator.CRLF + MulticastCommunicator.CRLF;

		byte[] data = chunk.getData();

		byte[] message = new byte[header.length() + data.length];

		try {
			System.arraycopy(header.getBytes(MulticastCommunicator.ASCII_CODE),
					0, message, 0, header.length());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		System.arraycopy(data, 0, message, header.length(), data.length);

		InetAddress multDBAddr = ConfigsManager.getInstance().getMDBAddr();
		int multDBPort = ConfigsManager.getInstance().getMDBPort();

		MulticastCommunicator sender = new MulticastCommunicator(multDBAddr,
				multDBPort);

		int counter = 0;

		Log.log("Sending chunk " + chunk.getChunkNo() + " of file "
				+ chunk.getFileId() + "with " + chunk.getData().length
				+ " bytes");

		do {
			try {
				sender.sendMessage(message);
			} catch (HasToJoinException e1) {
				e1.printStackTrace();
			}
			try {
				Log.log("WAITING : " + PUT_TIME_INTERVAL
						* (int) Math.pow(2, counter));
				Thread.sleep(PUT_TIME_INTERVAL * (int) Math.pow(2, counter));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			counter++;
			Log.log("REP DEG: " + chunk.getChunkNo() + " "
					+ chunk.getCurrentReplicationDeg());
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

		MulticastCommunicator sender = new MulticastCommunicator(multCtrlAddr,
				multCtrlPort);

		// save chunk in file

		chunk.saveToFile(data);

		chunk.incCurrentReplication();

		// add chunk to database
		ConfigsManager.getInstance().addSavedChunk(chunk);

		String message = null;

		message = STORED_COMMAND + " "
				+ chunk.getFileId() + " " + String.valueOf(chunk.getChunkNo())
				+ MulticastCommunicator.CRLF + MulticastCommunicator.CRLF;

		try {
			sender.sendMessage(message
					.getBytes(MulticastCommunicator.ASCII_CODE));
		} catch (HasToJoinException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		Log.log("Sent STORED command for chunk of file " + chunk.getFileId()
				+ " no " + chunk.getChunkNo() + " with " + data.length
				+ " bytes");

		return true;
	}
}
