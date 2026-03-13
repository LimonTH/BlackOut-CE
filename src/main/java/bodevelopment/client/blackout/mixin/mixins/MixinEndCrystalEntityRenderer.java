package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalState;
import bodevelopment.client.blackout.module.modules.visual.entities.CrystalChams;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

@Mixin(EndCrystalRenderer.class)
public abstract class MixinEndCrystalEntityRenderer {
    @Unique
    private final Random random = new Random();

    @Shadow @Final private EndCrystalModel model;

    @Unique private long seed = 0L;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;F)V", at = @At("RETURN"))
    private void onUpdateState(EndCrystal entity, EndCrystalRenderState state, float f, CallbackInfo ci) {
        ((IEndCrystalState) state).blackout_Client$setSpawnTime(
                ((bodevelopment.client.blackout.interfaces.mixin.IEndCrystalEntity) entity).blackout_Client$getSpawnTime()
        );
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void onRender(EndCrystalRenderState state, PoseStack matrices, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        if (!crystalChams.enabled) return;

        ci.cancel();

        this.seed = (long) (state.x * 1000.0 + state.y * 1000.0 + state.z * 1000.0);
        this.setSeed();

        float age;
        if (crystalChams.rotationSync.get()) {
            age = crystalChams.age + this.random.nextInt(100);
        } else {
            age = state.ageInTicks;
        }

        float rotationAge = age * crystalChams.rotationSpeed.get().floatValue() * 3.0F;

        matrices.pushPose();

        matrices.translate(0.0F, 1.0F, 0.0F);

        float scale = getCrystalScale(state);
        matrices.scale(scale, scale, scale);

        float bounce = getCustomBounce(state);
        matrices.translate(0.0F, -0.5F + bounce, 0.0F);

        renderBlackoutCrystal(matrices, rotationAge, i);

        matrices.popPose();

        if (state.beamOffset != null) {
            renderCrystalBeam(state, matrices, vertexConsumerProvider, i);
        }
    }

    @Unique
    private void renderBlackoutCrystal(PoseStack matrices, float rotationAge, int light) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        float sine45 = 0.70710677F;
        Quaternionf rotation = new Quaternionf().setAngleAxis((float) (Math.PI / 3), sine45, 0.0F, sine45);

        matrices.mulPose(Axis.YP.rotationDegrees(rotationAge));
        matrices.mulPose(rotation);
        crystalChams.renderBox(matrices, 2);

        matrices.scale(0.875F, 0.875F, 0.875F);
        matrices.mulPose(rotation);
        matrices.mulPose(Axis.YP.rotationDegrees(rotationAge));
        crystalChams.renderBox(matrices, 1);

        matrices.scale(0.875F, 0.875F, 0.875F);
        matrices.mulPose(rotation);
        matrices.mulPose(Axis.YP.rotationDegrees(rotationAge));
        crystalChams.renderBox(matrices, 0);
    }

    @Unique
    private float getCrystalScale(EndCrystalRenderState state) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        float baseScale = crystalChams.scale.get().floatValue() * 2.0F;
        if (crystalChams.spawnAnimation.get()) {
            long spawnTime = ((IEndCrystalState) state).blackout_Client$getSpawnTime();
            float animTime = crystalChams.animationTime.get().floatValue() * 1000.0F;
            return Mth.clampedLerp(0.0F, baseScale, Math.min((float) (System.currentTimeMillis() - spawnTime), animTime) / animTime);
        }
        return baseScale;
    }

    @Unique
    private float getCustomBounce(EndCrystalRenderState state) {
        CrystalChams crystalChams = CrystalChams.getInstance();
        this.setSeed();

        float r = crystalChams.bounceSync.get() ? (float) (this.random.nextFloat() * 2.0 * Math.PI) : 0.0F;
        float f = (crystalChams.bounceSync.get() ? crystalChams.age : state.ageInTicks);

        float g = Mth.sin(f * 0.2F * crystalChams.bounceSpeed.get().floatValue() + r) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F;

        return (float) (crystalChams.y.get() + 0.5 + g * crystalChams.bounce.get()) / 2.0F;
    }

    @Unique
    private void renderCrystalBeam(EndCrystalRenderState state, PoseStack matrices, MultiBufferSource vcp, int light) {
        float f = EndCrystalRenderer.getY(state.ageInTicks);
        float g = (float) state.beamOffset.x;
        float h = (float) state.beamOffset.y;
        float j = (float) state.beamOffset.z;
        matrices.pushPose();
        matrices.translate(state.beamOffset.x, state.beamOffset.y, state.beamOffset.z);
        EnderDragonRenderer.renderCrystalBeams(-g, -h + f, -j, state.ageInTicks, matrices, vcp, light);
        matrices.popPose();
    }

    @Unique
    private void setSeed() {
        this.random.setSeed(this.seed);
    }
}