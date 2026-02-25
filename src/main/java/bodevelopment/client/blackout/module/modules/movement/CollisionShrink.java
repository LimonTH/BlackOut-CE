package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.util.math.Box;

public class CollisionShrink extends Module {
    private static CollisionShrink INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Integer> shrinkAmount = this.sgGeneral.intSetting("Compression Level", 1, 1, 10, 1, "The intensity of the bounding box contraction. Higher values increase the likelihood of phasing through block edges.");

    public CollisionShrink() {
        super("Collision Shrink", "Slightly contracts your horizontal bounding box to allow for clipping through narrow gaps or block corners.", SubCategory.MOVEMENT, false);
        INSTANCE = this;
    }

    public static CollisionShrink getInstance() {
        return INSTANCE;
    }

    public Box getBox(Box normal) {
        double amount = 0.0625 * Math.pow(10.0, this.shrinkAmount.get()) / 1.0E10;
        return normal.contract(amount, 0.0, amount);
    }
}
