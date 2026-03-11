package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalState;
import bodevelopment.client.blackout.module.modules.visual.entities.CrystalChams;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer {
    @Unique
    private final Random random = new Random();

    @Shadow @Final private EndCrystalEntityModel model;

    @Unique private long seed = 0L;

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V", at = @At("RETURN"))
    private void onUpdateState(EndCrystalEntity entity, EndCrystalEntityRenderState state, float f, CallbackInfo ci) {
        ((IEndCrystalState) state).blackout_Client$setSpawnTime(
                ((bodevelopment.client.blackout.interfaces.mixin.IEndCrystalEntity) entity).blackout_Client$getSpawnTime()
        );
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void onRender(EndCrystalEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        if (!crystalChams.enabled) return;

        ci.cancel();

        this.seed = (long) (state.x * 1000.0 + state.y * 1000.0 + state.z * 1000.0);
        this.setSeed();

        float age;
        if (crystalChams.rotationSync.get()) {
            age = crystalChams.age + this.random.nextInt(100);
        } else {
            age = state.age;
        }

        float rotationAge = age * crystalChams.rotationSpeed.get().floatValue() * 3.0F;

        matrices.push();

        matrices.translate(0.0F, 1.0F, 0.0F);

        float scale = getCrystalScale(state);
        matrices.scale(scale, scale, scale);

        float bounce = getCustomBounce(state);
        matrices.translate(0.0F, -0.5F + bounce, 0.0F);

        renderBlackoutCrystal(matrices, rotationAge, i);

        matrices.pop();

        if (state.beamOffset != null) {
            renderCrystalBeam(state, matrices, vertexConsumerProvider, i);
        }
    }

    @Unique
    private void renderBlackoutCrystal(MatrixStack matrices, float rotationAge, int light) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        float sine45 = 0.70710677F;
        Quaternionf rotation = new Quaternionf().setAngleAxis((float) (Math.PI / 3), sine45, 0.0F, sine45);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAge));
        matrices.multiply(rotation);
        crystalChams.renderBox(matrices, 2);

        matrices.scale(0.875F, 0.875F, 0.875F);
        matrices.multiply(rotation);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAge));
        crystalChams.renderBox(matrices, 1);

        matrices.scale(0.875F, 0.875F, 0.875F);
        matrices.multiply(rotation);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAge));
        crystalChams.renderBox(matrices, 0);
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

        float r = crystalChams.bounceSync.get() ? (float) (this.random.nextFloat() * 2.0 * Math.PI) : 0.0F;
        float f = (crystalChams.bounceSync.get() ? crystalChams.age : state.age);

        float g = MathHelper.sin(f * 0.2F * crystalChams.bounceSpeed.get().floatValue() + r) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F;

        return (float) (crystalChams.y.get() + 0.5 + g * crystalChams.bounce.get()) / 2.0F;
    }

    @Unique
    private void renderCrystalBeam(EndCrystalEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vcp, int light) {
        float f = EndCrystalEntityRenderer.getYOffset(state.age);
        float g = (float) state.beamOffset.x;
        float h = (float) state.beamOffset.y;
        float j = (float) state.beamOffset.z;
        matrices.push();
        matrices.translate(state.beamOffset.x, state.beamOffset.y, state.beamOffset.z);
        EnderDragonEntityRenderer.renderCrystalBeam(-g, -h + f, -j, state.age, matrices, vcp, light);
        matrices.pop();
    }

    @Unique
    private void setSeed() {
        this.random.setSeed(this.seed);
    }
}