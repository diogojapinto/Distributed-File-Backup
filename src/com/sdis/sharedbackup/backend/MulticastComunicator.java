package com.sdis.sharedbackup.backend;

import java.net.InetAddress;

public class MulticastComunicator {
	private InetAddress addr;
	
	
	public MulticastComunicator(InetAddress addr, int port) {
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
			
		}).start();
	}
	
}
