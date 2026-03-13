package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import bodevelopment.client.blackout.rendering.renderer.FrameBufferRenderer;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    @Shadow
    @Final
    private Minecraft minecraft;

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
