package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.hud.HudEditor;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.PlayerUtils;

public class Hud extends Module {

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<KeyBind> editorKey = sgGeneral.keySetting(
            "Open Editor",
            "Bind"
    );

    public Hud() {
        super("HUD", "Enables the Hud.", SubCategory.CLIENT, true);
    }

    @Event
    public void onKey(KeyEvent event) {
        if (!event.pressed || !PlayerUtils.isInGame()) return;
        if (!editorKey.get().isKey(event.key)) return;

        BlackOut.mc.execute(() -> {
            if (HudEditor.isOpen()) {
                BlackOut.mc.setScreen(null);
            } else {
                BlackOut.mc.setScreen(Managers.HUD.HUD_EDITOR);
            }
        });
    }
}