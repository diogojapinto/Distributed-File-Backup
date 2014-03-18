package com.sdis.sharedbackup.backend;

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
	private boolean mHasJoined;

	private static final int MAX_PACKET_SIZE = 70000;
	private static final String ASCII_CODE = "US-ASCII";

	public MulticastComunicator(InetAddress addr, int port) {
		this.mAddr = addr;
		this.mPort = port;
		this.mHasJoined = false;
		this.mMSocket = null;
	}

	public void join() {
		try {
			mMSocket = new MulticastSocket(mPort);
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
		
		mHasJoined = true;
	}

	public boolean sendMessage(String messg) {
		if (!mHasJoined) {
			System.out.println("Cannot send message before joining group");
			return false;
		}
		byte[] bytesMsg = null;
		try {
			bytesMsg = messg.getBytes(ASCII_CODE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		DatagramPacket packet = new DatagramPacket(bytesMsg, bytesMsg.length);getClass();
		try {
			mMSocket.setTimeToLive(TTL);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			mMSocket.send(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public String receiveMessage() {
		byte[] bytesMsg = new byte[MAX_PACKET_SIZE];

		DatagramPacket packet = new DatagramPacket(bytesMsg, bytesMsg.length);

		try {
			mMSocket.receive(packet);
		} catch (IOException e) {
			System.err.println("Failed to send packet");
			e.printStackTrace();
			return null;
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

}
