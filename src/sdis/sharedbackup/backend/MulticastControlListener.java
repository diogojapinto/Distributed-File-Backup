package sdis.sharedbackup.backend;

import java.net.InetAddress;

/*
 * Class that receives and dispatches messages from the multicast control channel
 */
public class MulticastControlListener implements Runnable {

	@Override
	public void run() {
		InetAddress addr = ConfigsManager.getInstance().getMCAddr();
		int port = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator receiver = new MulticastComunicator(addr, port);

		while (true) {
			String message = receiver.receiveMessage();
		}
	}
}
