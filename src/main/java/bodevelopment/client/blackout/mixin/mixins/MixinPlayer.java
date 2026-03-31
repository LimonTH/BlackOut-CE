package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.misc.AntiPose;
import bodevelopment.client.blackout.module.modules.movement.SafeWalk;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class MixinPlayer {
    @Shadow
    protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose);

    @Shadow
    public abstract boolean isStayingOnGroundSurface();

    @Redirect(method = "maybeBackOffFromEdge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isStayingOnGroundSurface()Z"))
    private boolean sneakingThing(Player instance) {
        if ((Object) this == BlackOut.mc.player && SafeWalk.shouldSafeWalk()) {
            return true;
        }
        return this.isStayingOnGroundSurface();
    }

    @Redirect(
            method = "updatePlayerPose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canPlayerFitWithinBlocksAndEntitiesWhen(Lnet/minecraft/world/entity/Pose;)Z", ordinal = 1)
    )
    private boolean canEnterPose(Player instance, Pose pose) {
        return instance == BlackOut.mc.player && AntiPose.getInstance().enabled || this.canPlayerFitWithinBlocksAndEntitiesWhen(pose);
    }
}
