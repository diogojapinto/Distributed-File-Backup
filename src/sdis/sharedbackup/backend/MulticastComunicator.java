package sdis.sharedbackup.backend;

import java.io.IOException;
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

	public boolean sendMessage(byte[] messg) throws HasToJoinException {

		try {
			mMSocket = new MulticastSocket();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		DatagramPacket packet = new DatagramPacket(messg, messg.length,
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

	public byte[] receiveMessage() throws HasToJoinException {

		if (mMSocket == null) {
			throw new HasToJoinException();
		}

		byte[] buffer = new byte[MAX_PACKET_SIZE];

		DatagramPacket packet = new DatagramPacket(buffer, MAX_PACKET_SIZE);

		try {
			mMSocket.receive(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return null;
		}
		
		byte[] message = new byte[packet.getLength()];
		System.arraycopy(buffer, 0, message, 0, packet.getLength());
		

		return message;
	}

	/*
	 * Receives as argument a SenderRecord to be initialized in this function
	 */
	public byte[] receiveMessage(SenderRecord record) throws HasToJoinException {

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

		return packet.getData();
	}

	public static class HasToJoinException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

}
