package sdis.sharedbackup.functionality;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.SharedFile;
import sdis.sharedbackup.protocols.ChunkBackup;
import sdis.sharedbackup.protocols.Election;
import sdis.sharedbackup.protocols.FileDeletion;
import sdis.sharedbackup.protocols.FileRecord;

public class FileBackup {
    private static FileBackup mInstance = null;

    private FileBackup() {
    }

    public static FileBackup getInstance() {
        return (mInstance == null) ? mInstance = new FileBackup() : mInstance;
    }

    // call putChunk for each chunk in SharedFile
    public boolean saveFile(SharedFile file) {
        ArrayList<FileChunk> list = file.getChunkList();

        for (int i = 0; i < list.size(); i++) {

            final FileChunk chunk = list.get(i);

            if (!ChunkBackup.getInstance().putChunk(chunk)) {
                FileDeletion.getInstance().deleteFile(file.getFileId());
                ConfigsManager.getInstance().removeSharedFile(file.getFileId());
            }
        }
        Path p = Paths.get(file.getFilePath());
        String filename = p.getFileName().toString();

        try {
            Election.getInstance().getMasterStub().addFile(new FileRecord(filename, file.getFileId(), file
                    .getAccessLevel(), file.getChunkList().size()));
        } catch (RemoteException e) {
            System.err.println("Could not sync new file");
        } catch (Election.NotRegularPeerException e) {
            System.err.println("Could not sync new file");
        }

        return true;
    }
}
