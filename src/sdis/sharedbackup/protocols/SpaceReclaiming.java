package sdis.sharedbackup.protocols;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;

import sdis.sharedbackup.backend.ChunkRecord;
import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.FileChunkWithData;
import sdis.sharedbackup.backend.MulticastComunicator;

public class SpaceReclaiming {

	private static SpaceReclaiming sInstance = null;
	private static final String REMOVED_COMMAND = "REMOVED";

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
				+ " " + chunk.getChunkNo() + " " + MulticastComunicator.CRLF
				+ MulticastComunicator.CRLF;

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);
		sender.join();

		sender.sendMessage(message);

		return true;

	}

}
