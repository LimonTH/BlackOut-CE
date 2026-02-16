package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void getRain(float delta, CallbackInfoReturnable<Float> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyWeather.get()) {
            cir.setReturnValue(ambience.raining.get().floatValue());
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void getThunder(float delta, CallbackInfoReturnable<Float> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyWeather.get()) {
            cir.setReturnValue(ambience.thunder.get().floatValue());
        }
    }
}
