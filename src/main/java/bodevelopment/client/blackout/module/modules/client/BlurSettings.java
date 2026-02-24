package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class BlurSettings extends SettingsModule {
    private static BlurSettings INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> hudBlur = this.sgGeneral.intSetting("HUD Blur", 5, 1, 20, 1,
            "The intensity of the blur effect applied behind HUD elements. Higher values look smoother but can impact performance.");
    public final Setting<Integer> threeDBlur = this.sgGeneral.intSetting("3D Blur", 5, 1, 20, 1,
            "The strength of the blur shader for in-game 3D menus and background overlays. Great for visual depth.");
    public BlurSettings() {
        super("Blur", true, false);
        INSTANCE = this;
    }

    public static BlurSettings getInstance() {
        return INSTANCE;
    }

    public int getHUDBlurStrength() {
        return this.hudBlur.get();
    }

    public int get3DBlurStrength() {
        return this.threeDBlur.get();
    }
}
