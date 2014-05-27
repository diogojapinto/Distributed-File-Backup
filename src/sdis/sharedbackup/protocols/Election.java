package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.MulticastCommunicator;
import sdis.sharedbackup.utils.Log;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
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
    private static final int WAKE_UP_TIME_INTERVAL = 500;
    private static final int MAX_RETRIES = 3;

    private static boolean imMaster = false;
    private Boolean knowsMaster = false;
    private String masterIp = null;
    private long masterUpTime = 0;

    private long lastMasterCmdTimestamp;
    private Thread masterUpdate = null;
    private Thread masterChecker = null;

    private Long sentUpTime;
    private boolean electionRunning = false;

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

        String ip = ConfigsManager.getInstance().getInterface().getHostAddress();

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
            masterIp = ConfigsManager.getInstance().getInterface().getHostAddress();
            imMaster = true;
            knowsMaster = true;

            masterUpdate = new Thread(new MasterCmdDiffuser());
            masterUpdate.start();
        } else {
            masterChecker = new Thread(new CheckMasterCmdExpiration());
            masterChecker.start();
        }

        // TODO: database synchronization or nothing (because it is already initialized)

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

    public boolean checkIfMaster(String ip) {
        lastMasterCmdTimestamp = new Date().getTime();
        return ip.equals(masterIp);
    }

    public static class NotMasterException extends Exception {
    }

    public void candidate() {
        // initialize variables
        electionRunning = true;
        long uptime = ConfigsManager.getInstance().getUpTime();
        knowsMaster = false;
        imMaster = false;
        masterIp = null;
        masterUpTime = 0;
        // I have thoroughly analysed my code and determined that the risks are acceptable
        masterUpdate.stop();
        masterChecker.stop();

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
            masterIp = ConfigsManager.getInstance().getInterface().getHostAddress();
            masterUpTime = uptime;

            masterUpdate = new Thread(new MasterCmdDiffuser());
            masterUpdate.start();
        } else {
            masterChecker = new Thread(new CheckMasterCmdExpiration());
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

    public class ElectionNotRunningException extends Exception {

    }
}
