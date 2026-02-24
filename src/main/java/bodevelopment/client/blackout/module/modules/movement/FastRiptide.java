package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class FastRiptide extends Module {
    private static FastRiptide INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Double> cooldown = this.sgGeneral.doubleSetting("Cooldown", 0.0, 0.0, 1.0, 0.01, ".");
    public long prevRiptide = 0L;

    public FastRiptide() {
        super("Fast Riptide", ".", SubCategory.MOVEMENT, false);
        INSTANCE = this;
    }

    public static FastRiptide getInstance() {
        return INSTANCE;
    }
}
