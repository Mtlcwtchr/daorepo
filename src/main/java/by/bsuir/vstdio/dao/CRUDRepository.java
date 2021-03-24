package by.bsuir.vstdio.dao;

import java.util.List;
import java.util.Optional;

public interface CRUDRepository<T> {

    Optional<T> findById(int id);
    List<T> findAll();
    Optional<T> save(T t);
    Optional<T> update(T t);
    boolean delete(int id);

}
