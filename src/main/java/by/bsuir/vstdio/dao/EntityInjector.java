package by.bsuir.vstdio.dao;

import by.bsuir.vstdio.dao.annotations.*;
import by.bsuir.vstdio.dao.exceptions.UnsupportedTypeException;
import by.bsuir.vstdio.dao.keys.LimiterConjunctionType;
import by.bsuir.vstdio.dao.keys.LimiterType;
import by.bsuir.vstdio.dao.keys.RepositoryHelper;
import by.bsuir.vstdio.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EntityInjector<T extends Entity> {

    public Optional<T> injectEntity(T t, Class<? extends T> entity) {
        Table entityAnnotation = entity.getAnnotation(Table.class);
        String tableName = entityAnnotation.value();
        String idColumnName = RepositoryHelper.getIdColumnName(entity);

        Field[] fields = entity.getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(OneToOne.class)) {
                proceedOneToOneInjection(t, entity, field);
            }
        }

        proceedSingleEntityInjection(t, entity, tableName);

        for(Field field : fields) {
            Method fieldGetter = BeanHelper.getFieldGetter(entity, field);
            if(field.isAnnotationPresent(OneToMany.class)) {
                proceedOneToManyInjection(t, field, fieldGetter);
            } else if(field.isAnnotationPresent(ManyToOne.class)) {
                proceedManyToOneInjection(t, tableName, idColumnName, field, fieldGetter);
            } else if(field.isAnnotationPresent(ManyToMany.class)) {
                proceedManyToManyInjection(t, field, fieldGetter);
            }
        }
        return Optional.of(t);
    }

    private void proceedSingleEntityInjection(T t, Class<? extends T> entity, String tableName) {
        Map<String, Method> columns = RepositoryHelper.getColumns(entity, BeanHelper::getFieldGetter);
        String[] columnNames = RepositoryHelper.getColumnNames(columns);

        String saveEntityQuery = t.getID() == 0 ?
                QueryBuilder.insert(tableName, columnNames).getQuery() :
                QueryBuilder.update(tableName, columnNames).where(RepositoryHelper.getIdColumnName(entity), LimiterType.EQUALS).getQuery();

        try(Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try(PreparedStatement preparedStatement = connection.prepareStatement(saveEntityQuery, Statement.RETURN_GENERATED_KEYS)) {
                int parameterIndex = 0;
                for (Map.Entry<String, Method> entry : columns.entrySet()) {
                    Method getter = entry.getValue();
                    preparedStatement.setObject(++parameterIndex, getter.invoke(t));
                }
                if(t.getID() != 0) {
                    preparedStatement.setObject(++parameterIndex, t.getID());
                }
                preparedStatement.execute();
                try(ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if(resultSet.next()) {
                        t.setID(t.getID() == 0 ? resultSet.getInt(1) : t.getID());
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }
    }
    private void proceedOneToOneInjection(T t, Class<? extends T> entity, Field field) {
        try {
            T fieldValue = (T) BeanHelper.getFieldGetter(entity, field).invoke(t);
            if(t.getID() == 0) {
                t.setID(getDependencyId(fieldValue));
            } else {
                injectEntity(fieldValue, (Class<? extends T>) fieldValue.getClass());
            }
        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }
    }
    private void proceedOneToManyInjection(T t, Field field, Method fieldGetter) {
        OneToMany fieldAnnotation = field.getAnnotation(OneToMany.class);
        Class<?> referenceEntity = fieldAnnotation.referenceEntity();
        String referenceTable = fieldAnnotation.referenceTable();
        String referenceEntityIdColumnName = RepositoryHelper.getIdColumnName(referenceEntity);
        String referenceTableKey = fieldAnnotation.referenceTableKey();

        try {
            List<Integer> actualIds = ((List<T>) fieldGetter.invoke(t)).stream().map(this::getDependencyId).collect(Collectors.toList());
            List<Integer> storedIds = getStoredIds(t.getID(), referenceTable, referenceEntityIdColumnName, referenceTableKey);

            List<Integer> missingIds = actualIds;
            missingIds.removeAll(storedIds);
            List<Integer> extraIds = storedIds;
            extraIds.removeAll(actualIds);

            String setIdsQuery =
                    QueryBuilder
                            .update(referenceTable, String.format("%s.%s", referenceTable, fieldAnnotation.referenceTableKey()))
                            .where(referenceEntityIdColumnName, LimiterType.EQUALS)
                            .getQuery();

            try(Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
                try(PreparedStatement preparedStatement = connection.prepareStatement(setIdsQuery)) {
                    for (Integer id : missingIds) {
                        preparedStatement.setObject(2, id);
                        preparedStatement.setObject(1, t.getID());
                        preparedStatement.addBatch();
                    }
                    for (Integer id : extraIds) {
                        preparedStatement.setObject(2, id);
                        preparedStatement.setObject(1, null);
                        preparedStatement.addBatch();
                    }
                    preparedStatement.execute();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }
    }
    private void proceedManyToOneInjection(T t, String tableName, String idColumnName, Field field, Method fieldGetter) {
        ManyToOne fieldAnnotation = field.getAnnotation(ManyToOne.class);
        try {
            T dependency = (T) fieldGetter.invoke(t);

            int dependencyId = getDependencyId(dependency);

            String query =
                    QueryBuilder
                            .update(tableName, String.format("%s.%s", tableName, fieldAnnotation.referenceTableKey()))
                            .where(idColumnName, LimiterType.EQUALS)
                            .getQuery();

            try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
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
    }
    private void proceedManyToManyInjection(T t, Field field, Method fieldGetter) {
        ManyToMany fieldAnnotation = field.getAnnotation(ManyToMany.class);

        String intermediateTable = fieldAnnotation.intermediateTable();
        String intermediateReferenceKey = fieldAnnotation.intermediateReferenceKey();
        String intermediateSelfKey = fieldAnnotation.intermediateSelfKey();

        int entityId = t.getID();

        try {
            List<Integer> actualIds = ((List<T>) fieldGetter.invoke(t)).stream().map(this::getDependencyId).collect(Collectors.toList());
            List<Integer> storedIds = getStoredIds(entityId, intermediateTable, intermediateReferenceKey, intermediateSelfKey);

            List<Integer> missingIds = actualIds;
            missingIds.removeAll(storedIds);
            List<Integer> extraIds = storedIds;
            extraIds.removeAll(actualIds);

            String dropExtraDependenciesQuery =
                    QueryBuilder
                            .delete(intermediateTable)
                            .where(String.format("%s.%s", intermediateTable, intermediateSelfKey), LimiterType.EQUALS)
                            .where(String.format("%s.%s", intermediateTable, intermediateReferenceKey), LimiterType.EQUALS, LimiterConjunctionType.AND)
                            .getQuery();

            String insertMissingDependenciesQuery =
                    QueryBuilder
                            .insert(intermediateTable,
                                    String.format("%s.%s", intermediateTable, intermediateSelfKey),
                                    String.format("%s.%s", intermediateTable, intermediateReferenceKey))
                            .getQuery();

            updateIntermediateManyToManyDependencyTable(entityId, extraIds, dropExtraDependenciesQuery);
            updateIntermediateManyToManyDependencyTable(entityId, missingIds, insertMissingDependenciesQuery);

        } catch (IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }
    }

    private void updateIntermediateManyToManyDependencyTable(int entityId, List<Integer> extraIds, String dropExtraDependenciesQuery) {
        try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(dropExtraDependenciesQuery)) {
                for (Integer id : extraIds) {
                    preparedStatement.setObject(1, entityId);
                    preparedStatement.setObject(2, id);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private List<Integer> getStoredIds(int entityId, String referenceTable, String referenceEntityIdColumnName, String referenceTableKey) {
        List<Integer> storedIds = new ArrayList<>();

        if (entityId != 0) {
            String selectQuery =
                    QueryBuilder
                            .select(referenceTable, String.format("%s.%s", referenceTable, referenceEntityIdColumnName))
                            .where(String.format("%s.%s", referenceTable, referenceTableKey), LimiterType.EQUALS)
                            .getQuery();

            try (Connection connection = ConnectionsHandler.INSTANCE.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
                    preparedStatement.setObject(1, entityId);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            storedIds.add(resultSet.getInt(1));
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return storedIds;
    }
    private int getDependencyId(T dependency) {
        if(dependency == null) {
            return 0;
        }

        int dependencyId = dependency.getID();
        if (dependencyId == 0) {
            Optional<T> saved = injectEntity(dependency, (Class<? extends T>) dependency.getClass());
            dependencyId = saved.map(Entity::getID).orElse(dependencyId);
        }
        return dependencyId;
    }
}
