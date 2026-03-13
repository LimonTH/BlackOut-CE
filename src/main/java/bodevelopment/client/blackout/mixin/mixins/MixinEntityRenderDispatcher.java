package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import bodevelopment.client.blackout.util.render.RenderEntityCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Shadow
    private boolean shouldRenderShadow;

    @Inject(
            method = "render*",
            at = @At("HEAD")
    )
    private <E extends Entity> void captureEntity(E entity, double x, double y, double z, float yaw, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityCapture.CAPTURED_ENTITY.set(entity);
    }

    @Inject(
            method = "render*",
            at = @At("RETURN")
    )
    private <E extends Entity> void releaseEntity(E entity, double x, double y, double z, float yaw, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityCapture.CAPTURED_ENTITY.remove();
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRenderShadow:Z",
                    opcode = 180
            )
    )
    private boolean shouldRenderShadows(EntityRenderDispatcher instance) {
        return Brightness.getInstance().enabled && Brightness.getInstance().mode.get() != Brightness.Mode.Gamma && this.shouldRenderShadow;
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private <E extends Entity, S extends EntityRenderState> void onRender(
            EntityRenderer<? super E, S> instance, S state, PoseStack matrices, MultiBufferSource vertexConsumers, int light,
            E entity, double x, double y, double z, float tickDelta
    ) {
        ShaderESP esp = ShaderESP.getInstance();

        if (esp.enabled && esp.shouldRender(entity)) {
            esp.onRender(instance, entity, state, matrices, vertexConsumers, light);
        } else {
            instance.render(state, matrices, vertexConsumers, light);
        }
    }
}
