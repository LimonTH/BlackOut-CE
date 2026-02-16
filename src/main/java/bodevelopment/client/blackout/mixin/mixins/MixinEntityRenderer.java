package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.entities.Nametags;
import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    @Inject(
            method = "renderLabelIfPresent",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends Entity> void shouldRenderNametag(
            T entity,
            Text text,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (!ShaderESP.ignore && this.shouldCancel(entity)) {
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
