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

    public final Setting<Boolean> setLeaning = this.sgGeneral.booleanSetting("Override Leaning", false, "Enables custom torso tilt/leaning animations for player models.");
    private final Setting<Double> leaning = this.sgGeneral.doubleSetting("Tilt Intensity", 0.0, 0.0, 1.0, 0.01, "The maximum angle of the leaning effect.", this.setLeaning::get);
    private final Setting<Boolean> moveLeaning = this.sgGeneral.booleanSetting("Kinetic Leaning", true, "Dynamically adjusts the tilt based on the player's movement velocity and direction.", this.setLeaning::get);
    public final Setting<Boolean> forceSneak = this.sgGeneral.booleanSetting("Persistent Sneaking", true, "Forces the client-side model to render in a crouching pose regardless of server state.");
    public final Setting<Boolean> noAnimations = this.sgGeneral.booleanSetting("Disable Animations", true, "Prevents the rendering of limb movements, resulting in a static model pose.");
    public final Setting<Boolean> noSwing = this.sgGeneral.booleanSetting("Suppress Swing", true, "Disables the hand-swing animation when using items or attacking.");

    private final TimerMap<PlayerEntity, Float> leaningMap = new TimerMap<>(true);

    public PlayerModifier() {
        super("Player Modifier", "Manipulates the visual state and procedural animations of player entities for aesthetic or tactical purposes.", SubCategory.ENTITIES, false);
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
