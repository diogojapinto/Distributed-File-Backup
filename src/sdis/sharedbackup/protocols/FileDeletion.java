package sdis.sharedbackup.protocols;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.MulticastComunicator;

public class FileDeletion {

	public static final String DELETE_COMMAND = "DELETE";

	private static FileDeletion sInstance = null;

	private static FileDeletion getInstance() {
		if (sInstance == null) {
			sInstance = new FileDeletion();
		}
		return sInstance;
	}

	private FileDeletion() {

	}

	public boolean deleteFile(String fileId) {
		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		message += DELETE_COMMAND + " " + fileId + " "
				+ MulticastComunicator.CRLF + MulticastComunicator.CRLF;

		InetAddress multDBAddr = ConfigsManager.getInstance().getMDBAddr();
		int multDBPort = ConfigsManager.getInstance().getMDBPort();

		MulticastComunicator sender = new MulticastComunicator(multDBAddr,
				multDBPort);
		sender.join();

		return true;
	}
}
