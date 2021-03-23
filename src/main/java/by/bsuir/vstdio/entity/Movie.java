package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.ManyToMany;
import by.bsuir.vstdio.dao.annotations.Table;

import java.util.Date;
import java.util.List;

@Table("movie")
public class Movie {

    @Id
    @Column("id")
    private int ID;
    @Column("title")
    private String title;
    @Column("release_date")
    private Date releaseDate;
    @Column("age_rating")
    private int ageRating;
    @Column("has_oscar")
    private boolean hasOscar;
    @Column("rating")
    private int rating;
    @Column("copy_cost")
    private int copyCost;

    @ManyToMany(
            referenceTable = "genre",
            referenceTableKey = "id",
            intermediateTable = "genre_of_movie",
            intermediateReferenceKey = "genre_id",
            intermediateSelfKey = "movie_id",
            selfReferenceKey = "id",
            referenceEntity = Genre.class
    )
    private List<Genre> genres;

}
