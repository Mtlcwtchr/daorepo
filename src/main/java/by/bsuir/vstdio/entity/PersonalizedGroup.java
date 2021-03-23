package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.OneToMany;
import by.bsuir.vstdio.dao.annotations.Table;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

@Table("personalized_group")
public class PersonalizedGroup implements Entity{

    @Id
    @Column("id")
    private int ID;
    @Column("is_oscar_valuable")
    private boolean isOscarValuable;
    @Column("is_rating_valuable")
    private boolean isRatingValuable;
    @Column("age_from")
    private int ageFrom;
    @Column("age_to")
    private int ageTo;

    @OneToMany(
            referenceTable = "client",
            referenceKey = "id",
            referenceTableKey = "group_id",
            referenceEntity = Client.class)
    private List<Client> clients;

    public PersonalizedGroup() { }

    @Override
    public int getID() {
        return ID;
    }

    @Override
    public void setID(int ID) {
        this.ID = ID;
    }

    public boolean isOscarValuable() {
        return isOscarValuable;
    }

    public void setOscarValuable(boolean oscarValuable) {
        isOscarValuable = oscarValuable;
    }

    public boolean isRatingValuable() {
        return isRatingValuable;
    }

    public void setRatingValuable(boolean ratingValuable) {
        isRatingValuable = ratingValuable;
    }

    public int getAgeFrom() {
        return ageFrom;
    }

    public void setAgeFrom(int ageFrom) {
        this.ageFrom = ageFrom;
    }

    public int getAgeTo() {
        return ageTo;
    }

    public void setAgeTo(int ageTo) {
        this.ageTo = ageTo;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PersonalizedGroup.class.getSimpleName() + "[", "]")
                .add("ID=" + ID)
                .add("isOscarValuable=" + isOscarValuable)
                .add("isRatingValuable=" + isRatingValuable)
                .add("ageFrom=" + ageFrom)
                .add("ageTo=" + ageTo)
                .add("clients=" + Arrays.toString(clients.stream().map(Client::getID).toArray()))
                .toString();
    }
}
