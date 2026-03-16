package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class MixinWeatherEffectRenderer {
    @Inject(method = "render*", at = @At("HEAD"), cancellable = true)
    private void onRender(
            Level level,
            MultiBufferSource bufferSource,
            int ticks,
            float tickDelta,
            Vec3 cameraPos,
            CallbackInfo ci
    ) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.weather.get()) ci.cancel();
    }
}