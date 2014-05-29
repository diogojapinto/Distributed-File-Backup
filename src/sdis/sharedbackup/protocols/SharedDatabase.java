package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.User;
import sdis.sharedbackup.utils.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class SharedDatabase implements Serializable {

    public static final String FILE = ".shareddatabase.ser";
    private ArrayList<User> users;
    private Date date;
    private long lastModification;
    private ArrayList<AccessLevel> accessLevels;


    public SharedDatabase() {
        date = new Date();
        users = new ArrayList<>();
        accessLevels = new ArrayList<>();

        // add default access levels
        AccessLevel l1 = new AccessLevel("Administration", "dificultpass");
        AccessLevel l2 = new AccessLevel("Projects", "mediumpass");
        AccessLevel l3 = new AccessLevel("GeneralPurpose", "atuamae");
        l1.addChild(l2);
        l2.addChild(l3);

        accessLevels.add(l1);
        accessLevels.add(l2);
        accessLevels.add(l3);

        updateTimestamp();
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

    public AccessLevel getAccessLevel(String accessPassword) {
        for (int i = 0; i < accessLevels.size(); i++)
            if (accessLevels.get(i).getPassword().equals(accessPassword))
                return accessLevels.get(i);

        return null;
    }

    public boolean validLogin(String userName, String password) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserName().equals(userName) && users.get(i).getPassword().equals(password))
                return true;
        }

        return false;
    }
}
