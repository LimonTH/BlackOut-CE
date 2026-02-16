package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.misc.NoInterpolation;
import bodevelopment.client.blackout.module.modules.combat.offensive.BackTrack;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.util.BoxUtils;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OtherClientPlayerEntity.class)
public class MixinOtherClientPlayerEntity {
    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/OtherClientPlayerEntity;lerpPosAndRotation(IDDDDD)V"))
    private void updatePos(OtherClientPlayerEntity instance, int steps, double x, double y, double z, double yaw, double pit) {
        BackTrack backTrack = BackTrack.getInstance();
        Vec3d pos = instance.getPos();
        double[] realPos = this.realPos(instance, steps, x, y, z, yaw, pit);
        backTrack.realPositions.removeKey(instance);
        if (backTrack.enabled) {
            TickTimerList.TickTimer<Pair<OtherClientPlayerEntity, Box>> t = backTrack.spoofed
                    .get(timer -> timer.value.getLeft().equals(instance) && timer.ticks > 3);
            if (t != null) {
                backTrack.realPositions.add(instance, new Vec3d(realPos[0], realPos[1], realPos[2]), 1.0);
                this.setPosition(instance, BoxUtils.feet(t.value.getRight()), pos);
                return;
            }
        }

        this.setPosition(instance, new Vec3d(realPos[0], realPos[1], realPos[2]), pos);
        instance.setYaw((float) realPos[3]);
        instance.setPitch((float) realPos[4]);
    }

    @Unique
    private void setPosition(OtherClientPlayerEntity instance, Vec3d pos, Vec3d prev) {
        instance.setPosition(pos);
        Managers.EXTRAPOLATION.tick(instance, pos.subtract(prev));
    }

    @Unique
    private double[] realPos(OtherClientPlayerEntity instance, int steps, double x, double y, double z, double yaw, double pit) {
        NoInterpolation noInterpolation = NoInterpolation.getInstance();
        if (!noInterpolation.enabled) {
            double d = 1.0 / steps;
            double e = MathHelper.lerp(d, instance.getX(), x);
            double f = MathHelper.lerp(d, instance.getY(), y);
            double g = MathHelper.lerp(d, instance.getZ(), z);
            float h = (float) MathHelper.lerpAngleDegrees(d, instance.getYaw(), yaw);
            float i = (float) MathHelper.lerp(d, instance.getPitch(), pit);
            return new double[]{e, f, g, h, i};
        } else {
            return new double[]{x, y, z, yaw, pit};
        }
    }
}
