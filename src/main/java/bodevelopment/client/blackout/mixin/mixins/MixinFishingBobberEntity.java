package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.movement.Velocity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
public class MixinFishingBobberEntity {
    @Inject(method = "pullHookedEntity", at = @At("HEAD"), cancellable = true)
    private void onPull(Entity entity, CallbackInfo ci) {
        if (entity == BlackOut.mc.player) {
            Velocity velocity = Velocity.getInstance();
            if (velocity.enabled && velocity.fishingHook.get()) {
                ci.cancel();
            }
        }
    }
}
