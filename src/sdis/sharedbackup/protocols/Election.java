package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.MulticastCommunicator;
import sdis.sharedbackup.utils.Log;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

// TODO: call enterMainStage() of configsmanager after initialization
public class Election {

    private static Election instance = null;

    public static final String WAKEUP_CMD = "WAKED_UP";
    public static final String MASTER_CMD = "IM_MASTER";
    private static final int WU_TIME_INTERVAL = 500;
    private static final int MAX_RETRIES = 3;

    private static boolean imMaster = false;

    private Boolean knowsMaster = false;
    private String masterIp = null;

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
                Log.log("WAITING : " + WU_TIME_INTERVAL
                        * (int) Math.pow(2, counter));
                Thread.sleep(WU_TIME_INTERVAL * (int) Math.pow(2, counter));
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
        }

        // TODO: database synchronization or creation
        // TODO: begin sending im master cmds
        // todo: receive master cmds

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

    private void setMaster(String ip) {
        synchronized (knowsMaster) {
            masterIp = ip;
        }
    }

    public static class NotMasterException extends Exception{};
}
