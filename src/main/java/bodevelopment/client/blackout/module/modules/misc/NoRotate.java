package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class NoRotate extends Module {
    private static NoRotate INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<NoRotateMode> mode = this.sgGeneral.enumSetting("Bypass Mode", NoRotateMode.Cancel, "Determines how server-mandated rotation packets are handled during a rubberband or teleport event.");

    public NoRotate() {
        super("No Rotate", "Prevents the server from forcefully resetting your camera rotation, helping maintain focus during movement desync.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static NoRotate getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    public enum NoRotateMode {
        Cancel,
        Set,
        Spoof
    }
}
