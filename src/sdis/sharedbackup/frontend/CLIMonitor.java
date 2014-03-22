package sdis.sharedbackup.frontend;

import java.util.InputMismatchException;
import java.util.Scanner;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;

public class CLIMonitor {
	private static Scanner sc = new Scanner(System.in);
	private static boolean exit = false;

	public static int main(String[] args) {
		// TODO: create a BackupsDatabase
		// TODO: serialise the BackupsDatabase
		// TODO: the functionality is all implemented in ApplicationInterface
		// class, so that the functions may be called from another monitor, like
		// a gui

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

		return 0;
	}

	/*
	 * initiates the configuration of the Multicast addresses and ports
	 */
	private static void parseArgs(String[] args) throws ArgsException {
		if (args.length != 6) {
			throw new ArgsException();
		}

		if (!ConfigsManager.getInstance().setMulticastAddrs(args[0],
				Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]),
				args[4], Integer.parseInt(args[5]))) {
			throw new ArgsException();
		}
	}

	private static void setupService() {

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

	private static int promptAvailableSpace() throws InputMismatchException {
		// clearConsole();
		int allocatedSpace = 0;
		System.out.println("Choose your allocated space (KB):");
		allocatedSpace = sc.nextInt();
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
			}
		case 2:
			System.out.println("Enter new allocated space:");
			int space = sc.nextInt();
			// TODO: set new space (maxBackupSize);
			return false;
		case 3:
			System.out.println("Choose file to restore:");
			// String path = sc.next();
			// TODO: add file
			return false;
		case 4:
			System.out.println("Choose file to delete:");
			// String path = sc.next();
			// TODO: add file
			return false;
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

		public void error() {
			System.out
					.println("usage: java CLIMonitor <MCaddr> <MCport> <MDBaddr> <MDBport> <MDRaddr> <MDRport>");
			System.exit(1);
		}
	}

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
		System.out
				.println("Welcome to the Most Awesome Generic File Backup System Ever");
	}
}
