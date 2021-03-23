import by.bsuir.vstdio.dao.DAORepository;
import by.bsuir.vstdio.dao.exceptions.IllegalQueryAppendException;
import by.bsuir.vstdio.dao.exceptions.UnsupportedTypeException;
import by.bsuir.vstdio.entity.Movie;
import org.junit.Test;

import java.util.Optional;

public class DAORepositoryTest {

    @Test
    public void DAORepositoryTest_TestFindById() throws UnsupportedTypeException, IllegalQueryAppendException {
        DAORepository<Movie> repository = new DAORepository<>();
        Optional<Movie> user = repository.findById(1, Movie.class);
    }

}
