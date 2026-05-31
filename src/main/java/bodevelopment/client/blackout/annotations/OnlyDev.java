package bodevelopment.client.blackout.annotations;

import bodevelopment.client.blackout.annotations.PublicAPI;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a module or addon component that should only be loaded in
 * {@link bodevelopment.client.blackout.BlackOut.Type#Dev} builds.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li><b>Dev builds:</b> annotated classes are loaded normally.</li>
 *   <li><b>Beta / Release builds:</b> annotated classes are silently skipped
 *       during module scanning (both built-in and addon).</li>
 * </ul>
 *
 * <h3>Scope</h3>
 * <p>Works on any class discovered by package scanning:
 * <ul>
 *   <li>Built-in modules — checked in {@code ModuleManager.addModuleObjects()}</li>
 *   <li>Addon modules — checked in {@code AddonLoader.scan()}</li>
 *   <li>Addon commands / HUD elements / GUI screens — checked in {@code AddonLoader.scan()}</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @OnlyDev
 * public class DebugModule extends Module {
 *     public DebugModule() {
 *         super("Debug", "Dev-only debug module", SubCategory.MISC, true);
 *     }
 * }
 * }</pre>
 *
 * <p>Addon authors can use this to ship experimental or debug modules
 * that automatically hide themselves when the client is not a dev build.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PublicAPI
public @interface OnlyDev {
}
