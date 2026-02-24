package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.gui.menu.types.PanoramaMainMenu;
import bodevelopment.client.blackout.randomstuff.mainmenu.MainMenuRenderer;
import bodevelopment.client.blackout.gui.menu.types.SmokeMainMenu;
import bodevelopment.client.blackout.gui.menu.types.ColorMainMenu;

public class MainMenuSettings extends SettingsModule {
    private static MainMenuSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> blur = this.sgGeneral.i("Blur", 5, 0, 20, 1,
            "The intensity of the blur effect applied to the background. Set to 0 to keep the background sharp.");
    private final Setting<MenuMode> mode = this.sgGeneral.e("Mode", MenuMode.Smoke,
            "Selects the visual engine used to render the main menu background.");
    public final Setting<BlackOutColor> shitfuckingmenucolor = this.sgGeneral.c("Background Color", new BlackOutColor(125, 125, 125, 255),
            "The solid background color used when the menu mode is set to 'Color'.",
            () -> this.mode.get() == MenuMode.Color);
    public final Setting<BlackOutColor> color = this.sgGeneral.c("Color", new BlackOutColor(10, 10, 10, 255),
            "The primary accent color for the animated smoke particles.",
            () -> this.mode.get() == MenuMode.Smoke);
    public final Setting<BlackOutColor> color2 = this.sgGeneral.c("Color 2", new BlackOutColor(125, 125, 125, 255),
            "The secondary color used for gradient transitions in the smoke animation.",
            () -> this.mode.get() == MenuMode.Smoke);
    public final Setting<Double> speed = this.sgGeneral.d("Speed", 1.0, 0.0, 10.0, 0.1,
            "Controls the movement and evolution speed of the smoke effect. Higher values create a more chaotic visual.",
            () -> this.mode.get() == MenuMode.Smoke);

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
        Panorama(new PanoramaMainMenu());

        private final MainMenuRenderer renderer;

        MenuMode(MainMenuRenderer renderer) {
            this.renderer = renderer;
        }
    }
}
