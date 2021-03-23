package by.bsuir.vstdio.entity;

import by.bsuir.vstdio.dao.annotations.*;

import java.util.Date;
import java.util.StringJoiner;

@Table("client")
public class Client implements Entity {

    @Id
    @RequiredValue
    @Column("id")
    private int ID;
    @Column("name")
    private String name;
    @Column("email")
    private String email;
    @Column("phone")
    private String phone;
    @Column("registration_date")
    private Date registrationDate;

    @OneToOne(
            referenceTable = "user",
            referenceEntity = User.class)
    private User user;

    @ManyToOne(
            referenceTable = "personalized_group",
            referenceKey = "group_id",
            referenceTableKey = "id",
            referenceEntity = PersonalizedGroup.class)
    private PersonalizedGroup group;

    public Client() { }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PersonalizedGroup getGroup() {
        return group;
    }

    public void setGroup(PersonalizedGroup group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Client.class.getSimpleName() + "[", "]")
                .add("ID=" + ID)
                .add("name='" + name + "'")
                .add("email='" + email + "'")
                .add("phone='" + phone + "'")
                .add("registrationDate=" + registrationDate)
                .add("user=" + user)
                .add("group=" + group)
                .toString();
    }
}
