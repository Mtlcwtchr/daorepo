package by.bsuir.vstdio.dao;

import by.bsuir.vstdio.dao.annotations.*;
import by.bsuir.vstdio.dao.exceptions.DAOException;
import by.bsuir.vstdio.dao.exceptions.EntityInstantiationException;
import by.bsuir.vstdio.dao.exceptions.UnsupportedTypeException;
import by.bsuir.vstdio.dao.keys.LimiterType;
import by.bsuir.vstdio.dao.keys.RepositoryHelper;
import by.bsuir.vstdio.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;

public class DAORepository<T extends Entity> implements CRUDRepository<T> {

    private final Class<T> mainEntity;
    private final EntityInjector<Entity> entityInjector;

    public DAORepository(Class<T> mainEntity) {
        if(!mainEntity.isAnnotationPresent(Table.class)) {
            throw new UnsupportedTypeException("Table annotation expected but not found");
        }

        this.mainEntity = mainEntity;
        this.entityInjector = new EntityInjector<>();
    }

    @Override
    public Optional<T> findById(int id) {
        try {
            return findEntityById(id, mainEntity);
        } catch (DAOException ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public List<T> findAll() {
        try {
            return findEntities(mainEntity);
        } catch (DAOException ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    @Override
    public Optional<T> save(T t) {
        return (Optional<T>) entityInjector.injectEntity(t, t.getClass());
    }

    @Override
    public Optional<T> update(T t) {
        return (Optional<T>) entityInjector.injectEntity(t, t.getClass());
    }

    @Override
    public boolean delete(int id) {
        Table entityAnnotation = mainEntity.getAnnotation(Table.class);
        String tableName = entityAnnotation.value();
        String idColumnName = RepositoryHelper.getIdColumnName(mainEntity);

        String query =
                QueryBuilder
                .delete(tableName)
                .where(idColumnName, LimiterType.EQUALS)
                .getQuery();

        try(Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try(PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setObject(1, id);
                return !preparedStatement.execute();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private List<T> findEntities(Class<?> entity) throws DAOException {
        String tableName = entity.getAnnotation(Table.class).value();
        Map<String, Method> columns = RepositoryHelper.getColumns(entity, BeanHelper::getFieldSetter);

        String query =
                QueryBuilder
                .select(tableName, RepositoryHelper.getColumnNames(columns))
                .getQuery();

        try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    List<T> list = new ArrayList<>();
                    while(resultSet.next()) {
                        Optional<?> optionalInstance = getInstance(entity, columns, resultSet);
                        if (optionalInstance.isPresent()) {
                            T instance = ((Optional<T>) optionalInstance).get();
                            fillEntityWithDependencies(instance, null, entity);
                            list.add(instance);
                        }
                    }
                    return list;
                }
            }
        } catch (SQLException | EntityInstantiationException ex) {
            ex.printStackTrace();
            throw new DAOException(ex);
        }
    }

    private Optional<T> findEntityById(int id, Class<T> entity) throws DAOException {
        String tableName = entity.getAnnotation(Table.class).value();
        String idColumnName = RepositoryHelper.getIdColumnName(entity);
        Map<String, Method> columns = RepositoryHelper.getColumns(entity, BeanHelper::getFieldSetter);

        String query =
                QueryBuilder
                .select(tableName, RepositoryHelper.getColumnNames(columns))
                .where(idColumnName, LimiterType.EQUALS)
                .getQuery();

        try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, id);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if(resultSet.next()) {
                        Optional<?> optionalInstance = getInstance(entity, columns, resultSet);
                        if(optionalInstance.isPresent()) {
                            T instance = ((Optional<T>) optionalInstance).get();
                            fillEntityWithDependencies(instance, null, entity);
                            return Optional.of(instance);
                        }
                    }
                    return Optional.empty();
                }
            }
        } catch (SQLException | EntityInstantiationException ex) {
            ex.printStackTrace();
            throw new DAOException(ex);
        }
    }

