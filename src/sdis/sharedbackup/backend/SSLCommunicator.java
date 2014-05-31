package sdis.sharedbackup.backend;

import com.sun.deploy.util.SessionState;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;

public class SSLCommunicator {

    private KeyStore ks;
    SSLSocket clientSocket;
    SSLServerSocket serverSocket;

    SSLCommunicator(String hostName, int portNumber)  {

        //init keystore
        try {
            ks = KeyStore.getInstance("JKS");
            ks.load(SessionState.Client.class.getClassLoader().getResourceAsStream("resources/peer.keys"), "123456".toCharArray());
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

        //init UDP related variables
        try {
            clientSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(hostName, portNumber);
            serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(portNumber);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Client sends a message, function returns the server response
    public String clientSend(String message) {
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

                        String clientMessg = null;

                        try {
                            clientMessg = in.readLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //respond
                        try {
                            out = new PrintWriter(respSocket.getOutputStream(), true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        String reply = null;

                        //TODO mensagem de resposta de acordo com a mensagem recebida

                        out.println(reply);
                    }
                });

                processRequest.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void terminate() {
         try {
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
