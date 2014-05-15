package sdis.sharedbackup.utils;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class EnvironmentVerifier {

	private static ArrayList<String> networkInterfaces = null;

	private EnvironmentVerifier() {
	}

	private static void configureNetworkInterfaces() {
		networkInterfaces = new ArrayList<>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			NetworkInterface ni = null;

			while (interfaces.hasMoreElements()) {
				ni = interfaces.nextElement();
				if (!ni.isLoopback()) {
					networkInterfaces.add(ni.getDisplayName());
				}
			}
		} catch (SocketException e2) {
			e2.printStackTrace();
		}
	}

	public static ArrayList<String> getNetworkInterfaces() {
		if (networkInterfaces == null) {
			configureNetworkInterfaces();
		}
		return networkInterfaces;
	}

	public static long getFolderSize(String folderPath) {

		File folder = new File(folderPath);

		if (folder.isDirectory()) {

			long length = 0;
			for (File file : folder.listFiles()) {
				if (file.isFile())
					length += file.length();
				else
					length += getFolderSize(file.getAbsolutePath());
			}
			return length;
		} else {
			return -1;
		}
	}
}
