package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.util.math.Box;

public class CollisionShrink extends Module {
    private static CollisionShrink INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Integer> shrinkAmount = this.sgGeneral.intSetting("Shrink Amount", 1, 1, 10, 1, ".");

    public CollisionShrink() {
        super("Collision Shrink", "Shrinks your bounding box to phase inside walls.", SubCategory.MOVEMENT, false);
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
