package by.bsuir.vstdio.dao.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToMany {
    String referenceTable() default "";
    String referenceTableKey() default "";
    String intermediateTable() default "";
    String intermediateSelfKey() default "";
    String intermediateReferenceKey() default "";
    String selfReferenceKey() default "";
    Class<?> referenceEntity() default Object.class;
}
