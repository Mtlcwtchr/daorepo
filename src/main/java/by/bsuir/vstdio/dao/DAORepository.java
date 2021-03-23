package by.bsuir.vstdio.dao;

import by.bsuir.vstdio.dao.annotations.*;
import by.bsuir.vstdio.dao.exceptions.IllegalQueryAppendException;
import by.bsuir.vstdio.dao.exceptions.UnsupportedTypeException;
import by.bsuir.vstdio.dao.keys.LimiterType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DAORepository<T> implements CRUDRepository<T> {

    @Override
    public Optional<T> findById(int id, Class<T> entity) throws UnsupportedTypeException, IllegalQueryAppendException {
        findSimpleEntityById(id, entity);
        fillEntityWithDependencies(entity);

        return Optional.empty();
    }

    @Override
    public List<T> findAll(Class<T> entity) {
        return null;
    }

    @Override
    public Optional<T> save(T t) {
        return Optional.empty();
    }

    @Override
    public Optional<T> update(T t) {
        return Optional.empty();
    }

    @Override
    public boolean delete(int id, Class<T> entity) {
        return false;
    }

    private Optional<T> findSimpleEntityById(int id, Class<T> entity) throws IllegalQueryAppendException, UnsupportedTypeException {
        if(!entity.isAnnotationPresent(Table.class)) {
            throw new UnsupportedTypeException("Table annotation expected but not found");
        }

        String tableName = entity.getAnnotation(Table.class).value();
        String idColumnName = getIdColumnName(entity);
        Map<String, Method> columns = getColumns(entity);

        String query =
                QueryBuilder
                .select(tableName, getColumnNames(columns))
                .where(idColumnName, LimiterType.EQUALS)
                .getQuery();

        return pushQuery(entity, columns, query);
    }

    private Optional<T> fillEntityWithDependencies(Class<T> entity) throws IllegalQueryAppendException, UnsupportedTypeException {
        String tableName = entity.getAnnotation(Table.class).value();
        String idColumnName = getIdColumnName(entity);
        Map<String, Method> columns = getColumns(entity);

        Field[] fields = entity.getDeclaredFields();
        String query = null;
        for (Field field : fields) {
            if(field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany fieldAnnotation = field.getAnnotation(ManyToMany.class);

                String intermediateTable = fieldAnnotation.intermediateTable();
                String referenceTable = fieldAnnotation.referenceTable();

                query = QueryBuilder
                        .select(fieldAnnotation.intermediateTable(),
                                getColumnNames(columns))
                        .join(referenceTable,
                                String.format("%s.%s", intermediateTable, fieldAnnotation.intermediateReferenceKey()),
                                String.format("%s.%s", referenceTable, fieldAnnotation.referenceTableKey()))
                        .join(tableName,
                                String.format("%s.%s", intermediateTable, fieldAnnotation.intermediateSelfKey()),
                                String.format("%s.%s", tableName, fieldAnnotation.selfReferenceKey()))
                        .where(idColumnName, LimiterType.EQUALS)
                        .getQuery();
            } else if(field.isAnnotationPresent(OneToMany.class)) {
                OneToMany fieldAnnotation = field.getAnnotation(OneToMany.class);

                String referenceTable = fieldAnnotation.referenceTable();

                query = QueryBuilder
                        .select(tableName,
                                getColumnNames(columns))
                        .join(referenceTable,
                                String.format("%s.%s", referenceTable, fieldAnnotation.referenceTableKey()),
                                String.format("%s.%s", tableName, fieldAnnotation.referenceKey()))
                        .where(idColumnName, LimiterType.EQUALS)
                        .getQuery();
            } else if(field.isAnnotationPresent(OneToOne.class)) {

            }
        }

        return pushQuery(entity, columns, query);
    }

    private Optional<T> pushQuery(Class<T> entity, Map<String, Method> columns, String query) {
        try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return getInstance(entity, columns, resultSet);
                }
            }
        } catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<T> getInstance(Class<T> entity, Map<String, Method> columns, ResultSet resultSet) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SQLException {
        T instance = entity.getConstructor().newInstance();
        for (Map.Entry<String, Method> entry : columns.entrySet()) {
            String columnName = entry.getKey();
            Method setter = entry.getValue();
            setter.invoke(instance, resultSet.getObject(columnName));
        }
        return Optional.of(instance);
    }

    private String getIdColumnName(Class<T> entity) {
        String idColumnName = null;
        for (Field field : entity.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idColumnName = field.getName();
            }
        }
        return idColumnName;
    }

    private Map<String, Method> getColumns(Class<?> entity) {
        Table entityAnnotation = entity.getAnnotation(Table.class);
        String tableName = entityAnnotation.value();

        Field[] fields = entity.getDeclaredFields();
        Map<String, Method> columnNames = new HashMap<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column fieldAnnotation = field.getAnnotation(Column.class);
                String columnName = fieldAnnotation.value();
                for (Method method : entity.getDeclaredMethods()) {
                    if(method.getName().equals(String.format("get%s", field.getName()))) {
                        columnNames.put(String.format("%s.%s", tableName, columnName), method);
                    }
                }
            }
        }
        return columnNames;
    }

    private String[] getColumnNames(Map<String, Method> columns) {
        List<String> columnNames = new ArrayList<>(columns.size());
        columns.forEach((key, value) -> columnNames.add(key));
        return columnNames.toArray(new String[0]);
    }
}
