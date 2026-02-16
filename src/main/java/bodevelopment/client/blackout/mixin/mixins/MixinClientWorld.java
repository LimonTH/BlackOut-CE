package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.EntityAddEvent;
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld {
    @Shadow
    public abstract void setTime(long time);

    @Shadow
    public abstract void setTimeOfDay(long time);

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void preAddEntity(Entity entity, CallbackInfo ci) {
        if (BlackOut.EVENT_BUS.post(EntityAddEvent.Pre.get(entity.getId(), entity)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "addEntity", at = @At("TAIL"))
    private void postAddEntity(Entity entity, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(EntityAddEvent.Post.get(entity.getId(), entity));
    }

    @Redirect(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTimeOfDay(J)V"))
    private void tickGameTime(ClientWorld instance, long timeOfDay) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyTime.get()) {
            this.setTimeOfDay(ambience.time.get());
        } else {
            instance.setTimeOfDay(timeOfDay);
        }
    }
}
