package sdis.sharedbackup.backend;

import sdis.sharedbackup.frontend.ApplicationInterface;
import sdis.sharedbackup.functionality.FileRestore;
import sdis.sharedbackup.protocols.FileRecord;
import sdis.sharedbackup.utils.Log;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.ArrayList;

public class SSLCommunicator {

    public static final String FAILURE = "Failure";
    public static final String RESTORE = "Restore";
    public static final String BACKUP = "Backup";
    public static final String SUCCESS = "Success";
    public static final String LIST = "List";
    public static final String FILE = "File";
    public static final String LOGIN = "Login";
    private KeyStore ks;
    SSLSocket clientSocket;
    SSLServerSocket serverSocket;
    private boolean isServer = false;

    public SSLCommunicator(String hostName, int portNumber) {
        initCertificate();
        InetAddress host = null;
        try {
            host = InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            clientSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            clientSocket.startHandshake();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SSLCommunicator(int portNumber) {
        initCertificate();

        try {
            isServer = true;
            serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }

        serverSocket.setNeedClientAuth(true);
    }

    private void initCertificate() {
        //init keystore
        try {
            ks = KeyStore.getInstance("JKS");
            ks.load(SSLCommunicator.class.getClassLoader().getResourceAsStream("resources/peer.keys"),
                    "123456".toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "123456".toCharArray());
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLContext.setDefault(ctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Client sends a message, function returns the server response
    public String clientSend(String message) {
        if (isServer) {
            return null;
        }

        // send packet
        PrintWriter out = null;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.println(message);

        // read answer
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String serverReply = null;
        try {
            serverReply = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return serverReply;
    }

    // Client sends a message, function returns the server response
    public InputStream clientSendCustomResponse(String message) {
        if (isServer) {
            return null;
        }
        try {
            clientSocket.startHandshake();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // send packet
        PrintWriter out = null;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.println(message);

        try {
            return clientSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Client sends a message, function returns the server response
    public String clientSend(InputStream fin) {
        if (isServer) {
            return null;
        }
        try {
            clientSocket.startHandshake();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // send packet
        PrintWriter out = null;
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int character;
        try {
            while((character = fin.read()) != -1) {
                out.write(character);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.println();

        // read answer
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String serverReply = null;
        try {
            serverReply = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return serverReply;
    }

    //receives a message and replies accordingly
    public void serverReceive() {
        if (!isServer) {
            return;
        }
        // enable client authentication
        serverSocket.setNeedClientAuth(true);

        while (true) {
            try {
                final SSLSocket respSocket = (SSLSocket) serverSocket.accept();

                Thread processRequest = new Thread(new Runnable() {
                    public void run() {

                        PrintWriter out = null;
                        BufferedReader in = null;

                        //receive packet
                        try {
                            in = new BufferedReader(new InputStreamReader(respSocket.getInputStream()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //respond
                        try {
                            out = new PrintWriter(respSocket.getOutputStream(), true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            requestStart(in, out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });

                processRequest.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void requestStart(BufferedReader in, PrintWriter out) throws IOException, InterruptedException {
        String clientMessg = null;

        try {
            clientMessg = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] components = clientMessg.trim().split(" ");
        String option = components[0].trim();
        String username = components[1].trim();
        String password = components[2].trim();
        String reply;

        if (!option.equals(LOGIN)) {
            reply = FAILURE;
            out.println(reply);
            in.close();
            out.close();
            return;
        }

        User user = ConfigsManager.getInstance().getSDatabase().login(username, password);

        if (user == null) {
            reply = SUCCESS;
            Log.log("Valid login: " + username);
        } else {
            Log.log("Invalid login: " + username);
            reply = FAILURE;
            out.println(reply);
            Thread.sleep(1000);
            in.close();
            out.close();
            return;
        }

        out.println(reply);

        try {
            clientMessg = in.readLine();
        } catch (IOException e) {
            reply = FAILURE;
            out.println(reply);
            Thread.sleep(1000);
            in.close();
            out.close();
            return;
        }

        components = clientMessg.trim().split(" ");
        String command = components[0].trim();

        switch (command) {
            case RESTORE:
                requestRestore(in, out, user);
                break;
            case BACKUP:
                // Backup <filename> <replication>
                if (components.length != 3) {
                    Log.log("Invalid command");
                    reply = FAILURE;
                    out.println(reply);
                    Thread.sleep(1000);
                    in.close();
                    out.close();
                    return;
                }
                String filename = components[1].trim();
                int replication = Integer.parseInt(components[2].trim());
                requestBackup(in, out, user, filename, replication);
                break;
            default:
                Log.log("Invalid command");
                reply = FAILURE;
                out.println(reply);
                Thread.sleep(1000);
                in.close();
                out.close();
                return;
        }
    }

    private void requestBackup(BufferedReader in, PrintWriter out, User user, String fileName, int replication) throws
            IOException, InterruptedException {
        FileOutputStream fouts = new FileOutputStream(fileName);

        String reply;

        int character = -1;
        while ((character = in.read()) != -1) {
            fouts.write(character);
        }

        fouts.close();

        try {
            if (ApplicationInterface.getInstance().backupFile(fileName, replication, user.getAccessLevel())) {
                reply = SUCCESS;
                Log.log("Backup successful");
            } else {
                reply = FAILURE;
                Log.log("Backup failed");
            }
            out.println(reply);
            Thread.sleep(1000);
            in.close();
            out.close();
            return;

        } catch (Exception e) {
            reply = FAILURE;
            out.println(reply);
            Thread.sleep(1000);
            in.close();
            out.close();
            return;
        }

    }


    private void requestRestore(BufferedReader in, PrintWriter out, User user) throws InterruptedException, IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(LIST);
        stringBuilder.append(" ");

        // get available access levels
        ArrayList<String> accessLevels = user.getAccessLevel().getAvailableAccessLevels();
        // retrieve available files
        ArrayList<FileRecord> files = new ArrayList<>();
        for(String al : accessLevels) {
            files.addAll(ConfigsManager.getInstance().getSDatabase().getFilesByAccessLevel(al));
        }

        for (FileRecord fr : files) {
            // filename:hash ...
            stringBuilder.append(fr.getFileName());
            stringBuilder.append(":");
            stringBuilder.append(fr.getHash());
            stringBuilder.append(" ");
        }

        Log.log("Sending list of files");
        out.println(stringBuilder);

        // receive hash of selected file
        String clientMessg = null;

        try {
            clientMessg = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] components = clientMessg.trim().split(" ");
        if (components.length != 2 || components[0].equals(FILE)) {
            Log.log("Invalid command");
            String reply = FAILURE;
            out.println(reply);
            Thread.sleep(1000);
            in.close();
            out.close();
            return;
        }

        String fileId = components[1].trim();

        for (FileRecord fr : files) {
            if (fr.getHash().equals(fileId)) {
                Log.log("Sending file " + fr.getFileName());
                FileRestore.getInstance().restoreOthersFile(fr);
                FileInputStream fins = new FileInputStream(fr.getFileName());
                int character;
                while((character = fins.read()) != -1) {
                    out.write(character);
                }
                out.println();
                Thread.sleep(1000);
                in.close();
                out.close();
                return;
            }
        }
        return;
    }

    public void terminate() {
        if (isServer) {
            return;
        }
        try {
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
