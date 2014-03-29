package sdis.sharedbackup.backend;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.utils.Log;

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
		System.out.println("MDB listener started");
		InetAddress addr = ConfigsManager.getInstance().getMDBAddr();
		int port = ConfigsManager.getInstance().getMDBPort();

		MulticastComunicator receiver = new MulticastComunicator(addr, port);

		receiver.join();

		Log.log("Listening on " + addr.getHostAddress() + ":" + port);

		try {
			while (ConfigsManager.getInstance().isAppRunning()) {
				final String message;

				message = receiver.receiveMessage();

				ConfigsManager.getInstance().getExecutor()
						.execute(new Runnable() {

							@Override
							public void run() {
								Log.log("MDB:Received message");

								final String[] components;
								String separator = MulticastComunicator.CRLF
										+ MulticastComunicator.CRLF;

								components = message.trim().split(separator);

								String header = components[0].trim();

								final String[] header_components = header
										.split(" ");

								if (!header_components[1].equals(ConfigsManager
										.getInstance().getVersion())
										&& !header_components[1]
												.equals(ConfigsManager
														.getInstance()
														.getEnhancementsVersion())) {
									System.err
											.println("Received message with protocol with different version");
									return;
								}

								String messageType = header_components[0]
										.trim();

								switch (messageType) {
								case ChunkBackup.PUT_COMMAND:

									String fileId = header_components[2].trim();
									int chunkNo = Integer
											.parseInt(header_components[3]
													.trim());
									int desiredReplication = Integer
											.parseInt(header_components[4]
													.trim());

									if (ConfigsManager.getInstance()
											.getBackupDirActualSize()
											+ FileChunk.MAX_CHUNK_SIZE >= ConfigsManager
											.getInstance().getMaxBackupSize()) {
										return;
									}

									if (ConfigsManager.getInstance()
											.getFileById(fileId) == null) {
										return;
									}

									FileChunk savedChunk = ConfigsManager
											.getInstance().getSavedChunk(
													fileId, chunkNo);
									// file not yet saved
									if (savedChunk == null) {
										FileChunk pendingChunk = new FileChunk(
												fileId, chunkNo,
												desiredReplication);

										synchronized (mPendingChunks) {
											mPendingChunks.add(pendingChunk);
										}

										try {
											Thread.sleep(mRand
													.nextInt(MAX_WAIT_TIME));
										} catch (InterruptedException e) {
											e.printStackTrace();
										}

										// verify if it is
										// needed to save the
										// chunk

										if (pendingChunk
												.getCurrentReplicationDeg() < pendingChunk
												.getDesiredReplicationDeg()) {

											ChunkBackup
													.getInstance()
													.storeChunk(
															pendingChunk,
															components[1]
																	.getBytes());
											/*
											 * try { ChunkBackup .getInstance()
											 * .storeChunk( pendingChunk,
											 * components[1] .getBytes
											 * (MulticastComunicator
											 * .ASCII_CODE));
											 * 
											 * } catch (
											 * UnsupportedEncodingException e) {
											 * e.printStackTrace (); }
											 */

										}

										// remove the temporary
										// chunk from the
										// pending
										// chunks
										// list
										synchronized (mPendingChunks) {
											mPendingChunks.remove(pendingChunk);
										}
									}

									break;
								default:
									System.out
											.println("MDB received non recognized command");
									System.out.println(message);
								}
							}
						});
			}
		} catch (HasToJoinException e1) {
			e1.printStackTrace();
		}
	}
}
