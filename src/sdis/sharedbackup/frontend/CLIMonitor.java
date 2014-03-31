package sdis.sharedbackup.frontend;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.FileAlreadySaved;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;

public class CLIMonitor {
	private static Scanner sc = new Scanner(System.in);
	private static boolean exit = false;

	public static void main(String[] args) {
		// TODO: verify every input
		// TODO: apanhar exceptions de address already in use

		try {
			parseArgs(args);
		} catch (ArgsException e) {
			e.error();
		}

		// clearConsole();
		printHead();

		setupService();

		try {
			ApplicationInterface.getInstance().startupService();
		} catch (ConfigurationsNotInitializedException e) {
			System.out
					.println("Configurations haven't been correctly initialized");
			System.exit(1);
		}

		while (!exit) {
			promptMenuOption();
		}

		sc.close();

		ApplicationInterface.getInstance().terminate();

		System.exit(0);
		;
	}

	/*
	 * initiates the configuration of the Multicast addresses and ports
	 */
	private static void parseArgs(String[] args) throws ArgsException {

		/*
		 * if (args.length != 6) { throw new ArgsException(); }
		 */

		/*
		 * if (!ConfigsManager.getInstance().setMulticastAddrs(args[0],
		 * Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]),
		 * args[4], Integer.parseInt(args[5]))) { throw new ArgsException(); }
		 */

		
		  ConfigsManager.getInstance().setMulticastAddrs("239.254.254.252",
		  Integer.parseInt("50001"), "239.254.254.253",
		  Integer.parseInt("50002"), "239.254.254.254",
		  Integer.parseInt("50003"));
		 

		/*ConfigsManager.getInstance()
				.setMulticastAddrs("239.0.0.1", Integer.parseInt("8765"),
						"239.0.0.1", Integer.parseInt("8766"), "239.0.0.1",
						Integer.parseInt("8767"));*/

	}

	private static void setupService() {
		ApplicationInterface.getInstance().startConfigsManager();

		if (!ApplicationInterface.getInstance().getDatabaseStatus()) {

			while (true) {
				try {
					ApplicationInterface.getInstance().setAvailableDiskSpace(
							promptAvailableSpace());
					break;
				} catch (InvalidBackupSizeException e) {
					System.err.println("Please input a size greater than 0KB");
				} catch (InputMismatchException e1) {
					System.err.println("Please input a valid integer value");
				}
			}

			while (true) {
				try {
					ApplicationInterface.getInstance().setDestinationDirectory(
							promptDestinationDir());
					break;
				} catch (InvalidFolderException e) {
					System.err.println("Folder does not exist!!");
				}
			}
			System.out.println("Setup Successfull");
		}
	}

	private static long promptAvailableSpace() throws InputMismatchException {
		// clearConsole();
		long allocatedSpace = 0;
		System.out.println("Choose your allocated space (KB):");
		allocatedSpace = sc.nextLong();
		sc.nextLine();
		return allocatedSpace;
	}

	private static String promptDestinationDir() {
		// clearConsole();
		System.out.println("Choose the folder to save the files to:");
		return sc.nextLine();
	}

	private static void promptMenuOption() {
		// clearConsole();
		do {
			System.out.println("Choose an option:");
			System.out.println("1-Backup file");
			System.out.println("2-Restore file");
			System.out.println("3-Delete a replicated file");
			System.out.println("4-Change allocated space");
			System.out.println("5-EXIT");

		} while (!processChoice());
	}

