package sdis.sharedbackup.backend;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import javax.naming.ConfigurationException;

import sdis.sharedbackup.protocols.ChunkBackup;

/*
 * Class that receives and dispatches messages from the multicast control channel
 */
public class MulticastControlListener implements Runnable {

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

			String messageType = header_components[0].trim();

			switch (messageType) {
			case ChunkBackup.STORED_COMMAND:

				String fileId = header_components[2].trim();
				int chunkNo = Integer.parseInt(header_components[3].trim());

				try {
					ConfigsManager.getInstance().incChunkReplication(fileId,
							chunkNo);
				} catch (ConfigsManager.InvalidChunkException e) {
					// not my file
				}
				break;
			default:
			}
		}
	}
}
