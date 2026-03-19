package com.taskmanager.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a service method for audit logging.
 * The annotated method must take a UUID as its first argument (the entity ID)
 * for non-CREATE actions. Set {@code entityClass} to enable old-value capture
 * — the aspect will load the entity by the first UUID arg before the method runs.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String entityType();
    String action();
    /** JPA entity class to look up for old-value capture. Defaults to Void (disabled). */
    Class<?> entityClass() default Void.class;
}
