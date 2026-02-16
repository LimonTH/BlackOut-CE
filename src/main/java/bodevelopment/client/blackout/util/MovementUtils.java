package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class MovementUtils {
    public static double xMovement(double speed, double yaw) {
        return Math.cos(Math.toRadians(yaw + 90.0)) * speed;
    }

    public static double zMovement(double speed, double yaw) {
        return Math.sin(Math.toRadians(yaw + 90.0)) * speed;
    }

    public static double getSpeed(double baseSpeed) {
        return getSpeed(baseSpeed, 1.0);
    }

    public static double getSpeed(double baseSpeed, double multi) {
        double effectMulti = getEffectMulti();
        if (BlackOut.mc.player.isSneaking()) {
            baseSpeed *= 0.3;
            effectMulti++;
        } else {
            effectMulti = 1.0 + effectMulti * multi;
        }

        return baseSpeed * effectMulti;
    }

    public static double getEffectMulti() {
        double multiBonus = 0.0;
        if (BlackOut.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            multiBonus += BlackOut.mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() * 0.2 + 0.2;
        }

        if (BlackOut.mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            multiBonus -= BlackOut.mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() * 0.2 + 0.2;
        }

        return multiBonus;
    }

    public static void moveTowards(Vec3d movement, double baseSpeed, Vec3d vec, int step, int reverseStep) {
        double speed = getSpeed(baseSpeed);
        double yaw = RotationUtils.getYaw(BlackOut.mc.player.getPos(), vec, 0.0);
        double xm = xMovement(speed, yaw);
        double zm = zMovement(speed, yaw);
        double xd = vec.x - BlackOut.mc.player.getX();
        double zd = vec.z - BlackOut.mc.player.getZ();
        double x = Math.abs(xm) <= Math.abs(xd) ? xm : xd;
        double z = Math.abs(zm) <= Math.abs(zd) ? zm : zd;
        y(movement, x, z, step, reverseStep);
        ((IVec3d) movement).blackout_Client$setXZ(x, z);
    }

    private static void y(Vec3d movement, double x, double z, int step, int rev) {
        if (BlackOut.mc.player.isOnGround()
                && !OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox())
                && OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(x, 0.0, z))) {
            double s = getStep(BlackOut.mc.player.getBoundingBox().offset(x, 0.0, z), step);
            if (s > 0.0) {
                ((IVec3d) movement).blackout_Client$setY(s);
                BlackOut.mc.player.setVelocity(BlackOut.mc.player.getVelocity().x, 0.0, BlackOut.mc.player.getVelocity().z);
            }
        } else {
            if (BlackOut.mc.player.isOnGround()
                    && !OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(x, -0.04, z))) {
                double s = getReverse(BlackOut.mc.player.getBoundingBox(), rev);
                if (s > 0.0) {
                    ((IVec3d) movement).blackout_Client$setY(-s);
                    BlackOut.mc.player.setVelocity(BlackOut.mc.player.getVelocity().x, 0.0, BlackOut.mc.player.getVelocity().z);
                }
            }
        }
    }

    private static double getStep(Box box, int step) {
        for (double i = 0.0; i <= step + 0.125; i += 0.125) {
            if (!OLEPOSSUtils.inside(BlackOut.mc.player, box.offset(0.0, i, 0.0))) {
                return i;
            }
        }

        return 0.0;
    }

    private static double getReverse(Box box, int reverse) {
        for (double i = 0.0; i <= reverse; i += 0.125) {
            if (OLEPOSSUtils.inside(BlackOut.mc.player, box.offset(0.0, -i - 0.125, 0.0))) {
                return i;
            }
        }

        return 0.0;
    }
}
