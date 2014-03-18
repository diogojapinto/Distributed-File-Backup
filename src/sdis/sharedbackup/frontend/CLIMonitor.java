package sdis.sharedbackup.frontend;

import java.util.InputMismatchException;
import java.util.Scanner;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;

public class CLIMonitor {
	private static Scanner sc = null;
	private static boolean exit;
	public static void main(String[] args) {
		// TODO: serialize the config manager
		exit =false;
		System.out.println("Welcome to XXXX");
		sc = new Scanner(System.in);
		startSetup();
		while(exit==false){
		showMenu();
		processChoice();
		}
		try {
			parseArgs(args);
		} catch (ArgsException e) {
			e.error();
		}

		try {
			ConfigsManager.getInstance().init();
		} catch (ConfigurationsNotInitializedException e) {
			System.out
					.println("Configurations haven't been correctly initialized");
			e.printStackTrace();
		}
		sc.close();

	}

	// initiates the configuration of the Multicast addresses and ports
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

	private static void startSetup() {

	
		while (true) {
			try {
				System.out.println("Choose your allocated space (KB):");
				int allocatedSpace = sc.nextInt();
				sc.nextLine();
				break;
			} catch (InputMismatchException e) {
				System.out.println("Invalid input");
				System.out.println("");
			}
		}

		System.out.println("Choose the folder to save the files to:");
		String saveFolder = sc.nextLine();
		System.out.println("Setup Successfull!");
		

	}

	private static void showMenu() {
		System.out.println("Choose an option:");
		System.out.println("1-Add a file to the backup");
		System.out.println("2-Change allocated space");
		System.out.println("3-Restore file");
		System.out.println("4-Delete a replicated file");
		System.out.println("5-EXIT");
	}

	private static void processChoice() { 
		int choice = sc.nextInt();
		sc.nextLine();
		switch (choice) {
		case 1:
			System.out.println("Enter the path to the file:");
			String path = sc.nextLine();
			// TODO: add file																																																	
			break;
		case 2:
			System.out.println("Enter new allocated space:");
			int space = sc.nextInt();
			// TODO: set new space
			break;
		case 3:
			System.out.println("Choose file to restore:");
			// String path = sc.next();
			// TODO: add file
			break;
		case 4:
			System.out.println("Choose file to delete:");
			// String path = sc.next();
			// TODO: add file
			break;
		case 5:
			exit = true;
			System.out.println("Program will exit now");
			break;
		default:
			System.out.println("Option does not exist!!!");			
			break;
		}
	}

	private static class ArgsException extends Exception {

		public void error() {
			System.out
					.println("usage: java CLIMonitor <MCaddr> <MCport> <MDBaddr> <MDBport> <MDRaddr> <MDRport>");
			System.exit(1);
		}
	}
	
	private static void clearConsole()
	{
	    try
	    {
	        String os = System.getProperty("os.name");

	        if (os.contains("Windows"))
	        {
	            Runtime.getRuntime().exec("cls");
	        }
	        else
	        {
	            Runtime.getRuntime().exec("clear");
	        }
	    }
	    catch (Exception exception)
	    {
	        System.out.println("Could not clear console");
	    }
	}
}
