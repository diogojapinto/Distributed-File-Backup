package sdis.sharedbackup.backend;

import sdis.sharedbackup.protocols.ChunkRestore;
import sdis.sharedbackup.utils.Log;
import sdis.sharedbackup.utils.SplittedMessage;

public class MulticastDataRestoreHandler implements Runnable {
	private SplittedMessage mMessage;

	public MulticastDataRestoreHandler(SplittedMessage message) {
		mMessage = message;
	}

	@Override
	public void run() {

		String[] header_components = mMessage.getHeader().split(" ");

		if (!header_components[1].equals(ConfigsManager.getInstance()
				.getVersion())) {
			System.err
					.println("Received message with protocol with different version");
			return;
		}

		String messageType = header_components[0].trim();
		String fileId;
		int chunkNo;

		switch (messageType) {
		case ChunkRestore.CHUNK_COMMAND:

			fileId = header_components[2].trim();
			chunkNo = Integer.parseInt(header_components[3].trim());

			Log.log("Received CHUNK command for file " + fileId + " chunk "
					+ chunkNo);
			Log.log("Size: " + mMessage.getBody().length);
			

			// verify if I'm interested in this chunk
			boolean myRequest = false;
			for (ChunkRecord record : MulticastDataRestoreListener.getInstance().mSubscribedChunks) {
				if (record.fileId.equals(fileId) && record.chunkNo == chunkNo) {

					FileChunkWithData requestedChunk = new FileChunkWithData(
							fileId, chunkNo, mMessage.getBody());

					ChunkRestore.getInstance()
							.addRequestedChunk(requestedChunk);

					MulticastDataRestoreListener.getInstance().mSubscribedChunks.remove(record);
					
					myRequest = true;
					
					break;
				}
			}
			
			if (!myRequest) {
				MulticastControlListener.getInstance().notifyChunk(fileId, chunkNo);
			}

			break;
		default:
			System.out.println("MDR received non recognized command");
			System.out.println(new String(mMessage.getHeader()));
		}
	}

}
