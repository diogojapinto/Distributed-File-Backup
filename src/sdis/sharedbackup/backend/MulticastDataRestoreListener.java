package sdis.sharedbackup.backend;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;

import sdis.sharedbackup.protocols.ChunkRestore;

/*
 * Class that receives and dispatches messages from the multicast data restore channel
 */
public class MulticastDataRestoreListener implements Runnable {

	private static MulticastDataRestoreListener mInstance = null;

	private ArrayList<ChunkRecord> mSubscribedChunks;

	private MulticastDataRestoreListener() {
		mSubscribedChunks = new ArrayList<ChunkRecord>();
	}

	public static MulticastDataRestoreListener getInstance() {
		if (mInstance == null) {
			mInstance = new MulticastDataRestoreListener();
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
			final String[] components;
			String separator = MulticastComunicator.CRLF
					+ MulticastComunicator.CRLF;

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
			final String fileId;
			final int chunkNo;

			switch (messageType) {
			case ChunkRestore.CHUNK_COMMAND:

				fileId = header_components[2].trim();
				chunkNo = Integer.parseInt(header_components[3].trim());

				new Thread(new Runnable() {

					@Override
					public void run() {
						for (ChunkRecord record : mSubscribedChunks) {
							if (record.fileId.equals(fileId)
									&& record.chunkNo == chunkNo) {
								// TODO : ask teacher what is the replication
								// degree (if
								// any) of the new file
								byte[] data;
								try {
									data = components[1]
											.getBytes(MulticastComunicator.ASCII_CODE);

									FileChunkWithData requestedChunk = new FileChunkWithData(
											fileId, chunkNo, data);

									ChunkRestore.getInstance()
											.addRequestedChunk(requestedChunk);
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
							}
						}
					}
				});

				break;
			default:
				System.out.println("Received non recognized command");
			}
		}
	}

	public synchronized void subscribeToChunkData(String fileId, long chunkNo) {
		mSubscribedChunks.add(new ChunkRecord(fileId, (int) chunkNo));
	}

}
