package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.Table;

import java.util.StringJoiner;

@Table("user")
public class User implements Entity {

    @Id
    @Column("id")
    private int ID;
    @Column("username")
    private String username;
    @Column("password")
    private String password;
    @Column("is_promoted")
    private boolean isPromoted;

    public User() { }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isPromoted() {
        return isPromoted;
    }

    public void setPromoted(boolean promoted) {
        isPromoted = promoted;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", User.class.getSimpleName() + "[", "]")
                .add("ID=" + ID)
                .add("username='" + username + "'")
                .add("password='" + password + "'")
                .add("isPromoted=" + isPromoted)
                .toString();
    }
}
