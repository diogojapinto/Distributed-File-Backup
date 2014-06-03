package sdis.sharedbackup.backend;

import sdis.sharedbackup.protocols.AccessLevel;
import sdis.sharedbackup.utils.Encoder;

import java.io.Serializable;

public class User implements Serializable {

    private String userName, password;
    private AccessLevel accessLevel;

    public User(String userName, String password, AccessLevel accessLevel) {
        this.userName = userName;
        this.password = Encoder.byteArrayToHexString(password.getBytes());
        this.accessLevel = accessLevel;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setPassword(String password) {
        this.password = Encoder.byteArrayToHexString(password.getBytes());
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public boolean login(String userName, String password) {
        return this.userName.equals(userName) && Encoder.byteArrayToHexString(password.getBytes()).equals(this
                .password);
    }

    public String getHashedPassword() {
        return password;
    }

    public void setHashedPassword(String password) {
        this.password = password;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && ((User) obj).getUserName().equals(userName);
    }
}
