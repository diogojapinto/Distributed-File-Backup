package sdis.sharedbackup.backend;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import javax.naming.ConfigurationException;

import sdis.sharedbackup.backend.MulticastDataRestoreListener.ChunkRecord;
import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.protocols.ChunkRestore;

/*
 * Class that receives and dispatches messages from the multicast control channel
 */
public class MulticastControlListener implements Runnable {

	private static final int MAX_WAIT_TIME = 400;

	private static ArrayList<ChunkRecord> interestingChunks;

	private static MulticastControlListener mInstance = null;

	private Random random;

	private MulticastControlListener() {
		random = new Random();
		interestingChunks = new ArrayList<ChunkRecord>();
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
			final String fileId;
			final int chunkNo;

			switch (messageType) {
			case ChunkBackup.STORED_COMMAND:

				fileId = header_components[2].trim();
				chunkNo = Integer.parseInt(header_components[3].trim());

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							ConfigsManager.getInstance().incChunkReplication(
									fileId, chunkNo);
						} catch (ConfigsManager.InvalidChunkException e) {

							// not my file

							synchronized (MulticastDataBackupListener
									.getInstance().mPendingChunks) {
								for (FileChunk chunk : MulticastDataBackupListener
										.getInstance().mPendingChunks) {
									if (fileId.equals(chunk.getFileId())
											&& chunk.getChunkNo() == chunkNo) {
										chunk.incCurrentReplication();
									}
								}
							}
						}
					}

				});
				break;
			case ChunkRestore.GET_COMMAND:

				fileId = header_components[2].trim();
				chunkNo = Integer.parseInt(header_components[3].trim());

				new Thread(new Runnable() {

					@Override
					public void run() {
						ChunkRecord record = new ChunkRecord(fileId, chunkNo);
						synchronized (interestingChunks) {
							interestingChunks.add(record);
						}

						FileChunk chunk = ConfigsManager.getInstance()
								.getSavedChunk(fileId, chunkNo);

						if (chunk != null) {
							try {
								Thread.sleep(random.nextInt(MAX_WAIT_TIME));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							if (!record.isNotified) {
								// if no one else sent it:
								interestingChunks.remove(record);
								ChunkRestore.getInstance().sendChunk(chunk);
							}
						}// else I don't have it
					}
				});

			default:
				System.out.println("Received non recognized command");
			}
		}
	}

	public synchronized void notifyChunk(String fileId, int chunkNo) {
		for (ChunkRecord record : interestingChunks) {
			if (record.fileId.equals(fileId) && record.chunkNo == chunkNo) {
				record.isNotified = true;
			}
		}
	}
}
