package sdis.sharedbackup.protocols;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import sdis.sharedbackup.backend.ChunkRecord;
import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.utils.Log;

public class SpaceReclaiming {

	private static SpaceReclaiming sInstance = null;
	public static final String REMOVED_COMMAND = "REMOVED";

	public static SpaceReclaiming getInstance() {

		if (sInstance == null) {
			sInstance = new SpaceReclaiming();
		}
		return sInstance;
	}

	private SpaceReclaiming() {
	}

	public boolean reclaimSpace(FileChunk chunk) {

		if (!ConfigsManager.getInstance().deleteChunk(
				new ChunkRecord(chunk.getFileId(), (int) chunk.getChunkNo()))) {
			return false;
		}

		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		message += REMOVED_COMMAND + " " + version + " " + chunk.getFileId()
				+ " " + chunk.getChunkNo() + MulticastComunicator.CRLF
				+ MulticastComunicator.CRLF;

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);

		ConfigsManager.getInstance().deleteChunk(
				new ChunkRecord(chunk.getFileId(), (int) chunk.getChunkNo()));

		try {
			sender.sendMessage(message
					.getBytes(MulticastComunicator.ASCII_CODE));
		} catch (HasToJoinException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		Log.log("Sent REMOVED cmd of " + chunk.getFileId() + ":"
				+ chunk.getChunkNo());

		return true;

	}
}
