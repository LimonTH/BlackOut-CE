package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.misc.AntiPose;
import bodevelopment.client.blackout.module.modules.movement.SafeWalk;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {
    @Shadow
    protected abstract boolean canChangeIntoPose(EntityPose pose);

    @Redirect(method = "adjustMovementForSneaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;clipAtLedge()Z"))
    private boolean sneakingThing(PlayerEntity instance) {
        if (instance.isSneaking()) return true;
        if ((Object) this == BlackOut.mc.player) {
            SafeWalk.shouldSafeWalk();
        }
        return false;
    }

    @Redirect(
            method = "updatePose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;canChangeIntoPose(Lnet/minecraft/entity/EntityPose;)Z", ordinal = 1)
    )
    private boolean canEnterPose(PlayerEntity instance, EntityPose pose) {
        return instance == BlackOut.mc.player && AntiPose.getInstance().enabled || this.canChangeIntoPose(pose);
    }
}
