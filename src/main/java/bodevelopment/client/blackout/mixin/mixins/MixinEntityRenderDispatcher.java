package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import bodevelopment.client.blackout.util.render.RenderEntityCapture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    @Shadow
    private boolean renderShadows;

    @Inject(
            method = "render*",
            at = @At("HEAD")
    )
    private <E extends Entity> void captureEntity(E entity, double x, double y, double z, float yaw, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityCapture.CAPTURED_ENTITY.set(entity);
    }

    @Inject(
            method = "render*",
            at = @At("RETURN")
    )
    private <E extends Entity> void releaseEntity(E entity, double x, double y, double z, float yaw, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityCapture.CAPTURED_ENTITY.remove();
    }

    @Redirect(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderShadows:Z",
                    opcode = 180
            )
    )
    private boolean shouldRenderShadows(EntityRenderDispatcher instance) {
        return Brightness.getInstance().enabled && Brightness.getInstance().mode.get() != Brightness.Mode.Gamma && this.renderShadows;
    }

    @Redirect(
            method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"
            )
    )
    private <E extends Entity, S extends EntityRenderState> void onRender(
            EntityRenderer<? super E, S> instance, S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
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
