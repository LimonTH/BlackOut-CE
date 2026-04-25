package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class AimAssist extends Module {

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> range       = sgGeneral.doubleSetting("Range",    6.0,  1.0, 10.0,  0.1,  "Maximum range to target players.");
    private final Setting<Double> strength    = sgGeneral.doubleSetting("Strength", 0.1,  0.01, 1.0,  0.01, "Aiming speed towards the target.");
    private final Setting<Double> fov         = sgGeneral.doubleSetting("FOV",      90.0, 10.0, 360.0, 1.0, "Field of view cone for target selection.");
    private final Setting<Boolean> eyeAim     = sgGeneral.boolSetting("Eye Aim",       false, "Aim at eye position instead of body center.");
    private final Setting<Boolean> dynamicStr = sgGeneral.boolSetting("Dynamic Strength", true, "Scale strength based on distance to target.");

    private final Map<Player, Double> targetProgress = new HashMap<>();
    private double noiseOffset = Math.random() * 1000;
    private Player currentTarget = null;

    public AimAssist() {
        super("AimAssist", "Smoothly assists aim towards nearby players.", SubCategory.LEGIT, true);
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        targetProgress.clear();
        noiseOffset = Math.random() * 1000;
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        targetProgress.clear();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.level == null) return;
        if (BlackOut.mc.player.isUsingItem()) return;

        Player potentialTarget = BlackOut.mc.level.players()
                .stream()
                .filter(p -> p != BlackOut.mc.player)
                .filter(p -> p.isAlive() && !p.isSpectator())
                .filter(p -> BlackOut.mc.player.distanceToSqr(p) <= range.get() * range.get())
                .filter(p -> Math.abs(Mth.wrapDegrees(getYawTo(p) - BlackOut.mc.player.getYRot())) <= fov.get() / 2.0)
                .min(Comparator.comparingDouble(p -> BlackOut.mc.player.distanceToSqr(p)))
                .orElse(null);

        if (potentialTarget == null) {
            currentTarget = null;
            targetProgress.clear();
            return;
        }

        if (currentTarget != potentialTarget) {
            currentTarget = potentialTarget;
            targetProgress.put(currentTarget, 0.0);
        }

        Vec3 targetPos;
        if (eyeAim.get()) {
            targetPos = currentTarget.getEyePosition();
        } else {
            AABB bb = currentTarget.getBoundingBox();
            targetPos = new Vec3(
                    (bb.minX + bb.maxX) * 0.5 + (Math.random() - 0.5) * 0.2,
                    (bb.minY + bb.maxY) * 0.5 + 0.3  + (Math.random() - 0.5) * 0.15,
                    (bb.minZ + bb.maxZ) * 0.5 + (Math.random() - 0.5) * 0.2
            );
        }

        double t = targetProgress.getOrDefault(currentTarget, 0.0);
        Vec3 eyePos = BlackOut.mc.player.getEyePosition();

        Vec3 midPoint = new Vec3(
                (eyePos.x + targetPos.x) * 0.5,
                (eyePos.y + targetPos.y) * 0.5,
                (eyePos.z + targetPos.z) * 0.5
        );
        Vec3 controlPoint = midPoint.add(
                (perlinNoise(noiseOffset)     - 0.5) * 0.4,
                (perlinNoise(noiseOffset + 1) - 0.5) * 0.2,
                (perlinNoise(noiseOffset + 2) - 0.5) * 0.4
        );
        noiseOffset += 0.05;

        Vec3 bezierPos = bezier(eyePos, controlPoint, targetPos, t);
        double dx   = bezierPos.x - eyePos.x;
        double dy   = bezierPos.y - eyePos.y;
        double dz   = bezierPos.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw   = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));

        float yawDelta   = Mth.wrapDegrees(targetYaw   - BlackOut.mc.player.getYRot());
        float pitchDelta = targetPitch - BlackOut.mc.player.getXRot();

        float appliedStrength = strength.get().floatValue();
        if (dynamicStr.get()) {
            double d = BlackOut.mc.player.distanceToSqr(currentTarget) / (range.get() * range.get());
            appliedStrength *= 0.7f + 0.3f * easeOutCubic(1.0 - d);
        }

        float fraction   = appliedStrength * (0.2f + (float) Math.random() * 0.3f);
        float smoothYaw   = BlackOut.mc.player.getYRot()  + yawDelta   * fraction;
        float smoothPitch = BlackOut.mc.player.getXRot() + pitchDelta * fraction;

        smoothYaw   += (float) ((perlinNoise(noiseOffset + 3) - 0.5) * 0.3);
        smoothPitch += (float) ((perlinNoise(noiseOffset + 4) - 0.5) * 0.3);

        BlackOut.mc.player.setYRot(smoothYaw);
        BlackOut.mc.player.setXRot(Mth.clamp(smoothPitch, -90.0f, 90.0f));
        BlackOut.mc.player.yHeadRot = smoothYaw;
        BlackOut.mc.player.yBodyRot = smoothYaw;

        t += fraction * (0.7 + Math.random() * 0.3);
        targetProgress.put(currentTarget, Math.min(t, 1.0));
    }

    private Vec3 bezier(Vec3 start, Vec3 ctrl, Vec3 end, double t) {
        double u = 1.0 - t;
        return new Vec3(
                u * u * start.x + 2 * u * t * ctrl.x + t * t * end.x,
                u * u * start.y + 2 * u * t * ctrl.y + t * t * end.y,
                u * u * start.z + 2 * u * t * ctrl.z + t * t * end.z
        );
    }

    private float easeOutCubic(double x) {
        double sign = Math.signum(x);
        double abs  = Math.abs(x);
        return (float) (sign * (1.0 - Math.pow(1.0 - abs, 3.0)));
    }

    private float getYawTo(Player player) {
        Vec3 diff = player.position().subtract(BlackOut.mc.player.getEyePosition());
        return (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
    }

    private double perlinNoise(double x) {
        return ((Math.sin(x * 12.9898 + Math.cos(x * 78.233)) * 43758.5453) % 1.0 + 1.0) % 1.0;
    }
}