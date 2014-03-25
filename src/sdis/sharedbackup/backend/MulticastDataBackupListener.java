package sdis.sharedbackup.backend;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import sdis.sharedbackup.protocols.ChunkBackup;

/*
 * Class that receives and dispatches messages from the multicast data backup channel
 */
public class MulticastDataBackupListener implements Runnable {

	private static MulticastDataBackupListener mInstance = null;

	private static final int MAX_WAIT_TIME = 401;
	private Random mRand;

	// List containing chunks whom we heard about but have not been saved
	final ArrayList<FileChunk> mPendingChunks;

	private MulticastDataBackupListener() {
		mRand = new Random();
		mPendingChunks = new ArrayList<FileChunk>();
	}

	public static MulticastDataBackupListener getInstance() {
		if (mInstance == null) {
			mInstance = new MulticastDataBackupListener();
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

			final String[] header_components = header.split(" ");

			if (!header_components[1].equals(ConfigsManager.getInstance()
					.getVersion())) {
				System.err
						.println("Received message with protocol with different version");
				continue;
			}

			String messageType = header_components[0].trim();

			switch (messageType) {
			case ChunkBackup.PUT_COMMAND:

				if (ConfigsManager.getInstance().getBackupDirActualSize()
						+ FileChunk.MAX_CHUNK_SIZE >= ConfigsManager
						.getInstance().getMaxBackupSize()) {
					continue;
				}

				final String fileId = header_components[2].trim();
				final int chunkNo = Integer.parseInt(header_components[3]
						.trim());
				final int desiredReplication = Integer
						.parseInt(header_components[4].trim());

				new Thread(new Runnable() {

					@Override
					public void run() {

						FileChunk savedChunk = ConfigsManager.getInstance()
								.getSavedChunk(fileId, chunkNo);

						if (savedChunk != null) {
							FileChunk pendingChunk = new FileChunk(fileId,
									chunkNo, desiredReplication);

							synchronized (mPendingChunks) {
								mPendingChunks.add(pendingChunk);
							}

							try {
								Thread.sleep(mRand.nextInt(MAX_WAIT_TIME));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							// verify if it is needed to save the chunk

							if (pendingChunk.getCurrentReplicationDeg() < pendingChunk
									.getDesiredReplicationDeg()) {

								try {
									ChunkBackup
											.getInstance()
											.storeChunk(
													pendingChunk,
													components[1]
															.getBytes(MulticastComunicator.ASCII_CODE));
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}

							}

							// remove the temporary chunk from the pending
							// chunks
							// list
							synchronized (mPendingChunks) {
								mPendingChunks.remove(pendingChunk);
							}
						}
					}
				}).start();

				break;
			default:
				System.out.println("Received non recognized command");
			}
		}
	}
}
