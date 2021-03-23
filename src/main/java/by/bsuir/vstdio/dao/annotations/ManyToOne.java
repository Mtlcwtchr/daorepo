package by.bsuir.vstdio.dao.annotations;

import by.bsuir.vstdio.entity.Entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToOne {
    String referenceTable() default "";
    String referenceKey() default "";
    String referenceTableKey() default "";
    Class<? extends Entity> referenceEntity() default Entity.class;
}
