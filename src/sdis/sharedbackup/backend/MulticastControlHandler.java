package sdis.sharedbackup.backend;

import sdis.sharedbackup.protocols.*;
import sdis.sharedbackup.utils.Log;
import sdis.sharedbackup.utils.SplittedMessage;
import sdis.sharedbackup.utils.Splitter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

public class MulticastControlHandler implements Runnable {
    private SplittedMessage mMessage;
    private Random random;
    private SenderRecord mSender;
    private static final int MAX_WAIT_TIME = 400;
    private static final int BUFFER_SIZE = 128;

    public MulticastControlHandler(SplittedMessage message, SenderRecord sender) {
        mSender = sender;
        mMessage = message;
        random = new Random();
    }

    public void run() {

        String[] headers = mMessage.getHeader()
                .split(MulticastCommunicator.CRLF);

        String[] header_components = headers[0].split(" ");

        String messageType = header_components[0].trim();
        final String fileId;
        final int chunkNo;

        Log.log("MC RECEIVED A MESSAGE from " + mSender.getAddr() + ": " + mMessage.getHeader());
        switch (messageType) {
            case ChunkBackup.STORED_COMMAND:
                fileId = header_components[1].trim();
                chunkNo = Integer.parseInt(header_components[2].trim());

                try {
                    ConfigsManager.getInstance().incChunkReplication(fileId,
                            chunkNo);
                } catch (ConfigsManager.InvalidChunkException e) {

                    // not my file

                    synchronized (MulticastControlListener.getInstance().mPendingChunks) {
                        for (FileChunk chunk : MulticastControlListener
                                .getInstance().mPendingChunks) {
                            if (fileId.equals(chunk.getFileId())
                                    && chunk.getChunkNo() == chunkNo) {
                                chunk.incCurrentReplication();
                            }
                        }
                    }
                }
                break;
            case ChunkRestore.GET_COMMAND:

                fileId = header_components[1].trim();
                chunkNo = Integer.parseInt(header_components[2].trim());

                if (null != ConfigsManager.getInstance().getFileById(fileId)) {
                    return;
                }

                ChunkRecord record = new ChunkRecord(fileId, chunkNo);
                synchronized (MulticastControlListener.getInstance().interestingChunks) {
                    MulticastControlListener.getInstance().interestingChunks
                            .add(record);
                }

                FileChunk chunk = ConfigsManager.getInstance().getSavedChunk(
                        fileId, chunkNo);
                if (chunk != null) {
                    try {
                        Thread.sleep(random.nextInt(MAX_WAIT_TIME));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!record.isNotified) {
                        // if no one else
                        // sent it:

                        Thread restoreByIp = new Thread(new restoreSenderIPListener());

                        synchronized (MulticastControlListener.getInstance().mSentChunks) {
                            MulticastControlListener.getInstance().mSentChunks
                                    .add(record);

                            restoreByIp.start();

                        }

                        ChunkRestore.getInstance().sendChunk(chunk,
                                mSender.getAddr(), ChunkRestore.ENHANCEMENT_SEND_PORT);

                        try {
                            Thread.sleep(MAX_WAIT_TIME);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        synchronized (MulticastControlListener.getInstance().mSentChunks) {
                            if (!record.isNotified
                                    && MulticastControlListener.getInstance().mSentChunks
                                    .contains(record)) {
                                // if no one
                                // else sent
                                // it:
                                restoreByIp.interrupt();
                                MulticastControlListener.getInstance().mSentChunks
                                        .remove(record);
                                ChunkRestore.getInstance().sendChunk(chunk);
                            }
                        }

                        synchronized (MulticastControlListener.getInstance().interestingChunks) {
                            MulticastControlListener.getInstance().interestingChunks
                                    .remove(record);
                        }
                    }
                }// else I don't have it
                break;

            case FileDeletion.DELETE_COMMAND:
                String fileIde = header_components[1];

                if (ConfigsManager.getInstance().removeChunksOfFile(fileIde)) {
                    FileDeletion.getInstance().reply(fileIde);
                }

                break;
            case FileDeletion.RESPONSE_COMMAND:

                fileId = header_components[1].trim();

                ConfigsManager.getInstance().decDeletedFileReplication(fileId);
                break;
            case SpaceReclaiming.REMOVED_COMMAND:

                fileId = header_components[1].trim();
                chunkNo = Integer.parseInt(header_components[2].trim());

                FileChunk chunk2 = ConfigsManager.getInstance().getSavedChunk(
                        fileId, chunkNo);

                if (chunk2 != null) {
                    if (chunk2.decCurrentReplication() < chunk2
                            .getDesiredReplicationDeg()) {
                        try {
                            Thread.sleep(random.nextInt(MAX_WAIT_TIME));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        ChunkBackup.getInstance().putChunk(chunk2);
                    }

                } // else I don't have it
                break;
            case Election.WAKEUP_CMD:
                if (Election.getInstance().imMaster()) {
                    try {
                        Log.log("Sending IM_MASTER in response to WAKED_UP");
                        Election.getInstance().sendMasterCmd();
                    } catch (Election.NotMasterException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case Election.MASTER_CMD:
                String master = header_components[1];
                if (!Election.getInstance().checkIfMaster(master)) {
                    if (Election.getInstance().imMaster()) {
                        Election.getInstance().candidate();
                    } else {
                        // todo ask for new bd
                    }
                } else {
                    // everything is running silky smooth
                    Log.log("Received valid IM_MASTER command");
                }
                break;
            case Election.CANDIDATE_CMD:
                long itsUptime = Long.parseLong(header_components[1]);
                String itsIp = mSender.getAddr().getHostAddress();
                try {
                    Election.getInstance().updateMaster(itsIp, itsUptime);
                } catch (Election.ElectionNotRunningException e) {
                    new Thread() {
                        @Override
                        public void run() {
                            Election.getInstance().candidate();
                        }
                    }.start();
                    try {
                        Thread.sleep(400);
                        Election.getInstance().updateMaster(itsIp, itsUptime);
                    } catch (Election.ElectionNotRunningException e1) {
                        e1.printStackTrace();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

                break;
            default:
                Log.log("MC received non recognized command:");
        }
    }

    ;

    private static class restoreSenderIPListener implements Runnable {

        private static DatagramSocket restoreEnhSocket = null;

        @Override
        public void run() {
            if (restoreEnhSocket == null) {
                try {
                    restoreEnhSocket = new DatagramSocket(
                            ChunkRestore.ENHANCEMENT_RESPONSE_PORT);
                } catch (SocketException e) {
                    Log.log("Could not open the desired port for restore");
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);

            try {
                restoreEnhSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            SplittedMessage message = Splitter.split(packet.getData());

            String[] headers = message.getHeader()
                    .split(MulticastCommunicator.CRLF);

            String[] header_components = headers[0].split(" ");

            switch (header_components[0]) {
                case ChunkRestore.CHUNK_CONFIRMATION:
                    String fileId = header_components[1];
                    int chunkNo = Integer.parseInt(header_components[2]);

                    synchronized (MulticastControlListener.getInstance().mSentChunks) {
                        for (ChunkRecord record : MulticastControlListener
                                .getInstance().mSentChunks) {
                            if (record.fileId.equals(fileId)
                                    && record.chunkNo == chunkNo) {
                                MulticastControlListener.getInstance().mSentChunks
                                        .remove(record);
                                break;
                            }
                        }
                    }

                    break;
                default:
                    Log.log("Received non recognized command");
            }
        }
    }

}
