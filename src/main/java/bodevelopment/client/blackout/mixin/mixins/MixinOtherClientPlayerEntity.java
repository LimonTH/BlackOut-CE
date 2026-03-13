package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.misc.NoInterpolation;
import bodevelopment.client.blackout.module.modules.combat.offensive.BackTrack;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.util.BoxUtils;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RemotePlayer.class)
public class MixinOtherClientPlayerEntity {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/RemotePlayer;lerpPositionAndRotationStep(IDDDDD)V"))
    private void updatePos(RemotePlayer instance, int steps, double x, double y, double z, double yaw, double pit) {
        BackTrack backTrack = BackTrack.getInstance();
        Vec3 pos = instance.position();
        double[] realPos = this.realPos(instance, steps, x, y, z, yaw, pit);
        backTrack.realPositions.removeKey(instance);
        if (backTrack.enabled) {
            TickTimerList.TickTimer<Pair<RemotePlayer, AABB>> t = backTrack.spoofed
                    .get(timer -> timer.value.getA().equals(instance) && timer.ticks > 3);
            if (t != null) {
                backTrack.realPositions.add(instance, new Vec3(realPos[0], realPos[1], realPos[2]), 1.0);
                this.setPosition(instance, BoxUtils.feet(t.value.getB()), pos);
                return;
            }
        }

        this.setPosition(instance, new Vec3(realPos[0], realPos[1], realPos[2]), pos);
        instance.setYRot((float) realPos[3]);
        instance.setXRot((float) realPos[4]);
    }

    @Unique
    private void setPosition(RemotePlayer instance, Vec3 pos, Vec3 prev) {
        instance.setPos(pos);
        Managers.EXTRAPOLATION.tick(instance, pos.subtract(prev));
    }

    @Unique
    private double[] realPos(RemotePlayer instance, int steps, double x, double y, double z, double yaw, double pit) {
        NoInterpolation noInterpolation = NoInterpolation.getInstance();
        if (!noInterpolation.enabled) {
            double d = 1.0 / steps;
            double e = Mth.lerp(d, instance.getX(), x);
            double f = Mth.lerp(d, instance.getY(), y);
            double g = Mth.lerp(d, instance.getZ(), z);
            float h = (float) Mth.rotLerp(d, instance.getYRot(), yaw);
            float i = (float) Mth.lerp(d, instance.getXRot(), pit);
            return new double[]{e, f, g, h, i};
        } else {
            return new double[]{x, y, z, yaw, pit};
        }
    }
}
