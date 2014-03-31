package sdis.sharedbackup.backend;

import java.util.Random;

import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.utils.Log;
import sdis.sharedbackup.utils.SplittedMessage;

public class MulticastDataBackupHandler implements Runnable {
	private SplittedMessage mMessage;

	private static final int MAX_WAIT_TIME = 401;
	private Random mRand;

	public MulticastDataBackupHandler(SplittedMessage message) {
		mMessage = message;
		mRand = new Random();
	}

	@Override
	public void run() {
		Log.log("MDB:Received message");

		String[] header_components = mMessage.getHeader().split(" ");

		if (!header_components[1].equals(ConfigsManager.getInstance()
				.getVersion())
				&& !header_components[1].equals(ConfigsManager.getInstance()
						.getEnhancementsVersion())) {
			System.err
					.println("Received message with protocol with different version");
			return;
		}

		String messageType = header_components[0].trim();

		switch (messageType) {
		case ChunkBackup.PUT_COMMAND:
			System.out.println("REceived a PUT");
			if (ConfigsManager.getInstance().getBackupDirActualSize()
					+ FileChunk.MAX_CHUNK_SIZE >= ConfigsManager.getInstance()
					.getMaxBackupSize()) {
				return;
			}

			String fileId = header_components[2].trim();

			if (ConfigsManager.getInstance().isMyFile(fileId)) {
				Log.log("Received PUTCHUNK for a file of mine");
				return;
			}

			int chunkNo = Integer.parseInt(header_components[3].trim());
			int desiredReplication = Integer.parseInt(header_components[4]
					.trim());

			FileChunk savedChunk = ConfigsManager.getInstance().getSavedChunk(
					fileId, chunkNo);

			// file not yet saved
			if (savedChunk == null) {
				FileChunk pendingChunk = new FileChunk(fileId, chunkNo,
						desiredReplication);

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

					System.out.println("I tried a store ");
					ChunkBackup.getInstance().storeChunk(pendingChunk,
							mMessage.getBody());

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
				System.out.println("Received CHUNK IS ALREADY SAVED");
			}

			break;
		default:
			System.out.println("MDB received non recognized command");
			System.out.println(mMessage);
		}
	};
}