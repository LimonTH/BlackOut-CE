package bodevelopment.client.blackout.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element is a public API entry point intended
 * for use by addons and external consumers.
 * <p>
 * Methods, fields, and types annotated with {@code @PublicAPI} follow
 * semantic versioning: they will not be removed or have their signatures
 * changed in a backward-incompatible way within the same major version
 * of BlackOut Client.
 * <p>
 * This is the opposite of {@link Internal}. Addon authors should prefer
 * depending on {@code @PublicAPI} elements.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface PublicAPI {
}
