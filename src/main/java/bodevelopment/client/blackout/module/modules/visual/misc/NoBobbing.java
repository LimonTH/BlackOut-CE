package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class NoBobbing extends Module {
    private static NoBobbing INSTANCE;

    public final SettingGroup sgGeneral = new SettingGroup("General");

    public final Setting<Boolean> noHurtCam = this.sgGeneral.booleanSetting("No Hurt Cam", true, "Removes the camera shake when taking damage.");

    public NoBobbing() {
        super("No Bobbing", "Prevents the camera from bobbing while moving.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static NoBobbing getInstance() {
        return INSTANCE;
    }
}