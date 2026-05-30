package bodevelopment.client.blackout.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type, method, or field as experimental API.
 * <p>
 * Experimental APIs may be changed or removed in any minor or patch release
 * without prior notice. Addon authors should avoid depending on experimental
 * features in production addons, or be prepared to update their code
 * when upgrading the client.
 * <p>
 * When this annotation is present on a type, all members of that type
 * are considered experimental.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface Experimental {
    String value() default "";
}
