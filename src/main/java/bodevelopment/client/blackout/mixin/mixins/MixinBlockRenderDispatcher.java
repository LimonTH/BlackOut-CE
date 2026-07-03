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

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import bodevelopment.client.blackout.util.render.consumers.XRayVertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
@Internal
public class MixinBlockRenderDispatcher {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void onRenderBatched(BlockState state, BlockPos pos, BlockAndTintGetter world, PoseStack matrices, VertexConsumer vertexConsumer, boolean cull, RandomSource random, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray == null) return;

        if (xray.enabled && !xray.isTarget(state.getBlock())) {
            if (xray.opacity.get() <= 0) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "renderBatched", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer modifyVertexConsumer(
            VertexConsumer vertexConsumer,
            BlockState blockState,
            BlockPos blockPos,
            BlockAndTintGetter world,
            PoseStack poseStack,
            VertexConsumer originalConsumer,
            boolean bl,
            RandomSource randomSource
    ) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled && !xray.isTarget(blockState.getBlock())) {
            if (xray.opacity.get() > 0) {
                return new XRayVertexConsumer(vertexConsumer, xray.opacity.get());
            }
        }
        return vertexConsumer;
    }
}