package com.sdis.sharedbackup.frontend;

import com.sdis.sharedbackup.backend.ConfigManager;

public class CLIMonitor {

	public static void main(String[] args) {
		// TODO: parse xml
		// TODO: present menu
		try {
			parseArgs(args);
		} catch (ArgsException e) {
			e.error();
		}
	}

	// initiates the configuration of the Multicast addresses and ports
	private static void parseArgs(String[] args) throws ArgsException {
		if (args.length != 6) {
			throw new ArgsException();
		}

		if (!ConfigManager.getInstance().setMulticastAddrs(args[0],
				Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]),
				args[4], Integer.parseInt(args[5]))) {
			throw new ArgsException();
		}
	}

	private static class ArgsException extends Exception {

		public void error() {
			System.out
					.println("usage: java CLIMonitor <MCaddr> <MCport> <MDBaddr> <MDBport> <MDRaddr> <MDRport>");
			System.exit(1);
		}
	}
}
