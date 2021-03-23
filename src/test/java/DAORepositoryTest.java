import by.bsuir.vstdio.dao.DAORepository;
import by.bsuir.vstdio.entity.*;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class DAORepositoryTest {

    @Test
    public void DAORepositoryTest_TestFindById() {
        DAORepository<User> userRepository = new DAORepository<>();
        Optional<User> user = userRepository.findById(1, User.class);
        user.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestFindByIdWithOneToOne() {
        DAORepository<Client> clientRepository = new DAORepository<>();
        Optional<Client> client = clientRepository.findById(1, Client.class);
        client.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestFindByIdWithManyToOne() {
        DAORepository<Client> clientRepository = new DAORepository<>();
        Optional<Client> client = clientRepository.findById(1, Client.class);
        client.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestFindByIdWithManyToMany() {
        DAORepository<Movie> movieRepository = new DAORepository<>();
        Optional<Movie> movie = movieRepository.findById(1, Movie.class);
        movie.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestSaveOneToOne() {
        User user = new User();
        user.setUsername("u");
        user.setPassword("p");
        user.setPromoted(false);

        Client client = new Client();
        client.setName("n");
        client.setEmail("e");
        client.setRegistrationDate(new Date());
        client.setPhone("p");
        client.setUser(user);

        DAORepository<Client> clientRepository = new DAORepository<>();
        Optional<Client> saved = clientRepository.save(client);
        saved.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestSaveOneToMany() {
        User user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setPromoted(false);

        Client client = new Client();
        client.setName("name");
        client.setEmail("email");
        client.setRegistrationDate(new Date());
        client.setPhone("phone");
        client.setUser(user);

        User user1 = new User();
        user1.setUsername("username1");
        user1.setPassword("password1");
        user1.setPromoted(false);

        Client client1 = new Client();
        client1.setName("name1");
        client1.setEmail("email1");
        client1.setRegistrationDate(new Date());
        client1.setPhone("phone1");
        client1.setUser(user1);

        PersonalizedGroup personalizedGroup = new PersonalizedGroup();
        personalizedGroup.setAgeFrom(16);
        personalizedGroup.setAgeTo(32);
        personalizedGroup.setOscarValuable(true);
        personalizedGroup.setRatingValuable(false);

        personalizedGroup.setClients(List.of(client, client1));

        DAORepository<PersonalizedGroup> groupRepository = new DAORepository<>();
        Optional<PersonalizedGroup> saved = groupRepository.save(personalizedGroup);
        saved.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestSaveOneToManyExistingDependencies() {
        User user = new User();
        user.setUsername("username");
        user.setPassword("password");
        user.setPromoted(false);

        Client client = new Client();
        client.setName("name");
        client.setEmail("email");
        client.setRegistrationDate(new Date());
        client.setPhone("phone");
        client.setUser(user);

        User user1 = new User();
        user1.setUsername("username1");
        user1.setPassword("password1");
        user1.setPromoted(false);

        Client client1 = new Client();
        client1.setName("name1");
        client1.setEmail("email1");
        client1.setRegistrationDate(new Date());
        client1.setPhone("phone1");
        client1.setUser(user1);

        PersonalizedGroup personalizedGroup = new PersonalizedGroup();
        personalizedGroup.setAgeFrom(16);
        personalizedGroup.setAgeTo(32);
        personalizedGroup.setOscarValuable(true);
        personalizedGroup.setRatingValuable(false);

        DAORepository<Client> clientRepository = new DAORepository<>();
        Optional<Client> savedClient = clientRepository.save(client);
        Optional<Client> savedClient1 = clientRepository.save(client1);

        personalizedGroup.setClients(List.of(savedClient.orElseThrow(), savedClient1.orElseThrow()));

        DAORepository<PersonalizedGroup> groupRepository = new DAORepository<>();
        Optional<PersonalizedGroup> saved = groupRepository.save(personalizedGroup);
        saved.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestInsertManyToMany() {
        DAORepository<Movie> movieRepository = new DAORepository<>();

        Genre g1 = new Genre();
        Genre g2 = new Genre();
        Genre g3 = new Genre();
        g1.setTitle("genre1");
        g2.setTitle("genre2");
        g3.setTitle("genre3");
        List<Genre> genres = List.of(g1, g2, g3);
        Movie movie = new Movie();
        movie.setTitle("MOVIE1");
        movie.setReleaseDate(new Date());
        movie.setAgeRating(0);
        movie.setCopyCost(3);
        movie.setHasOscar(false);
        movie.setRating(50);
        movie.setGenres(genres);

        Optional<Movie> saved = movieRepository.save(movie);
        saved.ifPresent(System.out::println);
    }

    @Test
    public void DAORepositoryTest_TestInsertManyToManyExistingDependencies() {
        DAORepository<Movie> movieRepository = new DAORepository<>();
        DAORepository<Genre> genreRepository = new DAORepository<>();

        List<Genre> genres = genreRepository.findAll(Genre.class);
        Movie movie = new Movie();
        movie.setTitle("MOVIE");
        movie.setReleaseDate(new Date());
        movie.setAgeRating(0);
        movie.setCopyCost(3);
        movie.setHasOscar(false);
        movie.setRating(50);
        movie.setGenres(genres);

        Optional<Movie> saved = movieRepository.save(movie);
        saved.ifPresent(System.out::println);
    }

}
