package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.theme.Theme;

import java.util.ArrayList;
import java.util.List;

public class ThemeSettings extends SettingsModule {
    public static final List<Theme> themes = new ArrayList<>();
    private static ThemeSettings INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Theme> theme = this.sgGeneral.enumSetting("Theme", Theme.BLACKOUT,
            "Selects the global color palette for the client. This affects buttons, sliders, and highlights across all modules.");
    public final Setting<Integer> alpha = this.sgGeneral.intSetting("Color Alpha", 175, 0, 255, 1,
            "The primary transparency level for main background elements. Lower values create a more translucent, glass-like effect.");
    public final Setting<Integer> lowAlpha = this.sgGeneral.intSetting("Low Alpha", 50, 0, 255, 1,
            "A secondary, lower transparency level used for subtle overlays, disabled states, and accent backgrounds.");

    public ThemeSettings() {
        super("Theme", true, false);
        INSTANCE = this;
    }

    public static ThemeSettings getInstance() {
        return INSTANCE;
    }

    public Theme getTheme() {
        return this.theme.get();
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public int getMain(int alpha) {
        return this.getTheme().mainWithAlpha(alpha);
    }

    public int getSecond(int alpha) {
        return this.getTheme().secondaryWithAlpha(alpha);
    }

    public int getMain() {
        return this.getTheme().getMain();
    }

    public int getSecond() {
        return this.getTheme().getSecondary();
    }

    public int alpha() {
        return this.alpha.get();
    }

    // TODO: нет использований lowAlpha
    public int lowAlpha() {
        return this.lowAlpha.get();
    }

    // TODO: нет использований getThemeAlpha
    public int getThemeAlpha() {
        return this.alpha.get();
    }
}
