package sdis.sharedbackup.backend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;

import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.protocols.ChunkRestore;
import sdis.sharedbackup.protocols.FileDeletion;
import sdis.sharedbackup.protocols.SpaceReclaiming;

/*
 * Class that receives and dispatches messages from the multicast control channel
 */
public class MulticastControlListener implements Runnable {

	private static final int MAX_WAIT_TIME = 400;
	private static final int BUFFER_SIZE = 128;
	private ArrayList<ChunkRecord> mSentChunks;

	private static ArrayList<ChunkRecord> interestingChunks;

	private static MulticastControlListener mInstance = null;

	private Random random;

	private MulticastControlListener() {
		random = new Random();
		interestingChunks = new ArrayList<ChunkRecord>();
		mSentChunks = new ArrayList<ChunkRecord>();
	}

	public static MulticastControlListener getInstance() {
		if (mInstance == null) {
			mInstance = new MulticastControlListener();
		}
		return mInstance;
	}

	@Override
	public void run() {

		// throw thread to listen to responses to CHUNK messages sent directly
		// to ip

		InetAddress addr = ConfigsManager.getInstance().getMCAddr();
		int port = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator receiver = new MulticastComunicator(addr, port);

		receiver.join();

		try {
			while (ConfigsManager.getInstance().isAppRunning()) {

				final SenderRecord sender = new SenderRecord();

				String message;

				message = receiver.receiveMessage(sender);

				String[] components;
				String separator = MulticastComunicator.CRLF
						+ MulticastComunicator.CRLF;

				components = message.trim().split(separator);

				String header = components[0].trim();

				String[] header_components = header.split(" ");

				if (!header_components[1].equals(ConfigsManager.getInstance()
						.getVersion())
						|| !header_components[1].equals(ConfigsManager
								.getInstance().getVersion())) {
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

					ConfigsManager.getInstance().getExecutor()
							.execute(new Runnable() {

								@Override
								public void run() {
									try {
										ConfigsManager.getInstance()
												.incChunkReplication(fileId,
														chunkNo);
									} catch (ConfigsManager.InvalidChunkException e) {

										// not my file

										synchronized (MulticastDataBackupListener
												.getInstance().mPendingChunks) {
											for (FileChunk chunk : MulticastDataBackupListener
													.getInstance().mPendingChunks) {
												if (fileId.equals(chunk
														.getFileId())
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

					ConfigsManager.getInstance().getExecutor()
							.execute(new Runnable() {

								@Override
								public void run() {
									ChunkRecord record = new ChunkRecord(
											fileId, chunkNo);
									synchronized (interestingChunks) {
										interestingChunks.add(record);
									}

									FileChunk chunk = ConfigsManager
											.getInstance().getSavedChunk(
													fileId, chunkNo);

									if (chunk != null) {
										try {
											Thread.sleep(random
													.nextInt(MAX_WAIT_TIME));
										} catch (InterruptedException e) {
											e.printStackTrace();
										}

										if (!record.isNotified) {
											// if no one else sent it:

											synchronized (mSentChunks) {
												mSentChunks.add(record);
												ConfigsManager
														.getInstance()
														.getExecutor()
														.execute(
																new restoreSenderIPListener());
											}

											ChunkRestore.getInstance()
													.sendChunk(chunk,
															sender.getAddr(),
															sender.getPort());

											try {
												Thread.sleep(MAX_WAIT_TIME);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}

											synchronized (mSentChunks) {
												if (!record.isNotified
														&& mSentChunks
																.contains(record)) {
													// if no one else sent it:
													mSentChunks.remove(record);
													ChunkRestore.getInstance()
															.sendChunk(chunk);
												}
											}

											interestingChunks.remove(record);
										}
									}// else I don't have it
								}
							});
					break;

				case FileDeletion.DELETE_COMMAND:

					fileId = header_components[1].trim();

					ConfigsManager.getInstance().getExecutor()
							.execute(new Runnable() {

								public void run() {
									ConfigsManager.getInstance()
											.removeByFileId(fileId);
								}
							});
					break;
				case FileDeletion.RESPONSE_COMMAND:

					fileId = header_components[1].trim();

					ConfigsManager.getInstance().getExecutor()
							.execute(new Runnable() {

								public void run() {
									ConfigsManager.getInstance()
											.decDeletedFileReplication(fileId);
								}
							});
					break;
				case SpaceReclaiming.REMOVED_COMMAND:

					fileId = header_components[2].trim();
					chunkNo = Integer.parseInt(header_components[3].trim());

					ConfigsManager.getInstance().getExecutor()
							.execute(new Runnable() {

								@Override
								public void run() {

									FileChunk chunk = ConfigsManager
											.getInstance().getSavedChunk(
													fileId, chunkNo);

									if (chunk != null) {
										if (chunk.decCurrentReplication() < chunk
												.getDesiredReplicationDeg()) {
											try {
												Thread.sleep(random
														.nextInt(MAX_WAIT_TIME));
											} catch (InterruptedException e) {
												e.printStackTrace();
											}

											ChunkBackup.getInstance().putChunk(
													chunk);
										}

									} // else I don't have it
								}
							});
					break;
				default:
					System.out.println("Received non recognized command");
				}
			}
		} catch (HasToJoinException e1) {
			e1.printStackTrace();
		}
	}

	public synchronized void notifyChunk(String fileId, int chunkNo) {
		for (ChunkRecord record : interestingChunks) {
			if (record.fileId.equals(fileId) && record.chunkNo == chunkNo) {
				record.isNotified = true;
			}
		}
	}

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

				synchronized (mSentChunks) {
					for (ChunkRecord record : mSentChunks) {
						if (record.fileId.equals(fileId)
								&& record.chunkNo == chunkNo) {
							mSentChunks.remove(record);
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
