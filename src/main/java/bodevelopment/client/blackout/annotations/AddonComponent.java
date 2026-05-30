package bodevelopment.client.blackout.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an addon component that should be auto-discovered
 * by the addon classpath scanner. Using this annotation is an alternative
 * to placing the class in a scanned package path.
 *
 * <p>The annotation value specifies the component type, which determines
 * how the class is registered:
 * <ul>
 *   <li>{@link Kind#MODULE} — registered as a {@code Module}</li>
 *   <li>{@link Kind#COMMAND} — registered as a {@code Command}</li>
 *   <li>{@link Kind#HUD} — registered as a {@code HudElement}</li>
 *   <li>{@link Kind#THEME} — registered as a {@code Theme}</li>
 *   <li>{@link Kind#MENU_RENDERER} — registered as a {@code MainMenuRenderer}</li>
 *   <li>{@link Kind#GUI_SCREEN} — stored in {@code addon.guiScreens}</li>
 * </ul>
 *
 * <p>Example:
 * <pre>{@code
 * @AddonComponent(Kind.MODULE)
 * public class MyModule extends Module {
 *     public MyModule() {
 *         super("MyModule", "Description", SubCategory.MISC, true);
 *     }
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AddonComponent {
    Kind value();

    enum Kind {
        MODULE,
        COMMAND,
        HUD,
        THEME,
        MENU_RENDERER,
        GUI_SCREEN
    }
}
