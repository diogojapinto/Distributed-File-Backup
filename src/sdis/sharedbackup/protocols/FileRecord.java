package sdis.sharedbackup.protocols;

import java.io.Serializable;

/**
* Created by knoweat on 31/05/14.
*/
public class FileRecord implements Serializable{
    private String fileName;
    private String hash;
    private AccessLevel accessLevel;
    private int chunksCount;

    public FileRecord(String fileName, String hash, AccessLevel accessLevel, int chunksCount) {
        this.fileName = fileName;
        this.hash = hash;
        this.accessLevel = accessLevel;
        this.chunksCount = chunksCount;
    }

    public String getFileName() {
        return fileName;
    }

    public String getHash() {
        return hash;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public int getChunksCount() {
        return chunksCount;
    }
}
