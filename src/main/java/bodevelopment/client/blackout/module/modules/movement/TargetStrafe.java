package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TargetStrafe extends Module {
    private static TargetStrafe INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> preferredDist = this.sgGeneral.doubleSetting("Orbital Radius", 1.0, 0.0, 6.0, 0.1, "The target distance to maintain from the entity while strafing.");
    private final Setting<Double> approach = this.sgGeneral.doubleSetting("Approach Rate", 1.0, 0.0, 1.0, 0.01, "The speed at which the module adjusts the orbital radius to match the Preferred Distance.");
    private final Setting<Boolean> auraTarget = this.sgGeneral.booleanSetting("Aura Sync", true, "Automatically targets the entity currently selected by the Kill Aura module.");
    private final Setting<Double> range = this.sgGeneral.doubleSetting("Search Range", 4.0, 0.0, 10.0, 0.1, "The maximum distance to look for a potential target when not synced with Aura.", () -> !this.auraTarget.get());

    private Double bestYaw;
    private double closest;
    private boolean valid;
    private boolean right = false;
    private int sinceCollide = 0;
    private PlayerEntity target;

    public TargetStrafe() {
        super("Target Strafe", "Maintains a circular orbital path around a target to maximize evasion during combat.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static TargetStrafe getInstance() {
        return INSTANCE;
    }

    public void onMove(Vec3d movement) {
        this.sinceCollide++;
        this.target = this.getTarget();
        if (this.target != null) {
            double speed = movement.horizontalLength();
            if (!(speed <= 0.0)) {
                double yaw = this.getYaw(speed);
                if (this.valid) {
                    double x = Math.cos(yaw);
                    double z = Math.sin(yaw);
                    ((IVec3d) movement).blackout_Client$setXZ(x * speed, z * speed);
                }
            }
        }
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.player.horizontalCollision && this.sinceCollide > 10) {
            this.sinceCollide = 0;
            this.right = !this.right;
        }
    }

    private double getYaw(double movement) {
        if (!Aura.getInstance().enabled) {
            this.valid = false;
            return 0.0;
        } else {
            this.closest = 10000.0;
            this.bestYaw = null;
            this.calc(this.right, movement);
            if (this.bestYaw == null) {
                this.valid = false;
                return 0.0;
            } else {
                this.valid = true;
                return this.bestYaw;
            }
        }
    }

    private void calc(boolean right, double movement) {
        double distance = BlackOut.mc.player.getPos().subtract(this.target.getPos()).horizontalLength();

        for (double delta = -1.0; delta <= 1.0; delta += 0.01) {
            double d = distance + delta * movement * this.approach.get();
            double diff = Math.abs(d - this.preferredDist.get());
            if (!(diff >= this.closest)) {
                Double yaw = this.doTheMathing(movement, d, distance, right);
                if (yaw != null) {
                    Vec3d vec = new Vec3d(d * Math.cos(yaw), 0.0, d * Math.sin(yaw)).add(BlackOut.mc.player.getPos());
                    double width = 0.3;
                    double height = 1.8;
                    Box box = new Box(
                            vec.getX() - width,
                            vec.getY(),
                            vec.getZ() - width,
                            vec.getX() + width,
                            vec.getY() + height,
                            vec.getZ() + width
                    );
                    if (!OLEPOSSUtils.inLava(box) && !this.wouldFall(box, this.target.getY())) {
                        this.closest = diff;
                        this.bestYaw = yaw;
                    }
                }
            }
        }
    }

    private Double doTheMathing(double movement, double preferred, double distance, boolean reversed) {
        double val = (preferred * preferred - distance * distance - movement * movement) / (-2.0 * distance * movement);
        double angle = Math.acos(val);
        return Double.isNaN(angle) ? null : Math.toRadians(RotationUtils.getYaw(this.target)) + Math.abs(angle) * (reversed ? 1 : -1) + (float) (Math.PI / 2);
    }

    private boolean wouldFall(Box box, double y) {
        double diff = Math.min(BlackOut.mc.player.getY() - y, 0.0);
        return !OLEPOSSUtils.inside(BlackOut.mc.player, box.stretch(0.0, diff - 2.5, 0.0));
    }

    private PlayerEntity getTarget() {
        if (this.auraTarget.get()) {
            return Aura.targetedPlayer;
        } else {
            PlayerEntity closest = null;
            double closestDist = 0.0;

            for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
                if (player != BlackOut.mc.player && !Managers.FRIENDS.isFriend(player)) {
                    double dist = BlackOut.mc.player.distanceTo(player);
                    if (!(dist > this.range.get()) && (closest == null || !(dist > closestDist))) {
                        closest = player;
                        closestDist = dist;
                    }
                }
            }

            return closest;
        }
    }
}
