package sdis.sharedbackup.protocols;

import sdis.sharedbackup.utils.Encoder;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by knoweat on 29/05/14.
 */
public class AccessLevel implements Serializable {
    private String id;
    private String password;
    private ArrayList<AccessLevel> children;

    public AccessLevel(String id, String password) {
        this.id = id;
        this.password = Encoder.byteArrayToHexString(password.getBytes());
        children = new ArrayList<>();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPassword(String password) {
        this.password = Encoder.byteArrayToHexString(password.getBytes());
    }

    public boolean login(String password) {
        return this.password.equals(Encoder.byteArrayToHexString(password.getBytes()));
    }

    public void addChild(AccessLevel accessLevel) {
        children.add(accessLevel);
    }

    public String getId() {
        return id;
    }

    public ArrayList<AccessLevel> getChildren() {
        return children;
    }
}
