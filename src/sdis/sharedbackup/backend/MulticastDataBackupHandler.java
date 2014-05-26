package sdis.sharedbackup.backend;

import java.util.Random;

import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.utils.Log;
import sdis.sharedbackup.utils.SplittedMessage;

public class MulticastDataBackupHandler implements Runnable {
	private SplittedMessage mMessage;

	private static final int MAX_WAIT_TIME = 401;
	private static final int PUT_WAIT_TIME = 60000;
	private Random mRand;

	public MulticastDataBackupHandler(SplittedMessage message) {
		mMessage = message;
		mRand = new Random();
	}

	@Override
	public void run() {
		Log.log("MDB:Received message");

		String[] headers = mMessage.getHeader()
				.split(MulticastCommunicator.CRLF);

		String[] header_components = headers[0].split(" ");

		String messageType = header_components[0].trim();

		switch (messageType) {
		case ChunkBackup.PUT_COMMAND:
			Log.log("REceived a PUT");
			if (ConfigsManager.getInstance().getBackupDirActualSize()
					+ FileChunk.MAX_CHUNK_SIZE >= ConfigsManager.getInstance()
					.getMaxBackupSize()) {
				return;
			}

			final String fileId = header_components[1].trim();

			if (ConfigsManager.getInstance().isMyFile(fileId)) {
				Log.log("Received PUTCHUNK for a file of mine");
				return;
			}

			final int chunkNo = Integer.parseInt(header_components[2].trim());
			int desiredReplication = Integer.parseInt(header_components[3]
					.trim());

			// ENHANCEMENT
			int currReplication = 0;
			if (headers.length == 2) {
				currReplication = Integer.parseInt(headers[1].trim());
			}
			FileChunk savedChunk = ConfigsManager.getInstance().getSavedChunk(
					fileId, chunkNo);

			// file not yet saved
			if (savedChunk == null) {
				FileChunk pendingChunk = new FileChunk(fileId, chunkNo,
						desiredReplication);

				// ENHANCEMENT
				pendingChunk.setCurrReplication(currReplication);

				synchronized (MulticastControlListener.getInstance().mPendingChunks) {
					MulticastControlListener.getInstance().mPendingChunks
							.add(pendingChunk);
				}

				try {
					Thread.sleep(mRand.nextInt(MAX_WAIT_TIME));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				// verify if it is needed to save the
				// chunk

				if (pendingChunk.getCurrentReplicationDeg() < pendingChunk
						.getDesiredReplicationDeg()) {

				//	Log.log("I tried a store ");
					ChunkBackup.getInstance().storeChunk(pendingChunk,
							mMessage.getBody());

					// verify after some time if the chunk has the replication
					// degree desired
					new Thread(new Runnable() {

						@Override
						public void run() {
							while (true) {

								try {
									Thread.sleep(PUT_WAIT_TIME);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}

								FileChunk chunk = ConfigsManager.getInstance()
										.getSavedChunk(fileId, chunkNo);

								if (chunk == null) {
									return;
								}

								if (chunk.getCurrentReplicationDeg() < chunk
										.getDesiredReplicationDeg()) {
									ChunkBackup.getInstance().putChunk(chunk);
								}

							}
						}
					}).start();

				}

				// remove the temporary chunk from the
				// pending
				// chunks
				// list
				synchronized (MulticastControlListener.getInstance().mPendingChunks) {
					MulticastControlListener.getInstance().mPendingChunks
							.remove(pendingChunk);
				}
			} else {
				Log.log("Received CHUNK IS ALREADY SAVED");
			}

			break;
		default:
			Log.log("MDB received non recognized command");
			System.out.println(mMessage);
		}
	};
}
