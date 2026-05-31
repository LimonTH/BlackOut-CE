package bodevelopment.client.blackout.annotations;

import java.lang.annotation.*;

/**
 * Marks a method or class whose hot-path execution is designed to avoid
 * heap allocations (object instantiation, boxing, collection resizing, etc.).
 * <p>
 * This is a documentation-only annotation. Code reviewers and addon authors
 * should treat {@code @NoAlloc} as a contract: the annotated code must not
 * allocate new objects on the heap during normal execution. Typical patterns
 * include object pooling, primitive collections (FastUtil), stack-allocated
 * arrays, and pre-computed constants.
 * <p>
 * Violating the zero-allocation contract in hot paths (onTick, onRender)
 * can cause GC pressure and micro-stutter, especially on low-end hardware.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@PublicAPI
public @interface NoAlloc {
}
