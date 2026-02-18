package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.gui.menu.types.ColorMainMenu;
import bodevelopment.client.blackout.randomstuff.mainmenu.MainMenuRenderer;
import bodevelopment.client.blackout.gui.menu.types.SmokeMainMenu;
import bodevelopment.client.blackout.gui.menu.types.ThemeMainMenu;

public class MainMenuSettings extends SettingsModule {
    private static MainMenuSettings INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Integer> blur = this.sgGeneral.i("Blur", 5, 0, 20, 1, ".");
    private final Setting<MenuMode> mode = this.sgGeneral.e("Mode", MenuMode.Smoke, ".");
    public final Setting<BlackOutColor> shitfuckingmenucolor = this.sgGeneral
            .c("Background Color", new BlackOutColor(125, 125, 125, 255), ".", () -> this.mode.get() == MenuMode.Color);
    public final Setting<BlackOutColor> color = this.sgGeneral
            .c("Color", new BlackOutColor(10, 10, 10, 255), ".", () -> this.mode.get() == MenuMode.Smoke);
    public final Setting<BlackOutColor> color2 = this.sgGeneral
            .c("Color 2", new BlackOutColor(125, 125, 125, 255), ".", () -> this.mode.get() == MenuMode.Smoke);
    public final Setting<Double> speed = this.sgGeneral.d("Speed", 1.0, 0.0, 10.0, 0.1, ".", () -> this.mode.get() == MenuMode.Smoke);

    public MainMenuSettings() {
        super("Main Menu", true, false);
        INSTANCE = this;
    }

    public static MainMenuSettings getInstance() {
        return INSTANCE;
    }

    public MainMenuRenderer getRenderer() {
        return this.mode.get().renderer;
    }

    public enum MenuMode {
        Smoke(new SmokeMainMenu()),
        Color(new ColorMainMenu()),
        Theme(new ThemeMainMenu());

        private final MainMenuRenderer renderer;

        MenuMode(MainMenuRenderer renderer) {
            this.renderer = renderer;
        }
    }
}
