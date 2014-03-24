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

		try {
			message += DELETE_COMMAND
					+ " "
					+ fileId
					+ " "
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE)
					+ new String(MulticastComunicator.CRLF,
							MulticastComunicator.ASCII_CODE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		InetAddress multDBAddr = ConfigsManager.getInstance().getMDBAddr();
		int multDBPort = ConfigsManager.getInstance().getMDBPort();

		MulticastComunicator sender = new MulticastComunicator(multDBAddr,
				multDBPort);
		sender.join();

		return true;
	}
}
