package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Shadow
    private boolean renderShadows;

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderShadows:Z", opcode = 180))
    private boolean shouldRenderShadows(EntityRenderDispatcher instance) {
        Brightness brightness = Brightness.getInstance();
        return (!brightness.enabled || brightness.mode.get() != Brightness.Mode.Gamma) && this.renderShadows;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/entity/Entity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private <T extends Entity> void onRender(
            EntityRenderer<T> instance, T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light
    ) {
        ShaderESP esp = ShaderESP.getInstance();
        if (esp.enabled && esp.shouldRender(entity)) {
            esp.onRender(instance, entity, yaw, tickDelta, matrices, vertexConsumers, light);
        } else {
            instance.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        }
    }
}
