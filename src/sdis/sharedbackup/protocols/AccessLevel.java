package sdis.sharedbackup.protocols;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.utils.Encoder;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by knoweat on 29/05/14.
 */
public class AccessLevel implements Serializable {
    private String id;
    private String password;
    private ArrayList<AccessLevel> children;
    private AccessLevel parent;

    public AccessLevel(String id, String password) {
        this.id = id;
        this.password = Encoder.byteArrayToHexString(password.getBytes());
        children = new ArrayList<>();
        parent = null;
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
        accessLevel.setParent(this);
    }

    public String getId() {
        return id;
    }

    public ArrayList<AccessLevel> getChildren() {
        return children;
    }

    private void setParent(AccessLevel accessLevel) {
        parent = accessLevel;
    }

    public AccessLevel getParent() {
        return parent;
    }

    public String getRelativePath() {
        StringBuilder builder = new StringBuilder();
        Stack<String> stack = new Stack<>();
        AccessLevel al = this;
        do {
            stack.push(al.getId() + File.separator);
        } while ((al = al.getParent()) != null);

        while (!stack.empty()) {
            builder.append(stack.pop());
        }
        return builder.toString();
    }

    public void createFolders(String path) {
        String newPath = path + getId() + File.separator;
        File folder = new File(newPath);
        if (!folder.exists()) {
            folder.mkdir();
        }
        for (AccessLevel al : children) {
            al.createFolders(newPath);
        }
    }

    public ArrayList<String> getAvailableAccessLevels() {
        ArrayList<String> retList = new ArrayList<>();
        retList.add(this.getId());
        for (AccessLevel al : children) {
            retList.addAll(getAvailableAccessLevels());
        }
        return retList;
    }
}
