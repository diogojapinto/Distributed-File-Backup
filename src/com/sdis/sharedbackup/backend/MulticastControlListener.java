package com.sdis.sharedbackup.backend;

import java.net.InetAddress;

/*
 * Class that receives and dispatches messages from the multicast control channel
 */
public class MulticastControlListener implements Runnable {

	@Override
	public void run() {
		InetAddress addr = ConfigManager.getInstance().getMCAddr();
		int port = ConfigManager.getInstance().getMCPort();
		
		MulticastComunicator receiver = new MulticastComunicator(addr, port);
	}
}
