package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.Table;

@Table("user")
public class User {

    @Id
    @Column("id")
    private int ID;
    @Column("username")
    private String username;
    @Column("password")
    private String password;
    @Column("is_promoted")
    private boolean isPromoted;

}
