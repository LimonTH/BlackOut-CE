package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalState;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.entities.CrystalChams;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer {
    @Unique
    private final Random random = new Random();
    @Shadow
    @Final
    private EndCrystalEntityModel model;
    @Unique
    private long seed = 0L;

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V", at = @At("RETURN"))
    private void onUpdateState(EndCrystalEntity entity, EndCrystalEntityRenderState state, float f, CallbackInfo ci) {
        ((IEndCrystalState) state).blackout_Client$setSpawnTime(
                ((bodevelopment.client.blackout.interfaces.mixin.IEndCrystalEntity) entity).blackout_Client$getSpawnTime()
        );
    }

    @Inject(method = "shouldRender*", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(EndCrystalEntity entity, net.minecraft.client.render.Frustum frustum, double d, double e, double f, CallbackInfoReturnable<Boolean> cir) {
        if (!Managers.ENTITY.shouldRender(entity.getId())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER)
    )
    private void preRender(EndCrystalEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        CrystalChams crystalChams = CrystalChams.getInstance();

        if (crystalChams.enabled) {
            this.seed = (long) (state.x * 1000.0 + state.y * 1000.0 + state.z * 1000.0);

            float scale = getCrystalScale(state);
            matrixStack.scale(0.5F, 0.5F, 0.5F);
            matrixStack.scale(scale, scale, scale);

            float customY = getCustomBounce(state);
            matrixStack.translate(0.0F, customY, 0.0F);
        }
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"
            )
    )
    private void renderPart(EndCrystalEntityModel instance, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        if (crystalChams.enabled) {
            crystalChams.renderBox(matrices, 0);
            crystalChams.renderBox(matrices, 1);
            crystalChams.renderBox(matrices, 2);
        } else {
            instance.render(matrices, vertices, light, overlay);
        }
    }

    @Unique
    private float getCrystalScale(EndCrystalEntityRenderState state) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        float baseScale = crystalChams.scale.get().floatValue() * 2.0F;

        if (crystalChams.spawnAnimation.get()) {
            long spawnTime = ((IEndCrystalState) state).blackout_Client$getSpawnTime();
            float animTime = crystalChams.animationTime.get().floatValue() * 1000.0F;
            return MathHelper.clampedLerp(0.0F, baseScale, Math.min((float) (System.currentTimeMillis() - spawnTime), animTime) / animTime);
        }
        return baseScale;
    }

    @Unique
    private float getCustomBounce(EndCrystalEntityRenderState state) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        this.setSeed();
        float f = state.age * crystalChams.bounceSpeed.get().floatValue();
        float g = MathHelper.sin(f * 0.2F) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F;
        return (float) (crystalChams.y.get() + g * crystalChams.bounce.get());
    }

    @Unique
    private void setSeed() {
        this.random.setSeed(this.seed);
    }
}