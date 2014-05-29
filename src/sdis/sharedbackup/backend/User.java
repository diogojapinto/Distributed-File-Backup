package sdis.sharedbackup.backend;

import sdis.sharedbackup.protocols.AccessLevel;

import java.io.Serializable;

public class User implements Serializable{

    private String userName, password;
    private AccessLevel accessLevel;

    public User(String userName, String password, AccessLevel accessLevel) {
        this.userName = userName;
        this.password = password;
        this.accessLevel = accessLevel;
    }

    public void setUserName(String userName) { this.userName = userName; }

    public String getUserName() { return userName; }

    public void setPassword(String password) { this.password = password; }

    public String getPassword() { return password; }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public AccessLevel getAccessLevel() { return accessLevel; }
}
