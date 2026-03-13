package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.entities.Nametags;
import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import bodevelopment.client.blackout.util.render.RenderEntityCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(
            method = "renderNameTag",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shouldRenderNametag(
            EntityRenderState state,
            Component text,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        Entity entity = RenderEntityCapture.CAPTURED_ENTITY.get();

        if (entity != null && !ShaderESP.ignore && this.shouldCancel(entity)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean shouldCancel(Entity entity) {
        if (Nametags.shouldCancelLabel(entity)) {
            return true;
        }

        ShaderESP shaderESP = ShaderESP.getInstance();
        return shaderESP.enabled && shaderESP.shouldRender(entity);
    }
}
