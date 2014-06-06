package sdis.sharedbackup.frontend;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.FileAlreadySaved;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;
import sdis.sharedbackup.backend.SSLCommunicator;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import sdis.sharedbackup.utils.EnvironmentVerifier;

public class SSLServer {
    private static Scanner sc = new Scanner(System.in);
    private static boolean exit = false;

    private static int port;

    public static void main(String[] args) {
        try {
            parseArgs(args);
        } catch (ArgsException e) {
            e.error();
        }

        selectInterface();

        ConfigsManager.getInstance().setServer(true);

        clearConsole();

        setupService();

        try {
            ApplicationInterface.getInstance().startupService();
        } catch (ConfigurationsNotInitializedException e) {
            System.out
                    .println("Configurations haven't been correctly initialized");
            System.exit(1);
        }

        SSLCommunicator server = new SSLCommunicator(port);

        while (ConfigsManager.getInstance().isAppRunning()) {
            server.serverReceive();
        }

        ApplicationInterface.getInstance().terminate();

        System.exit(0);

    }

    private static void selectInterface() {
        ArrayList<String> interfaces = EnvironmentVerifier.getNetworkInterfaces();

        System.out.println("Please select an interface:");
        System.out.println();
        int i = 0;
        for (String intrfc : interfaces) {
            System.out.println("(" + ++i + ") " + intrfc);
        }

        Scanner sc = new Scanner(System.in);
        int choice;
        while (true) {
            try {
                System.out.println();
                System.out.print("Choice: ");
                choice = sc.nextInt();

                if (choice < 1 || choice > interfaces.size()) {
                    throw new InputMismatchException();
                }
                break;
            } catch (InputMismatchException e) {
                System.err.println("Invalid input");
            }
        }
        try {
            ConfigsManager.getInstance().setInterface(interfaces.get(i - 1));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /*
     * initiates the configuration of the Multicast addresses and ports
     */
    private static void parseArgs(String[] args) throws ArgsException {

        if (args.length != 7) {
            throw new ArgsException();
        }

        if (!ConfigsManager.getInstance().setMulticastAddrs(args[0],
                Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]),
                args[4], Integer.parseInt(args[5]))) {
            throw new ArgsException();
        }

        port = Integer.parseInt(args[6]);

    }

    private static void setupService() {
        ApplicationInterface.getInstance().startConfigsManager();

        if (!ApplicationInterface.getInstance().getDatabaseStatus()) {

            while (true) {
                try {
                    ApplicationInterface.getInstance().setAvailableDiskSpace(
                            1000);
                    break;
                } catch (InvalidBackupSizeException e) {
                    System.err.println("Please input a size greater than 0KB");
                    sc.nextLine();
                } catch (InputMismatchException e1) {
                    System.err.println("Please input a valid integer value");
                    sc.nextLine();
                }
            }

            while (true) {
                try {
                    ApplicationInterface.getInstance().setDestinationDirectory(
                            System.getProperty("user.dir"));
                    break;
                } catch (InvalidFolderException e) {
                    System.err.println("Folder does not exist!!");
                }
            }
            System.out.println("Setup Successfull");
        }
    }

    private static class ArgsException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public void error() {
            System.out
                    .println("usage: java Server <MCaddr> <MCport> <MDBaddr> <MDBport> <MDRaddr> <MDRport> " +
                            "<ServerPort>");
            System.exit(1);
        }
    }

    private static void clearConsole() {
        if (!ApplicationInterface.DEBUGG) {
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
        printHead();
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
