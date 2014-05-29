package sdis.sharedbackup.protocols;

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
        this.password = password;
        children = new ArrayList<>();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void addChild(AccessLevel accessLevel) {
        children.add(accessLevel);
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public ArrayList<AccessLevel> getChildren() {
        return children;
    }
}
