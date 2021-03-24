package by.bsuir.vstdio.dao;

import by.bsuir.vstdio.dao.exceptions.UnsupportedTypeException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BeanHelper {
    public static Method getFieldSetter(Class<?> entity, Field field) {
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
    public static Method getFieldGetter(Class<?> entity, Field field) {
        try {
            String fieldName = field.getName();
            return entity.getMethod(field.getType().equals(boolean.class) ? fieldName : String.format("get%s", StringUtils.capitalize(fieldName)));
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            throw new UnsupportedTypeException(ex);
        }
    }
}