    private void fillEntityWithDependencies(Entity entityInstance, Entity previousInstance, Class<?> entity) throws DAOException {
        String tableName = entity.getAnnotation(Table.class).value();
        String idColumnName = RepositoryHelper.getIdColumnName(entity);

        Map<String, Method> columns = null;
        Class<?> referenceEntity = null;
        String query = null;

        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany fieldAnnotation = field.getAnnotation(ManyToMany.class);

                referenceEntity = fieldAnnotation.referenceEntity();
                String intermediateTable = fieldAnnotation.intermediateTable();
                String referenceTable = fieldAnnotation.referenceTable();

                columns = RepositoryHelper.getColumns(referenceEntity, BeanHelper::getFieldSetter);

                query = QueryBuilder
                        .select(fieldAnnotation.intermediateTable(),
                                RepositoryHelper.getColumnNames(columns))
                        .join(referenceTable,
                                String.format("%s.%s", intermediateTable, fieldAnnotation.intermediateReferenceKey()),
                                String.format("%s.%s", referenceTable, fieldAnnotation.referenceTableKey()))
                        .join(tableName,
                                String.format("%s.%s", intermediateTable, fieldAnnotation.intermediateSelfKey()),
                                String.format("%s.%s", tableName, fieldAnnotation.selfReferenceKey()))
                        .where(String.format("%s.%s", tableName, idColumnName), LimiterType.EQUALS)
                        .getQuery();
            } else if(field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToOne.class)) {
                OneToMany oneToManyAnnotation = field.getAnnotation(OneToMany.class);
                ManyToOne manyToOneAnnotation = field.getAnnotation(ManyToOne.class);

                referenceEntity = oneToManyAnnotation != null ? oneToManyAnnotation.referenceEntity() : manyToOneAnnotation.referenceEntity();
                String referenceTable = oneToManyAnnotation != null ? oneToManyAnnotation.referenceTable() : manyToOneAnnotation.referenceTable();
                String referenceTableKey = oneToManyAnnotation != null ? oneToManyAnnotation.referenceTableKey() : manyToOneAnnotation.referenceTableKey();
                String referenceKey = oneToManyAnnotation != null ? oneToManyAnnotation.referenceKey() : manyToOneAnnotation.referenceKey();

                columns = RepositoryHelper.getColumns(referenceEntity, BeanHelper::getFieldSetter);

                query = QueryBuilder
                        .select(referenceTable,
                                RepositoryHelper.getColumnNames(columns))
                        .join(tableName,
                                String.format("%s.%s", referenceTable, referenceTableKey),
                                String.format("%s.%s", tableName, referenceKey))
                        .where(String.format("%s.%s", tableName, idColumnName), LimiterType.EQUALS)
                        .getQuery();
            } else if(field.isAnnotationPresent(OneToOne.class)) {
                OneToOne fieldAnnotation = field.getAnnotation(OneToOne.class);

                referenceEntity = fieldAnnotation.referenceEntity();
                String referenceTable = fieldAnnotation.referenceTable();

                columns = RepositoryHelper.getColumns(referenceEntity, BeanHelper::getFieldSetter);

                query = QueryBuilder
                        .select(tableName,
                                RepositoryHelper.getColumnNames(columns))
                        .join(referenceTable,
                                String.format("%s.%s", referenceTable, idColumnName),
                                String.format("%s.%s", tableName, idColumnName))
                        .where(String.format("%s.%s", tableName, idColumnName), LimiterType.EQUALS)
                        .getQuery();
                referenceEntity = fieldAnnotation.referenceEntity();
            }

            if(query != null) {
                try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setObject(1, entityInstance.getID());
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {

                            Object dependencies = null;
                            if(field.getType().equals(List.class)) {
                                dependencies = new ArrayList<Entity>();
                                while (resultSet.next()) {
                                    Optional<?> optionalInstance = getInstance(referenceEntity, columns, resultSet);
                                    if (optionalInstance.isPresent()) {
                                        Entity instance = (Entity) optionalInstance.get();
                                        if(previousInstance != null &&
                                           instance.getClass().equals(previousInstance.getClass()) &&
                                           instance.getID() == previousInstance.getID()) {
                                            instance = previousInstance;
                                        } else {
                                            fillEntityWithDependencies(instance, entityInstance, instance.getClass());
                                        }
                                        ((List<Entity>)dependencies).add(instance);
                                    }
                                }
                            } else {
                                if(resultSet.next()) {
                                    Optional<?> optionalInstance = getInstance(referenceEntity, columns, resultSet);
                                    if(optionalInstance.isPresent()) {
                                        Entity instance = (Entity) optionalInstance.get();
                                        if(previousInstance != null &&
                                           instance.getClass().equals(previousInstance.getClass()) &&
                                           instance.getID() == previousInstance.getID()) {
                                            instance = previousInstance;
                                        } else {
                                            fillEntityWithDependencies(instance, entityInstance, instance.getClass());
                                        }
                                        dependencies = instance;
                                    }
                                }
                            }

                            Method fieldSetter = BeanHelper.getFieldSetter(entity, field);
                            fieldSetter.invoke(entityInstance, dependencies);

                        } catch (IllegalAccessException | InvocationTargetException ex) {
                            ex.printStackTrace();
                            throw new UnsupportedTypeException(ex);
                        }
                    }
                } catch (SQLException | EntityInstantiationException ex) {
                    ex.printStackTrace();
                    throw new DAOException(ex);
                }
            }
        }
    }

    private Optional<?> getInstance(Class<?> entity, Map<String, Method> columns, ResultSet resultSet) throws SQLException {
        try {
            Object instance = entity.getConstructor().newInstance();
            for (Map.Entry<String, Method> entry : columns.entrySet()) {
                String columnName = entry.getKey();
                Method setter = entry.getValue();
                Class<?> fieldType = setter.getParameterTypes()[0];
                if (fieldType.equals(boolean.class)) {
                    setter.invoke(instance, resultSet.getBoolean(columnName));
                } else if (fieldType.equals(byte.class)) {
                    setter.invoke(instance, resultSet.getByte(columnName));
                } else if (fieldType.equals(short.class)) {
                    setter.invoke(instance, resultSet.getShort(columnName));
                } else if(fieldType.equals(int.class)) {
                    setter.invoke(instance, resultSet.getInt(columnName));
                } else if (fieldType.equals(long.class)) {
                    setter.invoke(instance, resultSet.getLong(columnName));
                } else if (fieldType.equals(float.class)) {
                    setter.invoke(instance, resultSet.getFloat(columnName));
                } else if (fieldType.equals(double.class)) {
                    setter.invoke(instance, resultSet.getDouble(columnName));
                } else {
                    setter.invoke(instance, resultSet.getObject(columnName));
                }
            }
            return Optional.of(instance);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException ex) {
            ex.printStackTrace();
            throw new EntityInstantiationException(ex);
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new SQLException(ex);
        }
    }
}
