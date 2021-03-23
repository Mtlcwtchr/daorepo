package by.bsuir.vstdio.dao;

import by.bsuir.vstdio.dao.annotations.*;
import by.bsuir.vstdio.dao.exceptions.DAOException;
import by.bsuir.vstdio.dao.exceptions.EntityInstantiationException;
import by.bsuir.vstdio.dao.exceptions.UnsupportedTypeException;
import by.bsuir.vstdio.dao.keys.LimiterConjunctionType;
import by.bsuir.vstdio.dao.keys.LimiterType;
import by.bsuir.vstdio.entity.Entity;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class DAORepository<T extends Entity> implements CRUDRepository<T> {

    @Override
    public Optional<T> findById(int id, Class<T> entity) {
        try {
            return findEntityById(id, entity);
        } catch (DAOException ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public List<T> findAll(Class<T> entity) {
        try {
            return findEntities(entity);
        } catch (DAOException ex) {
            ex.printStackTrace();
            return List.of();
        }
    }

    @Override
    public Optional<T> save(T t) {
        return (Optional<T>) save(t, t.getClass());
    }

    private Optional<Entity> save(Entity t, Class<? extends Entity> entity) {
        Table entityAnnotation = entity.getAnnotation(Table.class);
        String tableName = entityAnnotation.value();
        String idColumnName = getIdColumnName(entity);

        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            Method fieldGetter = getFieldGetter(entity, field);
            if(field.isAnnotationPresent(OneToOne.class)) {
                OneToOne fieldAnnotation = field.getAnnotation(OneToOne.class);
                Class<? extends Entity> referenceEntity = fieldAnnotation.referenceEntity();
                try {
                    Entity fieldValue = (Entity) fieldGetter.invoke(t);
                    if(fieldValue.getID() == 0) {
                        Optional<Entity> saved = save(fieldValue, referenceEntity);
                        saved.ifPresent(instance -> t.setID(instance.getID()));
                    } else {
                        t.setID(fieldValue.getID());
                    }
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                    throw new UnsupportedTypeException(ex);
                }
            }
        }

        Map<String, Method> columns = getColumns(entity, this::getFieldGetter);
        String[] columnNames = getColumnNames(columns);

        String saveEntityQuery =
                QueryBuilder
                        .insert(tableName, columnNames)
                        .getQuery();

        try(Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try(PreparedStatement preparedStatement = connection.prepareStatement(saveEntityQuery, Statement.RETURN_GENERATED_KEYS)) {
                int parameterIndex = 0;
                for (Map.Entry<String, Method> entry : columns.entrySet()) {
                    Method getter = entry.getValue();
                    preparedStatement.setObject(++parameterIndex, getter.invoke(t));
                }
                preparedStatement.execute();
                try(ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if(resultSet.next()) {
                        t.setID(resultSet.getInt(1));
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }

        for(Field field : fields) {
            Method fieldGetter = getFieldGetter(entity, field);
            if(field.isAnnotationPresent(OneToMany.class)) {
                OneToMany fieldAnnotation = field.getAnnotation(OneToMany.class);
                Class<?> referenceEntity = fieldAnnotation.referenceEntity();
                String referenceTable = fieldAnnotation.referenceTable();
                String referenceEntityIdColumnName = getIdColumnName(referenceEntity);

                try {
                    List<Entity> dependencies = (List<Entity>) fieldGetter.invoke(t);
                    for (Entity dependency : dependencies) {
                        int dependencyId = getDependencyId(dependency);

                        String query =
                                QueryBuilder
                                        .update(referenceTable, String.format("%s.%s", referenceTable, fieldAnnotation.referenceTableKey()))
                                        .where(referenceEntityIdColumnName, LimiterType.EQUALS)
                                        .getQuery();

                        try(Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
                            try(PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                                preparedStatement.setObject(2, dependencyId);
                                preparedStatement.setObject(1, t.getID());
                                preparedStatement.execute();
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                    throw new UnsupportedTypeException(ex);
                }
            } else if(field.isAnnotationPresent(ManyToOne.class)) {
                ManyToOne fieldAnnotation = field.getAnnotation(ManyToOne.class);

                try {
                    Entity dependency = (Entity) fieldGetter.invoke(t);
                    int dependencyId = dependency != null ? getDependencyId(dependency) : 0;

                    String query =
                            QueryBuilder
                                    .update(tableName, String.format("%s.%s", tableName, fieldAnnotation.referenceTableKey()))
                                    .where(idColumnName, LimiterType.EQUALS)
                                    .getQuery();

                    try(Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
                        try(PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                            preparedStatement.setObject(2, dependencyId != 0 ? dependencyId : null);
                            preparedStatement.setObject(1, t.getID());
                            preparedStatement.execute();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                    throw new UnsupportedTypeException(ex);
                }
            } else if(field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany fieldAnnotation = field.getAnnotation(ManyToMany.class);

                Class<?> referenceEntity = fieldAnnotation.referenceEntity();
                String intermediateTable = fieldAnnotation.intermediateTable();
                String intermediateReferenceKey = fieldAnnotation.intermediateReferenceKey();
                String intermediateSelfKey = fieldAnnotation.intermediateSelfKey();

                try {
                    List<Entity> dependencies = (List<Entity>) fieldGetter.invoke(t);
                    List<Integer> actualIds = new ArrayList<>();
                    for (Entity dependency : dependencies) {
                        int dependencyId = getDependencyId(dependency);
                        actualIds.add(dependencyId);
                    }

                    String insertMissingDependenciesQuery =
                            QueryBuilder
                                    .insert(intermediateTable,
                                            String.format("%s.%s", intermediateTable, intermediateSelfKey),
                                            String.format("%s.%s", intermediateTable, intermediateReferenceKey))
                                    .getQuery();

                    try(Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
                        try(PreparedStatement preparedStatement = connection.prepareStatement(insertMissingDependenciesQuery)) {
                            for (Integer id : actualIds) {
                                preparedStatement.setObject(1, t.getID());
                                preparedStatement.setObject(2, id);
                                preparedStatement.addBatch();
                            }
                            preparedStatement.executeBatch();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                    throw new UnsupportedTypeException(ex);
                }
            }
        }
        return Optional.of(t);
    }

    @Override
    public Optional<T> update(T t) {
        return Optional.empty();
    }

    @Override
    public boolean delete(int id, Class<T> entity) {
        return false;
    }

    private List<T> findEntities(Class<?> entity) throws DAOException {
        if(!entity.isAnnotationPresent(Table.class)) {
            throw new UnsupportedTypeException("Table annotation expected but not found");
        }

        String tableName = entity.getAnnotation(Table.class).value();
        Map<String, Method> columns = getColumns(entity, this::getFieldSetter);

        String query =
                QueryBuilder
                .select(tableName, getColumnNames(columns))
                .getQuery();

        try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    List<T> list = new ArrayList<>();
                    while(resultSet.next()) {
                        Optional<?> optionalInstance = getInstance(entity, columns, resultSet);
                        if (optionalInstance.isPresent()) {
                            T instance = ((Optional<T>) optionalInstance).get();
                            fillEntityWithDependencies(instance, entity);
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
        if(!entity.isAnnotationPresent(Table.class)) {
            throw new UnsupportedTypeException("Table annotation expected but not found");
        }

        String tableName = entity.getAnnotation(Table.class).value();
        String idColumnName = getIdColumnName(entity);
        Map<String, Method> columns = getColumns(entity, this::getFieldSetter);

        String query =
                QueryBuilder
                .select(tableName, getColumnNames(columns))
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
                            fillEntityWithDependencies(instance, entity);
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

    private void fillEntityWithDependencies(Entity entityInstance, Class<?> entity) throws DAOException {
        fillEntityWithDependencies(entityInstance, null, entity);
    }

    private void fillEntityWithDependencies(Entity entityInstance, Entity previousInstance, Class<?> entity) throws DAOException {
        String tableName = entity.getAnnotation(Table.class).value();
        String idColumnName = getIdColumnName(entity);

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

                columns = getColumns(referenceEntity, this::getFieldSetter);

                query = QueryBuilder
                        .select(fieldAnnotation.intermediateTable(),
                                getColumnNames(columns))
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

                columns = getColumns(referenceEntity, this::getFieldSetter);

                query = QueryBuilder
                        .select(referenceTable,
                                getColumnNames(columns))
                        .join(tableName,
                                String.format("%s.%s", referenceTable, referenceTableKey),
                                String.format("%s.%s", tableName, referenceKey))
                        .where(String.format("%s.%s", tableName, idColumnName), LimiterType.EQUALS)
                        .getQuery();
            } else if(field.isAnnotationPresent(OneToOne.class)) {
                OneToOne fieldAnnotation = field.getAnnotation(OneToOne.class);

                referenceEntity = fieldAnnotation.referenceEntity();
                String referenceTable = fieldAnnotation.referenceTable();

                columns = getColumns(referenceEntity, this::getFieldSetter);

                query = QueryBuilder
                        .select(tableName,
                                getColumnNames(columns))
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

                            Method fieldSetter = getFieldSetter(entity, field);
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

    private String getIdColumnName(Class<?> entity) {
        String idColumnName = null;
        for (Field field : entity.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idColumnName = field.getName();
            }
        }
        return idColumnName;
    }

    private Map<String, Method> getColumns(Class<?> entity, BiFunction<Class<?>, Field, Method> methodGetter) {
        Map<String, Method> columnNames = new HashMap<>();
        String tableName = entity.getAnnotation(Table.class).value();
        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column fieldAnnotation = field.getAnnotation(Column.class);
                String columnName = fieldAnnotation.value();
                columnNames.put(String.format("%s.%s", tableName, columnName),
                        methodGetter.apply(entity, field));
            }
        }
        return columnNames;
    }

    private String[] getColumnNames(Map<String, Method> columns) {
        List<String> columnNames = new ArrayList<>(columns.size());
        columns.forEach((key, value) -> columnNames.add(key));
        return columnNames.toArray(new String[0]);
    }

    private Method getFieldSetter(Class<?> entity, Field field) {
        try {
            String fieldName = field.getName();
            if (field.getType().equals(boolean.class) && fieldName.contains("is")) {
                fieldName = fieldName.substring(fieldName.indexOf("is") + 2);
            }
            return entity.getMethod(String.format("set%s", StringUtils.capitalize(fieldName)), field.getType());
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }
    }

    private Method getFieldGetter(Class<?> entity, Field field) {
        try {
            String fieldName = field.getName();
            return entity.getMethod(field.getType().equals(boolean.class) ? fieldName : String.format("get%s", StringUtils.capitalize(fieldName)));
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }
    }

    private int getDependencyId(Entity dependency) {
        int dependencyId = dependency.getID();
        if (dependencyId == 0) {
            Optional<Entity> saved = save(dependency, dependency.getClass());
            dependencyId = saved.map(Entity::getID).orElse(dependencyId);
        }
        return dependencyId;
    }
}
