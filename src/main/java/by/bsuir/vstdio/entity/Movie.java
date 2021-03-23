package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.ManyToMany;
import by.bsuir.vstdio.dao.annotations.Table;

import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

@Table("movie")
public class Movie implements Entity {

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

    public Movie() { }

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

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public int getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(int ageRating) {
        this.ageRating = ageRating;
    }

    public boolean hasOscar() {
        return hasOscar;
    }

    public void setHasOscar(boolean hasOscar) {
        this.hasOscar = hasOscar;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public int getCopyCost() {
        return copyCost;
    }

    public void setCopyCost(int copyCost) {
        this.copyCost = copyCost;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Movie.class.getSimpleName() + "[", "]")
                .add("ID=" + ID)
                .add("title='" + title + "'")
                .add("releaseDate=" + releaseDate)
                .add("ageRating=" + ageRating + "+")
                .add("hasOscar=" + hasOscar)
                .add("rating=" + rating)
                .add("copyCost=" + copyCost)
                .add("genres=" + genres)
                .toString();
    }
}
