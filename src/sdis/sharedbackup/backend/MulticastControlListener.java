package sdis.sharedbackup.backend;

import java.net.InetAddress;
import java.util.ArrayList;

import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.utils.Log;

/*
 * Class that receives and dispatches messages from the multicast control channel
 */
public class MulticastControlListener implements Runnable {


	public ArrayList<FileChunk> mPendingChunks;
	public ArrayList<ChunkRecord> mSentChunks;

	public static ArrayList<ChunkRecord> interestingChunks;

	private static MulticastControlListener mInstance = null;



	private MulticastControlListener() {
		interestingChunks = new ArrayList<ChunkRecord>();
		mSentChunks = new ArrayList<ChunkRecord>();
		mPendingChunks = new ArrayList<FileChunk>();
	}

	public static MulticastControlListener getInstance() {
		if (mInstance == null) {
			mInstance = new MulticastControlListener();
		}
		return mInstance;
	}

	@Override
	public void run() {
		System.out.println("MC Listener started");

		InetAddress addr = ConfigsManager.getInstance().getMCAddr();
		int port = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator receiver = new MulticastComunicator(addr, port);

		receiver.join();

		Log.log("Listening on " + addr.getHostAddress() + ":" + port);

		try {
			while (ConfigsManager.getInstance().isAppRunning()) {

				final SenderRecord sender = new SenderRecord();

				final String message;

				message = receiver.receiveMessage(sender);

				ConfigsManager.getInstance().getExecutor()
						.execute(new MulticastControlHandler(message,sender));
			}
		} catch (HasToJoinException e1) {
			e1.printStackTrace();
		}
	}
	
	//TODO Para que e isto?
	public synchronized void notifyChunk(String fileId, int chunkNo) {
		for (ChunkRecord record : interestingChunks) {
			if (record.fileId.equals(fileId) && record.chunkNo == chunkNo) {
				record.isNotified = true;
			}
		}
	}
}
