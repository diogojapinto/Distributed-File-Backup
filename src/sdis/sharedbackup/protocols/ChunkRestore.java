package sdis.sharedbackup.protocols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.FileChunkWithData;
import sdis.sharedbackup.backend.MulticastComunicator;
import sdis.sharedbackup.backend.MulticastComunicator.HasToJoinException;
import sdis.sharedbackup.backend.MulticastDataRestoreListener;
import sdis.sharedbackup.utils.Log;

public class ChunkRestore {

	private static ChunkRestore sInstance = null;

	public static final String GET_COMMAND = "GETCHUNK";
	public static final String CHUNK_COMMAND = "CHUNK";
	public static final String CHUNK_CONFIRMATION = "CHUNKCONFIRM";
	public static final int ENHANCEMENT_SEND_PORT = 50555;
	public static final int ENHANCEMENT_RESPONSE_PORT = 50556;
	private static final int REQUEST_TIME_INTERVAL = 500;

	private ArrayList<FileChunkWithData> mRequestedChunks;

	public static ChunkRestore getInstance() {

		if (sInstance == null) {
			sInstance = new ChunkRestore();
		}
		return sInstance;
	}

	private ChunkRestore() {
		mRequestedChunks = new ArrayList<FileChunkWithData>();
	}

	public FileChunkWithData requestChunk(String fileId, long chunkNo) {

		FileChunkWithData retChunk = null;

		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		message += GET_COMMAND + " " + version + " " + fileId + " " + chunkNo
				+ MulticastComunicator.CRLF + MulticastComunicator.CRLF;

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastComunicator sender = new MulticastComunicator(multCAddr,
				multCPort);

		MulticastDataRestoreListener.getInstance().subscribeToChunkData(fileId,
				chunkNo);

		do {
			try {
				sender.sendMessage(message
						.getBytes(MulticastComunicator.ASCII_CODE));
			} catch (HasToJoinException e1) {
				e1.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(REQUEST_TIME_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (mRequestedChunks) {
				for (FileChunkWithData chunk : mRequestedChunks) {
					if (chunk.getFileId().equals(fileId)
							&& chunk.getChunkNo() == chunkNo) {
						retChunk = chunk;
						mRequestedChunks.remove(chunk);
						break;
					}
				}
			}

		} while (retChunk == null);

		return retChunk;
	}

	public boolean sendChunk(FileChunk chunk) {

		InetAddress multDRAddr = ConfigsManager.getInstance().getMDRAddr();
		int multDRPort = ConfigsManager.getInstance().getMDRPort();

		String version = ConfigsManager.getInstance().getVersion();

		String header = "";

		header += CHUNK_COMMAND + " " + version + " " + chunk.getFileId() + " "
				+ chunk.getChunkNo() + MulticastComunicator.CRLF
				+ MulticastComunicator.CRLF;

		byte[] data = chunk.getData();

		byte[] message = new byte[header.length() + data.length];

		try {
			System.arraycopy(header.getBytes(MulticastComunicator.ASCII_CODE),
					0, message, 0, header.length());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		System.arraycopy(data, 0, message, header.length(), data.length);

		MulticastComunicator sender = new MulticastComunicator(multDRAddr,
				multDRPort);

		try {
			sender.sendMessage(message);
		} catch (HasToJoinException e) {
			e.printStackTrace();
		}

		Log.log("Sent CHUNK command to MULTICAST in response to request of "
				+ chunk.getFileId() + " no " + chunk.getChunkNo());

		return true;
	}

	public boolean sendChunk(FileChunk chunk, InetAddress destinationAddress,
			int destinationPort) {

		String version = ConfigsManager.getInstance().getVersion();

		String header = "";

		header += CHUNK_COMMAND + " " + version + " " + chunk.getFileId() + " "
				+ chunk.getChunkNo() + MulticastComunicator.CRLF
				+ MulticastComunicator.CRLF;

		byte[] data = chunk.getData();

		byte[] message = new byte[header.length() + data.length];

		try {
			System.arraycopy(header.getBytes(MulticastComunicator.ASCII_CODE),
					0, message, 0, header.length());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		System.arraycopy(data, 0, message, header.length(), data.length);

		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		DatagramPacket packet = null;

		packet = new DatagramPacket(message, message.length,
				destinationAddress, destinationPort);

		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		socket.close();

		Log.log("Sent CHUNK command to IP in response to request of "
				+ chunk.getFileId() + " no " + chunk.getChunkNo());

		return true;
	}

	public void answerToChunkMessage(InetAddress addr, int port) {
		String version = ConfigsManager.getInstance().getVersion();

		String message = "";

		message += CHUNK_CONFIRMATION + " " + version
				+ MulticastComunicator.CRLF + MulticastComunicator.CRLF;

		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		DatagramPacket packet = null;
		try {
			packet = new DatagramPacket(
					message.getBytes(MulticastComunicator.ASCII_CODE),
					message.getBytes(MulticastComunicator.ASCII_CODE).length,
					addr, port);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}

		socket.close();

		Log.log("Answerd to CHUNK command to IP");
	}

	public synchronized void addRequestedChunk(FileChunkWithData chunk) {
		mRequestedChunks.add(chunk);
	}
}
