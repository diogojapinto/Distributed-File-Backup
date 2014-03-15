package com.sdis.sharedbackup.protocol;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

import com.sdis.sharedbackup.backend.ConfigManager;
import com.sdis.sharedbackup.backend.FileChunk;
import com.sdis.sharedbackup.backend.MulticastComunicator;

public class ChunkBackup {
	
	private static final String COMMAND = "PUTCHUNK";
	private static final byte[] CRLF = {0xD, 0xA};

	private static ChunkBackup sInstance = null;

	public static ChunkBackup getInstance() {
		
		if (sInstance == null) {
			sInstance = new ChunkBackup();
		}
		return sInstance;
	}

	private ChunkBackup() {
	}
	
	public boolean putChunk(FileChunk chunk) {
		String version = ConfigManager.getInstance().getVersion();
		
		String message = "";
		
		message += COMMAND;
		message += COMMAND;
		message += " " + version;
		message += " " + chunk.getFileId();
		message += " " + chunk.getChunkNo();
		message += " " + chunk.getDesiredReplicationDeg();
		message += " " + new String(CRLF);
		message += " " + new String(CRLF);
		message += " " + chunk.getData();
		
		InetAddress multDBAddr = ConfigManager.getInstance().getMDBAddr();
		int multDBPort = ConfigManager.getInstance().getMDBPort();
		
		MulticastComunicator sender = new MulticastComunicator(multDBAddr, multDBPort);
		sender.join();
		
		return sender.sendMessage(message);
		
	}
	
	public boolean storeChunks() {
		
		InetAddress multCtrlAddr = ConfigManager.getInstance().getMCAddr();
		int multCtrlPort = ConfigManager.getInstance().getMCPort();
		
		MulticastComunicator sender = new MulticastComunicator(multCtrlAddr, multCtrlPort);
		sender.join();
		
		
		// save the chunk to a file in a separate thread
		new Thread( new Runnable() {
			
			@Override
			public void run() {
				
			}
		}).start();
		
		return true;
	}

}
