package sdis.sharedbackup.backend;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastComunicator {
	private static int TTL = 1;

	private MulticastSocket mMSocket;
	private InetAddress mAddr;
	private int mPort;

	private static final int MAX_PACKET_SIZE = 70000;
	public static final String ASCII_CODE = "US-ASCII";

	public static final String CRLF = new String("\r\n");

	public MulticastComunicator(InetAddress addr, int port) {
		this.mAddr = addr;
		this.mPort = port;
		this.mMSocket = null;
	}

	public void join() {
		try {
			mMSocket = new MulticastSocket(mPort);
			mMSocket.setTimeToLive(TTL);
		} catch (IOException e) {
			System.out.println("Could not create MulticastSocket.");
			e.printStackTrace();
			System.exit(1);
		}

		try {
			mMSocket.joinGroup(mAddr);
		} catch (IOException e) {
			System.out.println("Could not join multicast group.");
			e.printStackTrace();
			System.exit(1);
		}

	}

	public boolean sendMessage(String messg) throws HasToJoinException {

		try {
			mMSocket = new MulticastSocket();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		byte[] bytesMsg = null;

		try {
			bytesMsg = messg.getBytes(ASCII_CODE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		// TODO: remove
		try {
			FileOutputStream out = new FileOutputStream("messg_send");
			try {
				out.write(bytesMsg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		DatagramPacket packet = new DatagramPacket(bytesMsg, bytesMsg.length,
				mAddr, mPort);

		try {
			mMSocket.send(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public String receiveMessage() throws HasToJoinException {

		if (mMSocket == null) {
			throw new HasToJoinException();
		}

		byte[] bytesMsg = new byte[MAX_PACKET_SIZE];

		DatagramPacket packet = new DatagramPacket(bytesMsg, MAX_PACKET_SIZE);

		try {
			mMSocket.receive(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return null;
		}

		// TODO: remove
		try {
			FileOutputStream out = new FileOutputStream("messg_send");
			try {
				out.write(packet.getData());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String returnStr;

		try {
			returnStr = new String(packet.getData(), ASCII_CODE);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Could not parse received message");
			e.printStackTrace();
			return null;
		}

		return returnStr;
	}

	/*
	 * Receives as argument a SenderRecord to be initialized in this function
	 */
	public String receiveMessage(SenderRecord record) throws HasToJoinException {

		if (mMSocket == null) {
			throw new HasToJoinException();
		}

		byte[] bytesMsg = new byte[MAX_PACKET_SIZE];

		DatagramPacket packet = new DatagramPacket(bytesMsg, bytesMsg.length);

		try {
			mMSocket.receive(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return null;
		}

		if (record != null) {
			record.setAddr(packet.getAddress());
			record.setPort(packet.getPort());
		}

		String returnStr;

		returnStr = new String(packet.getData());
		/*
		 * try { returnStr = new String(packet.getData(), ASCII_CODE); } catch
		 * (UnsupportedEncodingException e) {
		 * System.err.println("Could not parse received message");
		 * e.printStackTrace(); return null; }
		 */

		return returnStr;
	}

	public static class HasToJoinException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

}
