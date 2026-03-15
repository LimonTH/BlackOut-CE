package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public class MixinCloudRenderer {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(
            int cloudColor,
            CloudStatus cloudStatus,
            float cloudHeight,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Vec3 cameraPos,
            float tickDelta,
            CallbackInfo ci
    ) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.clouds.get()) ci.cancel();
    }
}