package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.RotationUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PlayerModifier extends Module {
    private static PlayerModifier INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> setLeaning = this.sgGeneral.booleanSetting("Set Leaning", false, ".");
    private final Setting<Double> leaning = this.sgGeneral.doubleSetting("Leaning", 0.0, 0.0, 1.0, 0.01, ".", this.setLeaning::get);
    private final Setting<Boolean> moveLeaning = this.sgGeneral.booleanSetting("Move Leaning", true, ".", this.setLeaning::get);
    public final Setting<Boolean> forceSneak = this.sgGeneral.booleanSetting("Force Sneak", true, ".");
    public final Setting<Boolean> noAnimations = this.sgGeneral.booleanSetting("No Animations", true, ".");
    public final Setting<Boolean> noSwing = this.sgGeneral.booleanSetting("No Swing", true, ".");
    private final TimerMap<PlayerEntity, Float> leaningMap = new TimerMap<>(true);

    public PlayerModifier() {
        super("Player Modifier", "Modifies players.", SubCategory.ENTITIES, false);
        INSTANCE = this;
    }

    public static PlayerModifier getInstance() {
        return INSTANCE;
    }

    public float getLeaning(PlayerEntity player) {
        float current = this.getLeaningValue(player);
        if (!this.leaningMap.containsKey(player)) {
            this.leaningMap.add(player, current, 1.0);
            return current;
        } else {
            double prev = this.leaningMap.get(player);
            float newLeaning = MathHelper.clamp(
                    (float) MathHelper.lerp(MathHelper.clamp(BlackOut.mc.getRenderTickCounter().getLastFrameDuration() / 10.0F, 0.0F, 1.0F), prev, current), 0.0F, 1.0F
            );
            this.leaningMap.add(player, newLeaning, 1.0);
            return newLeaning;
        }
    }

    private float getLeaningValue(PlayerEntity player) {
        if (!this.moveLeaning.get()) {
            return this.leaning.get().floatValue();
        } else {
            double yaw = RotationUtils.getYaw(Vec3d.ZERO, player.getVelocity(), 0.0);
            double yawAngle = Math.abs(RotationUtils.yawAngle(yaw, player.getYaw()));
            float yawRatio = (float) MathHelper.clamp(MathHelper.getLerpProgress(yawAngle, 90.0, 0.0), 0.0, 1.0);
            float velRatio = (float) MathHelper.clamp(player.getVelocity().horizontalLength() / 0.25, 0.0, 1.0);
            return this.leaning.get().floatValue() * yawRatio * velRatio;
        }
    }
}
