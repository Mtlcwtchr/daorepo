package by.bsuir.vstdio.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToMany {
    String referenceTable() default "";
    String referenceKey() default "";
    String referenceTableKey() default "";
    Class<?> referenceEntity() default Object.class;
}
