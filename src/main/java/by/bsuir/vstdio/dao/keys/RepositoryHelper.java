package by.bsuir.vstdio.dao.keys;

import by.bsuir.vstdio.dao.annotations.Column;
import by.bsuir.vstdio.dao.annotations.Id;
import by.bsuir.vstdio.dao.annotations.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class RepositoryHelper {
    public static String getIdColumnName(Class<?> entity) {
        String idColumnName = null;
        for (Field field : entity.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                idColumnName = field.getName();
            }
        }
        return idColumnName;
    }
    public static Map<String, Method> getColumns(Class<?> entity, BiFunction<Class<?>, Field, Method> methodGetter) {
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
    public static String[] getColumnNames(Map<String, Method> columns) {
        List<String> columnNames = new ArrayList<>(columns.size());
        columns.forEach((key, value) -> columnNames.add(key));
        return columnNames.toArray(new String[0]);
    }
}
