package sdis.sharedbackup.protocols;

import jdk.nashorn.internal.runtime.regexp.joni.Config;
import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.MulticastCommunicator;
import sdis.sharedbackup.utils.Log;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Random;

// TODO: call enterMainStage() of configsmanager after initialization
public class Election {

    public static final int MASTER_CMD_INTERVAL = 1000 * 60;
    public static final int TEN_SECONDS = 10000;
    private static Election instance = null;

    public static final String WAKEUP_CMD = "WAKED_UP";
    public static final String MASTER_CMD = "IM_MASTER";
    public static final String CANDIDATE_CMD = "CANDIDATE";
    public static final int REGISTRY_PORT = 6548;
    private static final int WAKE_UP_TIME_INTERVAL = 500;
    private static final int MAX_RETRIES = 3;

    private static boolean imMaster = false;
    private static Boolean knowsMaster = false;
    private static String masterIp = null;
    private long masterUpTime = 0;

    private long lastMasterCmdTimestamp;
    private Thread masterUpdate = null;
    private Thread masterChecker = null;

    private Long sentUpTime;
    private boolean electionRunning = false;

    Registry reg;

    private Election() {
    }

    public static Election getInstance() {
        if (instance == null) {
            instance = new Election();
        }
        return instance;
    }

    public boolean imMaster() {
        return imMaster;
    }

