package sdis.sharedbackup.backend;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import javax.naming.ConfigurationException;

import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.protocols.ChunkRestore;

/*
 * Class that receives and dispatches messages from the multicast control channel
 */
public class MulticastControlListener implements Runnable {

	private static MulticastControlListener mInstance = null;

	private ArrayList<ChunkRecord> mSubscribedChunks;

	private MulticastControlListener() {
		mSubscribedChunks = new ArrayList<ChunkRecord>();
	}

	public static MulticastControlListener getInstance() {
		if (mInstance == null) {
			mInstance = new MulticastControlListener();
		}
		return mInstance;
	}

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
			String fileId = null;
			int chunkNo = 0;

			switch (messageType) {
			case ChunkBackup.STORED_COMMAND:

				fileId = header_components[2].trim();
				chunkNo = Integer.parseInt(header_components[3].trim());

				try {
					ConfigsManager.getInstance().incChunkReplication(fileId,
							chunkNo);
				} catch (ConfigsManager.InvalidChunkException e) {

					// not my file

					synchronized (MulticastDataBackupListener.getInstance().mPendingChunks) {
						for (FileChunk chunk : MulticastDataBackupListener
								.getInstance().mPendingChunks) {
							if (fileId.equals(chunk.getFileId())
									&& chunk.getChunkNo() == chunkNo) {
								chunk.incCurrentReplication();
							}
						}
					}
				}
				break;
			case ChunkRestore.CHUNK_COMMAND:

				fileId = header_components[2].trim();
				chunkNo = Integer.parseInt(header_components[3].trim());

				for (ChunkRecord record : mSubscribedChunks) {
					if (record.fileId.equals(fileId)
							&& record.chunkNo == chunkNo) {
						// TODO : ask teacher what is the replication degree (if
						// any) of the new file
						byte[] data;
						try {
							data = components[1]
									.getBytes(MulticastComunicator.ASCII_CODE);

							FileChunkWithData requestedChunk = new FileChunkWithData(
									fileId, chunkNo, data);

							ChunkRestore.getInstance().addRequestedChunk(
									requestedChunk);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}
				}

				break;
			default:
			}
		}
	}

	public synchronized void subscribeToChunkData(String fileId, int chunkNo) {
		mSubscribedChunks.add(new ChunkRecord(fileId, chunkNo));
	}

	private class ChunkRecord {
		String fileId;
		int chunkNo;

		public ChunkRecord(String fileId, int chunkNo) {
			this.fileId = fileId;
			this.chunkNo = chunkNo;
		}
	}
}
