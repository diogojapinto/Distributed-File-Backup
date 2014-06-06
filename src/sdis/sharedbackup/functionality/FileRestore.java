package sdis.sharedbackup.functionality;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import sdis.sharedbackup.backend.FileChunk;
import sdis.sharedbackup.backend.FileChunkWithData;
import sdis.sharedbackup.backend.SharedFile;
import sdis.sharedbackup.protocols.ChunkRestore;
import sdis.sharedbackup.protocols.FileRecord;

public class FileRestore {
    private static FileRestore mInstance = null;

    private FileRestore() {
    }

    public static FileRestore getInstance() {
        return (mInstance == null) ? mInstance = new FileRestore() : mInstance;
    }

    public boolean restoreFile(SharedFile file) {
        ArrayList<FileChunkWithData> receivedChunks = new ArrayList<FileChunkWithData>();

        for (FileChunk chunk : file.getChunkList()) {
            receivedChunks.add(ChunkRestore.getInstance().requestChunk(
                    chunk.getFileId(), chunk.getChunkNo()));
        }

        rebuildFile(file, receivedChunks);

        return true;
    }

    public boolean restoreOthersFile(FileRecord record) {
        ArrayList<FileChunkWithData> receivedChunks = new ArrayList<FileChunkWithData>();

        for (int i = 0; i < record.getChunksCount(); i++) {
            FileChunkWithData chunk = ChunkRestore.getInstance().requestChunk(record.getHash(), i);
            if (chunk == null) {
                return false;
            }
            receivedChunks.add(chunk);
        }

        rebuildOthersFile(record, receivedChunks);

        return true;
    }

    private boolean rebuildFile(SharedFile file,
                                ArrayList<FileChunkWithData> chunks) {

        for (FileChunkWithData chunk : chunks) {
            if (!appendToFile(chunk.getData(), file.getFilePath())) {
                return false;
            }
        }

        return true;
    }

    private boolean rebuildOthersFile(FileRecord record,
                                      ArrayList<FileChunkWithData> chunks) {

        String folderPath = record.getAccessLevel().getId();
        String filePath = record.getAccessLevel().getId() + File.separator + record.getFileName();

        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdir();
        }

        File file = new File(filePath);
        if (file.exists()) {
            return false;
        }
        try {
            file.createNewFile();
            for (FileChunkWithData chunk : chunks) {
                if (!appendToFile(chunk.getData(), filePath)) {
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean appendToFile(byte[] data, String filePath) {

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(new File(filePath), true);

            os.write(data);
            os.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}
