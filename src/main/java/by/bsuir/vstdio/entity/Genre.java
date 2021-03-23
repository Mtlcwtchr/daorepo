package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.Table;

@Table("genre")
public class Genre {

    @Id
    @Column("id")
    private int ID;
    @Column("title")
    private String title;

}
