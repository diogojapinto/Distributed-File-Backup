package sdis.sharedbackup.backend;

import java.net.InetAddress;

import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.utils.Log;
import sdis.sharedbackup.utils.SplittedMessage;
import sdis.sharedbackup.utils.Splitter;

/*
 * Class that receives and dispatches messages from the multicast data backup channel
 */
public class MulticastDataBackupListener implements Runnable {

	private static MulticastDataBackupListener mInstance = null;

	private MulticastDataBackupListener() {
		
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
				final byte[] message;

				message = receiver.receiveMessage();
				
				SplittedMessage splittedMessage= Splitter.split(message);
				
				ConfigsManager.getInstance().getExecutor()
						.execute(new MulticastDataBackupHandler(splittedMessage));

			}
		} catch (HasToJoinException e1) {
			e1.printStackTrace();
		}
	}
}