	private static boolean processChoice() {
		// TODO: catch errors in reads
		// clearConsole();
		// read choice
		int choice = sc.nextInt();
		sc.nextLine();

		// call corresponding interface method
		switch (choice) {
		// backup file
		case 1:
			System.out.println("Enter the path to the file:");
			String path = sc.nextLine();
			System.out.println("Enter desired Replication degree:");
			int repdeg = sc.nextInt();
			sc.nextLine();
			try {
				ApplicationInterface.getInstance().backupFile(path, repdeg);
				return true;
			} catch (FileTooLargeException e) {
				System.out.println("The selected file is too large");
				return false;
			} catch (FileDoesNotExistsExeption e) {
				System.out.println("The selected file does not exists");
				return false;
			} catch (FileAlreadySaved e) {
				System.out
						.println("The selected file is already in the database");
				return false;
			}
		case 4:
			System.out.println("Enter new allocated space:");
			int space = sc.nextInt();
			sc.nextLine();
			ApplicationInterface.getInstance().setNewSpace(space);
			return true;
		case 2:
			ArrayList<String> files = ApplicationInterface.getInstance()
					.getRestorableFiles();

			if (files.size() == 0) {
				System.out
						.println("The files you've backed up have are still saved in the file system");
				return true;
			}

			printFilesOrderedInfo(files);
			System.out.println("Choose file to restore:");
			int file_i = sc.nextInt();
			sc.nextLine();

			ApplicationInterface.getInstance().restoreFileByPath(
					files.get(file_i - 1));

			return true;
		case 3:
			ArrayList<String> deletableFiles = ApplicationInterface
					.getInstance().getDeletableFiles();
			if (deletableFiles.size() == 0) {
				System.out.println("There is no backed up files to delete");
				return true;
			}
			printFilesOrderedInfo(deletableFiles);
			System.out.println("Choose file to delete:");
			int i = sc.nextInt();
			sc.nextLine();
			try {
				ApplicationInterface.getInstance().deleteFile(
						deletableFiles.get(i - 1));
			} catch (FileDoesNotExistsExeption e) {
				System.out.println("The selected file does not exists");
			}

			return true;
		case 5:
			exit = true;
			System.out.println("Program will exit now");
			return true;
		default:
			System.out.println("Option does not exist");
			return false;
		}
	}

	private static class ArgsException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void error() {
			System.out
					.println("usage: java CLIMonitor <MCaddr> <MCport> <MDBaddr> <MDBport> <MDRaddr> <MDRport>");
			System.exit(1);
		}
	}

	private static void printFilesOrderedInfo(ArrayList<String> files) {

		int i = 1;
		System.out.println("Op. | Old file path");
		for (String path : files) {
			System.out.format("%3d | %s", i++, path);
		}
		System.out.println(" ");
	}

	@SuppressWarnings("unused")
	private static void clearConsole() {
		try {
			String os = System.getProperty("os.name");

			if (os.contains("Windows")) {
				Runtime.getRuntime().exec("cls");
			} else {
				Runtime.getRuntime().exec("clear");
			}
		} catch (Exception exception) {
			System.out.println("Could not clear console");
		}
	}

	private static void printHead() {
		System.out.println("Welcome to the...");
		System.out
				.println("   __  ___         __    ___                               ");
		System.out
				.println("  /  |/  /__  ___ / /_  / _ |_    _____ ___ ___  __ _  ___ ");
		System.out
				.println(" / /|_/ / _ \\(_-</ __/ / __ | |/|/ / -_|_-</ _ \\/  ' \\/ -_)");
		System.out
				.println("/_/  /_/\\___/___/\\__/ /_/ |_|__,__/\\__/___/\\___/_/_/_/\\__/ ");
		System.out.println("");
		System.out
				.println("  _____                 _       _____ __      ___           __           ");
		System.out
				.println(" / ___/__ ___  ___ ____(_)___  / __(_) /__   / _ )___ _____/ /____ _____ ");
		System.out
				.println("/ (_ / -_) _ \\/ -_) __/ / __/ / _// / / -_) / _  / _ `/ __/  '_/ // / _ \\");
		System.out
				.println("\\___/\\__/_//_/\\__/_/ /_/\\__/ /_/ /_/_/\\__/ /____/\\_,_/\\__/_/\\_\\_,_/ .__/");
		System.out
				.println("                                                                  /_/    ");
		System.out.println("   ____         __              ____            ");
		System.out.println("  / __/_ _____ / /____ __ _    / __/  _____ ____");
		System.out
				.println(" _\\ \\/ // (_-</ __/ -_)  ' \\  / _/| |/ / -_) __/");
		System.out
				.println("/___/\\_, /___/\\__/\\__/_/_/_/ /___/|___/\\__/_/   ");
		System.out.println("    /___/                                       ");
		System.out.println();
	}
}
