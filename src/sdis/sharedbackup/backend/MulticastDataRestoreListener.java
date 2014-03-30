package sdis.sharedbackup.backend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.protocols.ChunkRestore;
import sdis.sharedbackup.utils.Log;
import sdis.sharedbackup.utils.SplittedMessage;
import sdis.sharedbackup.utils.Splitter;

/*
 * Class that receives and dispatches messages from the multicast data restore channel
 */
public class MulticastDataRestoreListener implements Runnable {

	private static final int BUFFER_SIZE = 70000;

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
		InetAddress addr = ConfigsManager.getInstance().getMDRAddr();
		int port = ConfigsManager.getInstance().getMDRPort();

		MulticastComunicator receiver = new MulticastComunicator(addr, port);

		receiver.join();

		ConfigsManager.getInstance().getExecutor()
				.execute(new restoreListenerIPListener());

		Log.log("Listening on " + addr.getHostAddress() + ":" + port);

		try {
			while (ConfigsManager.getInstance().isAppRunning()) {
				final byte[] message;
				message = receiver.receiveMessage();

				final SplittedMessage splittedMessage = Splitter.split(message);

				ConfigsManager.getInstance().getExecutor()
						.execute(new Runnable() {

							@Override
							public void run() {

								String[] header_components = splittedMessage
										.getHeader().split(" ");

								if (!header_components[1].equals(ConfigsManager
										.getInstance().getVersion())) {
									System.err
											.println("Received message with protocol with different version");
									return;
								}

								String messageType = header_components[0]
										.trim();
								String fileId;
								int chunkNo;

								switch (messageType) {
								case ChunkRestore.CHUNK_COMMAND:

									fileId = header_components[2].trim();
									chunkNo = Integer
											.parseInt(header_components[3]
													.trim());

									Log.log("Received CHUNK command for file "
											+ fileId + " chunk " + chunkNo);
									Log.log("Size: "
											+ splittedMessage.getBody().length);

									for (ChunkRecord record : mSubscribedChunks) {
										if (record.fileId.equals(fileId)
												&& record.chunkNo == chunkNo) {

											FileChunkWithData requestedChunk = new FileChunkWithData(
													fileId, chunkNo,
													splittedMessage.getBody());

											ChunkRestore.getInstance()
													.addRequestedChunk(
															requestedChunk);

											mSubscribedChunks.remove(record);
											break;
										}
									}

									break;
								default:
									System.out
											.println("MDR received non recognized command");
									System.out.println(new String(message));
								}
							}

						});

			}
		} catch (HasToJoinException e1) {
			e1.printStackTrace();
		}
	}

	public synchronized void subscribeToChunkData(String fileId, long chunkNo) {
		mSubscribedChunks.add(new ChunkRecord(fileId, (int) chunkNo));
	}

	private class restoreListenerIPListener implements Runnable {

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
			while (ConfigsManager.getInstance().isAppRunning()) {
				byte[] buffer = new byte[BUFFER_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);

				try {
					restoreSocket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}

				final SenderRecord sender = new SenderRecord();
				sender.setAddr(packet.getAddress());
				sender.setPort(packet.getPort());

				String message = null;

				try {
					message = new String(packet.getData(),
							MulticastComunicator.ASCII_CODE).trim();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				final String[] components = message.trim().split(
						MulticastComunicator.CRLF + MulticastComunicator.CRLF);

				String[] headerComponents = components[0].trim().split(" ");

				switch (headerComponents[0]) {
				case ChunkRestore.CHUNK_COMMAND:
					final String fileId = headerComponents[2];
					final int chunkNo = Integer.parseInt(headerComponents[3]);

					ConfigsManager.getInstance().getExecutor()
							.execute(new Runnable() {

								@Override
								public void run() {
									for (ChunkRecord record : mSubscribedChunks) {
										if (record.fileId.equals(fileId)
												&& record.chunkNo == chunkNo) {
											byte[] data;
											try {
												data = components[1]
														.getBytes(MulticastComunicator.ASCII_CODE);

												FileChunkWithData requestedChunk = new FileChunkWithData(
														fileId, chunkNo, data);

												ChunkRestore.getInstance()
														.addRequestedChunk(
																requestedChunk);

												ChunkRestore
														.getInstance()
														.answerToChunkMessage(
																sender.getAddr(),
																sender.getPort());

												mSubscribedChunks
														.remove(record);
												break;
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
			restoreSocket.close();
		}
	}

}
