/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.interfaces.mixin.IEndCrystal;
import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalRenderState;
import bodevelopment.client.blackout.module.modules.visual.entities.CrystalChams;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(EndCrystalRenderer.class)
@Internal
public abstract class MixinEndCrystalRenderer {
    @Unique
    private final Random random = new Random();

    @Shadow
    @Final
    private EndCrystalModel model;

    @Unique
    private long seed = 0L;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;F)V", at = @At("RETURN"))
    private void onUpdateState(EndCrystal entity, EndCrystalRenderState state, float f, CallbackInfo ci) {
        ((IEndCrystalRenderState) state).blackout_Client$setSpawnTime(
                ((IEndCrystal) entity).blackout_Client$getSpawnTime()
        );
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.crystalBase.get()) {
            state.showsBottom = false;
        }
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
            long spawnTime = ((IEndCrystalRenderState) state).blackout_Client$getSpawnTime();
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