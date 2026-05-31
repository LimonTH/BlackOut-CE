package bodevelopment.client.blackout.annotations;

import java.lang.annotation.*;

/**
 * Indicates that a class, method, or field is designed for thread-safe access.
 * <p>
 * This annotation documents the thread-safety contract. It does not enforce
 * thread safety at compile time or runtime. Code marked with this annotation
 * should be safe to call from any thread (main thread, network thread,
 * render thread, async workers) without external synchronization.
 * <p>
 * Addon authors can use this to quickly identify APIs that are safe to use
 * in packet handlers, async tasks, or other off-main-thread contexts.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@PublicAPI
public @interface ThreadSafe {
}
