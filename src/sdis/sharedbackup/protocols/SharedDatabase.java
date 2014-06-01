package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.User;
import sdis.sharedbackup.utils.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class SharedDatabase implements Serializable {

    public static final String FILE = ".shareddatabase.ser";
    private ArrayList<User> users;
    private Date date;
    private long lastModification;
    private ArrayList<AccessLevel> accessLevels;
    private HashMap<AccessLevel, ArrayList<FileRecord>> files;


    public SharedDatabase() {
        date = new Date();
        users = new ArrayList<>();
        accessLevels = new ArrayList<>();
        files = new HashMap<>();

        // add default access levels
        AccessLevel l1 = new AccessLevel("Administration", "dificultpass");
        AccessLevel l2 = new AccessLevel("Projects", "mediumpass");
        AccessLevel l3 = new AccessLevel("GeneralPurpose", "easypass");
        l1.addChild(l2);
        l2.addChild(l3);

        accessLevels.add(l1);
        accessLevels.add(l2);
        accessLevels.add(l3);

        files.put(l1, new ArrayList<FileRecord>());
        files.put(l2, new ArrayList<FileRecord>());
        files.put(l3, new ArrayList<FileRecord>());


        // indicates default database
        lastModification = 0;
    }

    private void updateTimestamp() {
        try {
            lastModification = SharedClock.getInstance().getTime();
        } catch (SharedClock.NotSyncedException e) {
            lastModification = date.getTime();
        }
    }

    public boolean addUser(User user) {
        for (int i = 0; i < users.size(); i++)
            if (users.get(i).getUserName().equals(user.getUserName()))
                return false;

        users.add(user);
        Log.log("User " + user.getUserName() + " added");

        updateTimestamp();
        saveDatabase();

        return true;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public long getLastModification() {
        return lastModification;
    }

    public void saveDatabase() {
        try {
            FileOutputStream fileOut = new FileOutputStream(FILE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();

            Log.log("Shared Database Saved");
        } catch (IOException i) {
            Log.log("Could not save shared database");
            i.printStackTrace();
        }
    }

    public AccessLevel getAccessLevelByPassword(String accessPassword) {
        for (int i = 0; i < accessLevels.size(); i++) {
            AccessLevel al = accessLevels.get(i);
            if (al.login(accessPassword)) {
                return al;
            }
        }

        return null;
    }

    public AccessLevel getAccessLevelById(String id) {
        for (int i = 0; i < accessLevels.size(); i++) {
            AccessLevel al = accessLevels.get(i);
            if (al.getId().equals(id)) {
                return al;
            }
        }

        return null;
    }

    public User login(String userName, String password) {
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.login(userName, password))
                return u;
        }

        return null;
    }

    public void createNameSpace(String path) {
        accessLevels.get(0).createFolders(path);
        Log.log("Created namespace folders");
    }

    public void merge(SharedDatabase masterDB) {
        long masterTimestamp = masterDB.getLastModification();
        ArrayList<AccessLevel> masterLevels = masterDB.getAccessLevels();
        ArrayList<User> masterUsers = masterDB.getUsers();
        HashMap<AccessLevel, ArrayList<FileRecord>> masterFiles = masterDB.getFiles();

        if (lastModification == 0) {
            // this is the default database
            lastModification = masterTimestamp;
            accessLevels = masterLevels;
            users = masterUsers;
            files = masterFiles;
            return;
        }
        // merge access levels (update password according to timestamp)
        for (AccessLevel masterLV : masterLevels) {
            boolean found = false;
            for (AccessLevel mineLV : accessLevels) {
                if (masterLV.equals(mineLV)) {
                    found = true;
                    if (masterLV.getHashedPassword().equals(mineLV.getHashedPassword())
                            && masterTimestamp > lastModification) {
                        mineLV.updateHashedPassword(masterLV.getHashedPassword());
                    }
                    break;
                }
            }
            if (!found) {
                accessLevels.add(masterLV);
            }
        }
        // merge users
        for (User masterU : masterUsers) {
            boolean found = false;
            for (User mineU : users) {
                if (masterU.equals(mineU)) {
                    found = true;
                    if (masterU.getHashedPassword().equals(mineU.getHashedPassword())
                            && masterTimestamp > lastModification) {
                        mineU.setHashedPassword(masterU.getHashedPassword());
                    }
                    break;
                }
            }
            if (!found) {
                users.add(masterU);
            }
        }

        // merge files
        for (AccessLevel masterAl : masterFiles.keySet()) {
            boolean found = false;
            for (AccessLevel mineAl : files.keySet()) {
                if (masterAl.equals(mineAl)) {
                    found = true;

                    // if equals, verify individual files
                    ArrayList<FileRecord> masterRecords = masterFiles.get(masterAl);
                    ArrayList<FileRecord> mineRecords = files.get(mineAl);
                    for (FileRecord masterFr : masterRecords) {
                        boolean fileFound = false;
                        for (FileRecord mineFr : mineRecords) {
                            if (masterFr.equals(mineFr)) {
                                fileFound = true;
                                break;
                            }
                        }
                        if (!fileFound) {
                            files.get(mineAl).add(masterFr);
                        }
                    }


                    break;
                }
            }
            if (!found) {
                files.put(masterAl, masterFiles.get(masterAl));
            }
        }
    }

    public boolean addFile(FileRecord record) {
        ArrayList<FileRecord> list = files.get(record.getAccessLevel());
        for (FileRecord fr : list) {
            if (fr.getHash().equals(record.getHash())) {
                return false;
            }
        }
        // file not found
        list.add(record);
        return true;
    }

    public void removeFile(FileRecord record) {
        ArrayList<FileRecord> list = files.get(record.getAccessLevel());
        for (FileRecord fr : list) {
            if (fr.getHash().equals(record.getHash())) {
                list.remove(fr);
            }
        }
    }

    public ArrayList<FileRecord> getFilesByAccessLevel(String id) {
        AccessLevel accessLevel = getAccessLevelById(id);
        return files.get(accessLevel);
    }

    private HashMap<AccessLevel, ArrayList<FileRecord>> getFiles() {
        return files;
    }

    private ArrayList<AccessLevel> getAccessLevels() {
        return accessLevels;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
