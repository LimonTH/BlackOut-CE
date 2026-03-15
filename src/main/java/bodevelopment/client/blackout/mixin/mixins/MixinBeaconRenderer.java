package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconRenderer.class)
public class MixinBeaconRenderer {
    @Inject(method = "renderBeaconBeam", at = @At("HEAD"), cancellable = true)
    private static void noBeaconBeam(
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            float tickDelta,
            long time,
            int yOffset,
            int height,
            int color,
            CallbackInfo ci
    ) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.beaconBeam.get()) ci.cancel();
    }
}