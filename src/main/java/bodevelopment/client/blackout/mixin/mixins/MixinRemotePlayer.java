package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.misc.NoInterpolation;
import bodevelopment.client.blackout.module.modules.combat.offensive.BackTrack;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.util.BoxUtils;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RemotePlayer.class)
public class MixinRemotePlayer {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/InterpolationHandler;interpolate()V"))
    private void updatePos(InterpolationHandler handler) {
        RemotePlayer instance = (RemotePlayer) (Object) this;
        BackTrack backTrack = BackTrack.getInstance();
        NoInterpolation noInterpolation = NoInterpolation.getInstance();
        Vec3 prevPos = instance.position();

        Vec3 targetPos = handler.position();
        float targetYRot = handler.yRot();
        float targetXRot = handler.xRot();

        if (noInterpolation.enabled) {
            handler.cancel();
        } else {
            handler.interpolate();
            targetPos = instance.position();
            targetYRot = instance.getYRot();
            targetXRot = instance.getXRot();
        }

        backTrack.realPositions.removeKey(instance);
        if (backTrack.enabled) {
            TickTimerList.TickTimer<Pair<RemotePlayer, AABB>> t = backTrack.spoofed
                    .get(timer -> timer.value.getA().equals(instance) && timer.ticks > 3);
            if (t != null) {
                backTrack.realPositions.add(instance, targetPos, 1.0);
                this.setPosition(instance, BoxUtils.feet(t.value.getB()), prevPos);
                return;
            }
        }

        if (noInterpolation.enabled) {
            instance.setPos(targetPos);
            instance.setYRot(targetYRot);
            instance.setXRot(targetXRot);
        }
        Managers.EXTRAPOLATION.tick(instance, targetPos.subtract(prevPos));
    }

    @Unique
    private void setPosition(RemotePlayer instance, Vec3 pos, Vec3 prev) {
        instance.setPos(pos);
        Managers.EXTRAPOLATION.tick(instance, pos.subtract(prev));
    }
}
