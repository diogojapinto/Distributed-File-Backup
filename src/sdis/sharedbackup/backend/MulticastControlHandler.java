package sdis.sharedbackup.backend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.protocols.ChunkRestore;
import sdis.sharedbackup.protocols.FileDeletion;
import sdis.sharedbackup.protocols.SpaceReclaiming;
import sdis.sharedbackup.utils.SplittedMessage;

public class MulticastControlHandler implements Runnable {
	private SplittedMessage mMessage;
	private Random random;
	private SenderRecord mSender;
	private static final int MAX_WAIT_TIME = 400;
	private static final int BUFFER_SIZE = 128;

	public MulticastControlHandler(SplittedMessage message, SenderRecord sender) {
		mSender = sender;
		mMessage = message;
		random = new Random();
	}

	public void run() {

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
		final String fileId;
		final int chunkNo;
		System.out.println("MC RECEIVED A MESSAGE!!:" + mMessage.getHeader());
		switch (messageType) {
		case ChunkBackup.STORED_COMMAND:
			fileId = header_components[2].trim();
			chunkNo = Integer.parseInt(header_components[3].trim());

			try {
				ConfigsManager.getInstance().incChunkReplication(fileId,
						chunkNo);
			} catch (ConfigsManager.InvalidChunkException e) {

				// not my file

				synchronized (MulticastControlListener.getInstance().mPendingChunks) {
					for (FileChunk chunk : MulticastControlListener
							.getInstance().mPendingChunks) {
						if (fileId.equals(chunk.getFileId())
								&& chunk.getChunkNo() == chunkNo) {
							chunk.incCurrentReplication();
						}
					}
				}
			}
			break;
		case ChunkRestore.GET_COMMAND:
			
			fileId = header_components[2].trim();
			chunkNo = Integer.parseInt(header_components[3].trim());

			ChunkRecord record = new ChunkRecord(fileId, chunkNo);
			synchronized (MulticastControlListener.interestingChunks) {
				MulticastControlListener.interestingChunks.add(record);
			}

			FileChunk chunk = ConfigsManager.getInstance().getSavedChunk(
					fileId, chunkNo);
			System.out.println("Line 81");
			if (chunk != null) {
				try {
					Thread.sleep(random.nextInt(MAX_WAIT_TIME));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("88");
				if (!record.isNotified) {
					// if no one else
					// sent it:

					synchronized (MulticastControlListener.getInstance().mSentChunks) {
						MulticastControlListener.getInstance().mSentChunks
								.add(record);
						ConfigsManager.getInstance().getExecutor()
								.execute(new restoreSenderIPListener());
					}

					ChunkRestore.getInstance().sendChunk(chunk,
							mSender.getAddr(), mSender.getPort());

					try {
						Thread.sleep(MAX_WAIT_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					synchronized (MulticastControlListener.getInstance().mSentChunks) {
						if (!record.isNotified
								&& MulticastControlListener.getInstance().mSentChunks
										.contains(record)) {
							// if no one
							// else sent
							// it:
							MulticastControlListener.getInstance().mSentChunks
									.remove(record);
							ChunkRestore.getInstance().sendChunk(chunk);
						}
					}

					MulticastControlListener.interestingChunks.remove(record);
				}
			}// else I don't have it
			break;

		case FileDeletion.DELETE_COMMAND:

			fileId = header_components[1].trim();

			ConfigsManager.getInstance().removeByFileId(fileId);
			break;
		case FileDeletion.RESPONSE_COMMAND:

			fileId = header_components[1].trim();

			ConfigsManager.getInstance().decDeletedFileReplication(fileId);
			break;
		case SpaceReclaiming.REMOVED_COMMAND:

			fileId = header_components[2].trim();
			chunkNo = Integer.parseInt(header_components[3].trim());

			FileChunk chunk2 = ConfigsManager.getInstance().getSavedChunk(
					fileId, chunkNo);

			if (chunk2 != null) {
				if (chunk2.decCurrentReplication() < chunk2
						.getDesiredReplicationDeg()) {
					try {
						Thread.sleep(random.nextInt(MAX_WAIT_TIME));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					ChunkBackup.getInstance().putChunk(chunk2);
				}

			} // else I don't have it
			break;
		default:
			System.out.println("MC received non recognized command:");
			System.out.println(mMessage);
		}
	};

	private class restoreSenderIPListener implements Runnable {

		@Override
		public void run() {
			DatagramSocket restoreSocket = null;
			try {
				restoreSocket = new DatagramSocket(
						ChunkRestore.ENHANCEMENT_SEND_PORT);
			} catch (SocketException e) {
				System.out
						.println("Could not open the desired port for restore");
				e.printStackTrace();
				System.exit(-1);
			}

			byte[] buffer = new byte[BUFFER_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);

			try {
				restoreSocket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}

			String message = null;

			try {
				message = new String(packet.getData(),
						MulticastComunicator.ASCII_CODE).trim();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			String[] components = message.trim().split(
					MulticastComunicator.CRLF + MulticastComunicator.CRLF);

			String[] headerComponents = components[0].trim().split(" ");

			switch (headerComponents[0]) {
			case ChunkRestore.CHUNK_CONFIRMATION:
				String fileId = headerComponents[2];
				int chunkNo = Integer.parseInt(headerComponents[3]);

				synchronized (MulticastControlListener.getInstance().mSentChunks) {
					for (ChunkRecord record : MulticastControlListener
							.getInstance().mSentChunks) {
						if (record.fileId.equals(fileId)
								&& record.chunkNo == chunkNo) {
							MulticastControlListener.getInstance().mSentChunks
									.remove(record);
							break;
						}
					}
				}

				break;
			default:
				System.out.println("Received non recognized command");
			}
		}
	}

}
