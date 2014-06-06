package sdis.sharedbackup.frontend;

import sdis.sharedbackup.backend.SSLCommunicator;

import java.io.*;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Created by knoweat on 04/06/14.
 */
public class SSLClient {

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: SSLClient <username> <password> <hostname> <port> backup <filepath> " +
                    "<replication>");
            System.err.println("Usage: SSLClient <username> <password> <hostname> <port> restore");
            System.exit(1);
        }

        String username = args[0];
        String password = args[1];
        String hostname = args[2];
        int port = Integer.parseInt(args[3]);

        SSLCommunicator clientSkt = new SSLCommunicator(hostname, port);
        System.out.println("coiso");

        String request = SSLCommunicator.LOGIN + " " + username + " " + password;
        String response = clientSkt.clientSend(request);

        if (!response.equals(SSLCommunicator.SUCCESS)) {
            System.err.println("Login unsuccessful");
        } else {
            System.out.println("Login successful");
        }

        String action = args[4].toLowerCase();

        if (action.equals("backup")) {

            String filePath = args[5];
            int replication = Integer.parseInt(args[6]);

            request = SSLCommunicator.BACKUP + " " + filePath + " " + replication;

            response = clientSkt.clientSend(request);

            if (response.equals(SSLCommunicator.SUCCESS)) {
                System.err.println("Command unsuccessful");
                System.exit(1);
            }

            try {
                FileInputStream fins = new FileInputStream(filePath);

                response = clientSkt.clientSend(fins);

                if (response.equals(SSLCommunicator.SUCCESS)) {
                    System.out.println("Backup successful");
                } else {
                    System.err.println("Backup was not successful");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (action == "restore") {
            response = clientSkt.clientSend(SSLCommunicator.RESTORE);

            String[] files = response.trim().split(" ");

            System.out.println("Available files:");
            System.out.println();
            int i = 0;
            for (String file : files) {
                String[] fileComponents = file.trim().split(":");
                String filename = fileComponents[1].trim();
                System.out.println("(" + ++i + ") " + filename);
            }
            System.out.println();
            System.out.print("Input file index:");

            Scanner scanner = new Scanner(System.in);
            int option;
            while (true) {
                try {
                    option = scanner.nextInt();
                    scanner.nextLine();
                    if (option < 1 || option > files.length) {
                        throw new InputMismatchException();
                    }
                    break;
                } catch (InputMismatchException e) {
                    System.err.println("Invalid input");
                }
            }

            String[] fileComponents = files[i - 1].trim().split(":");
            String fileId = fileComponents[0].trim();
            String filepath = fileComponents[1].trim();

            request = SSLCommunicator.FILE + " " + fileId;

            try {
                // create file for saving
                File f = new File(Paths.get(filepath).getFileName().toString());
                if (!f.exists()) {
                    f.createNewFile();
                    FileOutputStream fout = new FileOutputStream(f.getName());
                    InputStream in;
                    in = clientSkt.clientSendCustomResponse(request);

                    int c;
                    while ((c = in.read()) != -1) {
                        fout.write(c);
                    }
                    fout.close();
                    System.out.println("Restore successful");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            clientSkt.terminate();
        }
    }
}
