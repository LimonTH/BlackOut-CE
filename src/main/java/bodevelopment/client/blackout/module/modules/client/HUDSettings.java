package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.keys.Key;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class HUDSettings extends SettingsModule {
    private static HUDSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<KeyBind> editorKey = sgGeneral.keySetting(
            "Open Editor",
            "Bind",
            new KeyBind(new Key(345))
    );

    public HUDSettings() {
        super("HUD", true, true);
        INSTANCE = this;
    }

    public static HUDSettings getInstance() {
        return INSTANCE;
    }

}