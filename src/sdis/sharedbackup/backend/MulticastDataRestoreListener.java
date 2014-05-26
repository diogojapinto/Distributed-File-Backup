package sdis.sharedbackup.backend;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.protocols.ChunkRestore;
import sdis.sharedbackup.utils.Log;
import sdis.sharedbackup.utils.SplittedMessage;
import sdis.sharedbackup.utils.Splitter;

import javax.net.ssl.SSLSocket;

/*
 * Class that receives and dispatches messages from the multicast data restore channel
 */
public class MulticastDataRestoreListener implements Runnable {

	private static final int BUFFER_SIZE = 70000;

	private static MulticastDataRestoreListener mInstance = null;

	public ArrayList<ChunkRecord> mSubscribedChunks;

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

		while (ConfigsManager.getInstance().isAppRunning()) {
			final byte[] message;
			try {
				message = receiver.receiveMessage();
				final SplittedMessage splittedMessage = Splitter.split(message);
				Log.log("Received a msg on MDR: " + splittedMessage.getHeader()
						+ " " + splittedMessage.getBody().length);
				ConfigsManager
						.getInstance()
						.getExecutor()
						.execute(
								new MulticastDataRestoreHandler(splittedMessage));
			} catch (HasToJoinException e) {

				e.printStackTrace();
			}
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
				
				byte[] message = new byte[packet.getLength()];
				System.arraycopy(buffer, 0, message, 0, packet.getLength());

				final SenderRecord sender = new SenderRecord();
				sender.setAddr(packet.getAddress());
				sender.setPort(packet.getPort());

				final SplittedMessage splittedMessage = Splitter
						.split(message);

				String[] headers = splittedMessage.getHeader().split(
						MulticastComunicator.CRLF);

				String[] header_components = headers[0].split(" ");

				switch (header_components[0]) {
				case ChunkRestore.CHUNK_COMMAND:
					final String fileId = header_components[2];
					final int chunkNo = Integer.parseInt(header_components[3]);

					ConfigsManager.getInstance().getExecutor()
							.execute(new Runnable() {

								@Override
								public void run() {
									for (ChunkRecord record : mSubscribedChunks) {
										if (record.fileId.equals(fileId)
												&& record.chunkNo == chunkNo) {
											byte[] data;
											data = splittedMessage.getBody();

											FileChunkWithData requestedChunk = new FileChunkWithData(
													fileId, chunkNo, data);

											ChunkRestore.getInstance()
													.addRequestedChunk(
															requestedChunk);

											ChunkRestore
													.getInstance()
													.answerToChunkMessage(
															sender.getAddr(),
															ChunkRestore.ENHANCEMENT_RESPONSE_PORT,
															requestedChunk);

											mSubscribedChunks.remove(record);
											break;
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
