package sdis.sharedbackup.backend;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import sdis.sharedbackup.protocols.ChunkBackup;

/*
 * Class that receives and dispatches messages from the multicast data backup channel
 */
public class MulticastDataBackupListener implements Runnable {

	@Override
	public void run() {
		InetAddress addr = ConfigsManager.getInstance().getMCAddr();
		int port = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator receiver = new MulticastComunicator(addr, port);

		while (true) {
			String message = receiver.receiveMessage();
			String[] components;
			String separator = null;
			try {
				separator = new String(MulticastComunicator.CRLF,
						MulticastComunicator.ASCII_CODE)
						+ " "
						+ new String(MulticastComunicator.CRLF,
								MulticastComunicator.ASCII_CODE);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			components = message.trim().split(separator);

			String header = components[0].trim();

			String[] header_components = header.split(" ");

			if (!header_components[1].equals(ConfigsManager.getInstance()
					.getVersion())) {
				System.err
						.println("Received message with protocol with different version");
				continue;
			}

			String messageType = header_components[0].trim();

			switch (messageType) {
			case ChunkBackup.PUT_COMMAND:

				String fileId = header_components[2].trim();
				int chunkNo = Integer.parseInt(header_components[3].trim());
				int desiredReplication = Integer.parseInt(header_components[4]
						.trim());

				ChunkBackup.getInstance().storeChunks(fileId, chunkNo,
						desiredReplication, components[1].getBytes());

				break;
			default:
			}
		}
	}
}
