package sdis.sharedbackup.protocols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

import sdis.sharedbackup.backend.*;
import sdis.sharedbackup.backend.MulticastCommunicator;
import sdis.sharedbackup.backend.MulticastCommunicator.HasToJoinException;
import sdis.sharedbackup.utils.Log;

public class ChunkRestore {

    public static final int MAX_RESTORE_TRIES = 5;
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

		String message = "";

		message += GET_COMMAND + " " + fileId + " " + chunkNo
				+ MulticastCommunicator.CRLF + MulticastCommunicator.CRLF;

		InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
		int multCPort = ConfigsManager.getInstance().getMCPort();

		MulticastCommunicator sender = new MulticastCommunicator(multCAddr,
				multCPort);

		MulticastDataRestoreListener.getInstance().subscribeToChunkData(fileId,
				chunkNo);

        int nrTries = 0;

		do {
			try {
				sender.sendMessage(message
						.getBytes(MulticastCommunicator.ASCII_CODE));
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
            nrTries++;
		} while (retChunk == null && nrTries < MAX_RESTORE_TRIES);

		return retChunk;
	}

	public boolean sendChunk(FileChunk chunk) {

		InetAddress multDRAddr = ConfigsManager.getInstance().getMDRAddr();
		int multDRPort = ConfigsManager.getInstance().getMDRPort();

		String header = "";

		header += CHUNK_COMMAND + " " + chunk.getFileId() + " "
				+ chunk.getChunkNo() + MulticastCommunicator.CRLF
				+ MulticastCommunicator.CRLF;

		byte[] data = chunk.getData();

		byte[] message = new byte[header.length() + data.length];

		try {
			System.arraycopy(header.getBytes(MulticastCommunicator.ASCII_CODE),
					0, message, 0, header.length());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		System.arraycopy(data, 0, message, header.length(), data.length);

		MulticastCommunicator sender = new MulticastCommunicator(multDRAddr,
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

		String header = "";

		header += CHUNK_COMMAND + " " + chunk.getFileId() + " "
				+ chunk.getChunkNo() + MulticastCommunicator.CRLF
				+ MulticastCommunicator.CRLF;

		byte[] data = chunk.getData();

		byte[] message = new byte[header.length() + data.length];

		try {
			System.arraycopy(header.getBytes(MulticastCommunicator.ASCII_CODE),
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

	public void answerToChunkMessage(InetAddress addr, int port, FileChunk chunk) {

		String message = "";

		message += CHUNK_CONFIRMATION + " " + chunk.getFileId() + " "
				+ chunk.getChunkNo() + MulticastCommunicator.CRLF
				+ MulticastCommunicator.CRLF;

		DatagramSocket socket = null;

		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		DatagramPacket packet = null;
		try {
			packet = new DatagramPacket(
					message.getBytes(MulticastCommunicator.ASCII_CODE),
					message.getBytes(MulticastCommunicator.ASCII_CODE).length,
					addr, port);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		
		Log.log("Sent message to IP: " + message);

		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}

		socket.close();

		Log.log("Answered to CHUNK command to IP");
	}

	public synchronized void addRequestedChunk(FileChunkWithData chunk) {
		mRequestedChunks.add(chunk);
	}
}
