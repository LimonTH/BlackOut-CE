package bodevelopment.client.blackout.annotations;

import java.lang.annotation.*;

/**
 * Marks a type, method, or field as internal API.
 * <p>
 * Internal APIs are implementation details of BlackOut Client that may change
 * at any time without warning. <b>Addons must not depend on internal APIs</b> —
 * doing so will likely cause breakage on client updates.
 * <p>
 * Use the public API exposed through {@code BlackoutAddon}, {@code Managers},
 * and the stable module/setting/hud abstractions instead.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@PublicAPI
public @interface Internal {
}
