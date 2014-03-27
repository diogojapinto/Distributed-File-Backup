package sdis.sharedbackup.protocols;

import java.net.InetAddress;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;

public class FileDeletion {

	public static final String DELETE_COMMAND = "DELETE";
	public static final String RESPONSE_COMMAND = "WASDELETED";

	private static FileDeletion sInstance = null;

	public static FileDeletion getInstance() {
		if (sInstance == null) {
			sInstance = new FileDeletion();
		}
		return sInstance;
	}

	private FileDeletion() {

	}

	public boolean deleteFile(String fileId) {

		String message = "";

		message += DELETE_COMMAND + " " + fileId + " "
				+ MulticastComunicator.CRLF + MulticastComunicator.CRLF;

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);
		sender.join();

		try {
			sender.sendMessage(message);
		} catch (HasToJoinException e) {
			e.printStackTrace();
		}

		return true;
	}

	public boolean reply(String fileId) {

		String message = "";

		message += RESPONSE_COMMAND + " " + fileId + " "
				+ MulticastComunicator.CRLF + MulticastComunicator.CRLF;

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);
		sender.join();

		try {
			sender.sendMessage(message);
		} catch (HasToJoinException e) {
			e.printStackTrace();
		}

		return true;
	}
}
