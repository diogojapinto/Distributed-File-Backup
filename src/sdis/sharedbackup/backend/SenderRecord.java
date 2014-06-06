package sdis.sharedbackup.backend;

import java.net.InetAddress;

public class SenderRecord {
	private InetAddress addr;
	private int port;

	public InetAddress getAddr() {
		return addr;
	}

	public int getPort() {
		return port;
	}

	public void setAddr(InetAddress addr) {
		this.addr = addr;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
