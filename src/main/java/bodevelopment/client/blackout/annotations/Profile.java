package bodevelopment.client.blackout.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for performance profiling.
 * <p>
 * When profiling is enabled via {@code /profile} command, methods annotated
 * with {@code @Profile} will have their execution time measured and reported.
 * <p>
 * Usage: annotate module event handlers ({@code onTick}, {@code onRender}, etc.)
 * that you want to monitor for performance regressions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Profile {
    /** Human-readable label for this profiled operation. Defaults to method name. */
    String value() default "";
}
