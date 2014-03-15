package com.sdis.sharedbackup.backend;

import java.net.InetAddress;

public class MulticastControlListener implements Runnable {

	@Override
	public void run() {
		InetAddress addr = ConfigManager.getInstance().getMCAddr();
		int port = ConfigManager.getInstance().getMCPort();
		
		MulticastComunicator receiver = new MulticastComunicator(addr, port);
	}
}
