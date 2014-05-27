package sdis.sharedbackup.backend;

import sdis.sharedbackup.utils.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class SharedDatabase implements Serializable{

    public static final String FILE = ".shareddatabase.ser";
    private ArrayList<User> users;
    private Date date;
    private long lastModification;
    private ArrayList<String> accessLevelPasswords;


    public SharedDatabase(){
        accessLevelPasswords = new ArrayList<>();
        accessLevelPasswords.add("cenas1");
        accessLevelPasswords.add("cenas2");
        accessLevelPasswords.add("cenas3");

        date = new Date();
        lastModification = date.getTime();
        users = new ArrayList<>();
    }

    public boolean addUser(User user) {
        for (int i = 0; i < users.size(); i++)
            if (users.get(i).getUserName().equals(user.getUserName()))
                return false;

        users.add(user);
        Log.log("User " + user.getUserName() + " added");

        setLastModification();
        saveDatabase();

        return true;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    private void setLastModification() {
        lastModification = date.getTime();
    }

    public long getLastModification() { return lastModification; }

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

    public int getAccessLevel(String accessPassword) {
        for (int i = 0; i < accessLevelPasswords.size(); i++)
            if (accessLevelPasswords.get(i).equals(accessPassword))
                return i+1;

        return 0;
    }

    public boolean validLogin(String userName, String password) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserName().equals(userName) && users.get(i).getPassword().equals(password))
                return true;
        }

        return false;
    }
}
