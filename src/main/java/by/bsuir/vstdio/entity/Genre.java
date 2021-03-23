package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.Table;

import java.util.StringJoiner;

@Table("genre")
public class Genre implements Entity {

    @Id
    @Column("id")
    private int ID;
    @Column("title")
    private String title;

    public Genre() { }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Genre.class.getSimpleName() + "[", "]")
                .add("ID=" + ID)
                .add("title='" + title + "'")
                .toString();
    }
}
