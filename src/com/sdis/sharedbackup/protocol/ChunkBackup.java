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
		
		try {
			byte[] raw_msg = message.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		InetAddress multCtrlAddr = ConfigManager.getInstance().getMCAddr();
		int multCtrlPort = ConfigManager.getInstance().getMCPort();
		
		
		return true;
	}
	
	public boolean storeChunks() {
		// TODO: this function shall wait for requests, and act accordingly in a separate thread
		return true;
	}

}
