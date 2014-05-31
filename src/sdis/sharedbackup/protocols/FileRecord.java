package sdis.sharedbackup.protocols;

import java.io.Serializable;

/**
* Created by knoweat on 31/05/14.
*/
public class FileRecord implements Serializable{
    private String fileName;
    private String hash;
    private AccessLevel accessLevel;

    public FileRecord(String fileName, String hash, AccessLevel accessLevel) {
        this.fileName = fileName;
        this.hash = hash;
        this.accessLevel = accessLevel;
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
}
