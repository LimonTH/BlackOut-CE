package bodevelopment.client.blackout.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method or constructor requires a valid
 * in-game player ({@code BlackOut.mc.player != null} and
 * {@code BlackOut.mc.level != null}) to function correctly.
 * <p>
 * This annotation serves as documentation and does not perform any
 * runtime validation. Callers are responsible for null-safety checks
 * before invoking annotated methods.
 * <p>
 * Addon event handlers annotated with {@code @Event} that need
 * a valid player should either check {@code mc.player != null} at the
 * top of the handler or annotate the handler with {@code @RequiresPlayer}
 * for clarity.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface RequiresPlayer {
}
