package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class NoRotate extends Module {
    private static NoRotate INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<NoRotateMode> mode = this.sgGeneral.enumSetting("Mode", NoRotateMode.Cancel, ".");

    public NoRotate() {
        super("No Rotate", "Doesn't set rotation on rubberband", SubCategory.MISC, false);
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