    public void sendStartup() {
        String message = "";

        String ip = null;
        try {
            ip = ConfigsManager.getInstance().getInterfaceIP();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        message += WAKEUP_CMD + " " + ip + MulticastCommunicator.CRLF
                + MulticastCommunicator.CRLF;

        InetAddress multCAddr = ConfigsManager.getInstance().getMCAddr();
        int multCPort = ConfigsManager.getInstance().getMCPort();

        MulticastCommunicator sender = new MulticastCommunicator(multCAddr,
                multCPort);

        int counter = 0;

        do {

            try {
                sender.sendMessage(message.getBytes(MulticastCommunicator.ASCII_CODE));
            } catch (MulticastCommunicator.HasToJoinException e1) {
                e1.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                Log.log("WAITING : " + WAKE_UP_TIME_INTERVAL
                        * (int) Math.pow(2, counter));
                Thread.sleep(WAKE_UP_TIME_INTERVAL * (int) Math.pow(2, counter));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            counter++;

            synchronized (knowsMaster) {
                if (knowsMaster) {
                    break;
                }
            }

        } while (counter < MAX_RETRIES);

        if (!knowsMaster) {
            try {
                masterIp = ConfigsManager.getInstance().getInterfaceIP();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            imMaster = true;
            knowsMaster = true;

            masterUpdate = new Thread(new MasterCmdDiffuser());
            masterUpdate.start();
            try {
                electedStartup();
            } catch (NotMasterException e) {
                e.printStackTrace();
            }
        } else {
            masterChecker = new Thread(new CheckMasterCmdExpiration());
            masterChecker.start();

            // update database with master's one
            SharedDatabase masterDB = null;
            try {
                masterDB = getMasterStub().getMasterDB();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotRegularPeerException e) {
                e.printStackTrace();
            }
            ConfigsManager.getInstance().getSDatabase().merge(masterDB);
        }

        try {
            ConfigsManager.getInstance().enterMainStage();
        } catch (ConfigsManager.ConfigurationsNotInitializedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void sendMasterCmd() throws NotMasterException {
        if (!imMaster) {
            throw new NotMasterException();
        }

        InetAddress multCtrlAddr = ConfigsManager.getInstance().getMCAddr();
        int multCtrlPort = ConfigsManager.getInstance().getMCPort();

        MulticastCommunicator sender = new MulticastCommunicator(multCtrlAddr,
                multCtrlPort);

        String message = null;

        message = MASTER_CMD + " "
                + masterIp
                + MulticastCommunicator.CRLF + MulticastCommunicator.CRLF;

        try {
            sender.sendMessage(message
                    .getBytes(MulticastCommunicator.ASCII_CODE));
        } catch (MulticastCommunicator.HasToJoinException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void updateMaster(String ip, long upTime) throws ElectionNotRunningException {
        if (!electionRunning) {
            throw new ElectionNotRunningException();
        }
        if (upTime > masterUpTime) {
            synchronized (knowsMaster) {
                knowsMaster = true;
                masterIp = ip;
            }
            masterUpTime = upTime;
        }
    }

    public static void setInitMaster(String ip) {
        knowsMaster = true;
        masterIp = ip;
    }

    public boolean checkIfMaster(String ip) {
        lastMasterCmdTimestamp = new Date().getTime();
        return ip.equals(masterIp);
    }

    public void candidate() {

        if (imMaster) {
            try {
                reg.unbind(MasterServices.REG_ID);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }

        // initialize variables
        electionRunning = true;
        long uptime = ConfigsManager.getInstance().getUpTime();
        knowsMaster = false;
        imMaster = false;
        masterIp = null;
        masterUpTime = 0;
        // I have thoroughly analysed my code and determined that the risks are acceptable
        if (imMaster) {
            masterUpdate.stop();
        } else {
            masterChecker.stop();
        }

        synchronized (sentUpTime) {
            sentUpTime = uptime;
        }

        InetAddress multCtrlAddr = ConfigsManager.getInstance().getMCAddr();
        int multCtrlPort = ConfigsManager.getInstance().getMCPort();

        MulticastCommunicator sender = new MulticastCommunicator(multCtrlAddr,
                multCtrlPort);

        String message = null;

        message = CANDIDATE_CMD + " "
                + uptime
                + MulticastCommunicator.CRLF + MulticastCommunicator.CRLF;

        Random r = new Random();
        int waitTime = r.nextInt() % 400;

        try {
            Thread.sleep(waitTime);
            sender.sendMessage(message
                    .getBytes(MulticastCommunicator.ASCII_CODE));
            Thread.sleep(500 - waitTime);
        } catch (MulticastCommunicator.HasToJoinException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!knowsMaster) {
            imMaster = true;
            knowsMaster = true;
            try {
                masterIp = ConfigsManager.getInstance().getInterfaceIP();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            masterUpTime = uptime;

            masterUpdate = new Thread(new MasterCmdDiffuser());
            masterUpdate.start();

            try {
                electedStartup();
            } catch (NotMasterException e) {
                e.printStackTrace();
            }
        } else {
            masterChecker = new Thread(new CheckMasterCmdExpiration());
            SharedClock.getInstance().startSync();
            masterChecker.start();
        }
    }

    private class MasterCmdDiffuser implements Runnable {

        @Override
        public void run() {
            while (ConfigsManager.getInstance().isAppRunning()) {
                try {
                    sendMasterCmd();
                    Thread.sleep(MASTER_CMD_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NotMasterException e) {
                    Log.log("Not master trying to send MASTER_CMD");
                    System.exit(1);
                }
            }
        }
    }

    public Long getSentUpTime() throws ElectionNotRunningException {
        if (!electionRunning) {
            throw new ElectionNotRunningException();
        }
        return sentUpTime;
    }

    private class CheckMasterCmdExpiration implements Runnable {

        @Override
        public void run() {
            while (ConfigsManager.getInstance().isAppRunning()) {
                try {
                    Thread.sleep(MASTER_CMD_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long now = new Date().getTime();
                if (now - lastMasterCmdTimestamp > MASTER_CMD_INTERVAL + TEN_SECONDS) {
                    candidate();
                }
            }
        }
    }

    private void electedStartup() throws NotMasterException {
        if (!imMaster) {
            throw new NotMasterException();
        }

        MasterActions obj = new MasterActions();
        try {
            System.setProperty("java.rmi.server.hostname", ConfigsManager.getInstance().getInterfaceIP());
            reg = LocateRegistry.createRegistry(REGISTRY_PORT);
            MasterServices stub = (MasterServices) UnicastRemoteObject.exportObject(obj, 0);
            reg.rebind(MasterServices.REG_ID, stub);
            Log.log("Registering stub with id " + MasterServices.REG_ID);

            Log.log("Master services ready");

        } catch (RemoteException e) {
            System.err.println("RMI registry not available. Exiting...");
            System.exit(1);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public MasterServices getMasterStub() throws NotRegularPeerException {
        if (imMaster) {
            throw new NotRegularPeerException();
        }

        try {
            reg = LocateRegistry.getRegistry(masterIp, REGISTRY_PORT);
            return (MasterServices) reg.lookup(MasterServices.REG_ID);
        } catch (RemoteException e) {
            System.err.println("Error getting stub from RMI Registry. Exiting...");
            e.printStackTrace();
            System.exit(1);
        } catch (NotBoundException e) {
            System.err.println("Error getting stub from RMI Registry. Exiting...");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public class ElectionNotRunningException extends Exception {
    }

    public static class NotMasterException extends Exception {
    }

    public static class NotRegularPeerException extends Exception {
    }
}
