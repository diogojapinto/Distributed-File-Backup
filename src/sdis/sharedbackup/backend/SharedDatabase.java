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
    private long lastModification; //TODO hash access levels ou la o que era
    private Date date;
    private ArrayList<User> users;

    public SharedDatabase(){
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
}
