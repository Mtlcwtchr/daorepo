package by.bsuir.vstdio.dao;

import by.bsuir.vstdio.dao.exceptions.DAOException;
import by.bsuir.vstdio.entity.Entity;

import java.util.List;
import java.util.Optional;

public interface CRUDRepository<T> {

    Optional<T> findById(int id, Class<T> entity);
    List<T> findAll(Class<T> entity);
    Optional<T> save(T t);
    Optional<T> update(T t);
    boolean delete(int id, Class<T> entity);

}
