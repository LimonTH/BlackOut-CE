package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/FogParameters;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderSky(
            FrameGraphBuilder frameGraphBuilder,
            Camera camera,
            float tickDelta,
            net.minecraft.client.renderer.FogParameters fog,
            CallbackInfo ci
    ) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.thickFog.get() && !ambience.removeFog.get()) {
            ci.cancel();
        }
    }
}
