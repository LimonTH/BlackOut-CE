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
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
@Internal
public class MixinLiquidBlockRenderer {
    @Unique
    private static final ThreadLocal<Boolean> XRAY_TARGET_FLUID = ThreadLocal.withInitial(() -> false);

    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void onRenderFluid(BlockAndTintGetter world, BlockPos pos, VertexConsumer vertexConsumer,
                               BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) {
            XRAY_TARGET_FLUID.set(false);
            return;
        }
        if (xray.isTarget(blockState.getBlock())) {
            XRAY_TARGET_FLUID.set(true);
            return;
        }
        XRAY_TARGET_FLUID.set(false);
        if (xray.opacity.get() <= 0) ci.cancel();
    }

    @Inject(method = "tesselate", at = @At("RETURN"))
    private void clearTargetFlag(BlockAndTintGetter world, BlockPos pos, VertexConsumer vertexConsumer,
                                 BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        XRAY_TARGET_FLUID.set(false);
    }

    @Redirect(
            method = "tesselate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;shouldRenderFace(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z"
            )
    )
    private boolean redirectShouldRenderFace(
            FluidState fluidState,
            BlockState blockState,
            Direction direction,
            FluidState neighborFluidState
    ) {
        if (XRAY_TARGET_FLUID.get()) {
            return !neighborFluidState.getType().isSame(fluidState.getType());
        }
        return LiquidBlockRenderer.shouldRenderFace(fluidState, blockState, direction, neighborFluidState);
    }

    @Redirect(
            method = "tesselate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean redirectIsFaceOccludedByNeighbor(
            Direction direction,
            float height,
            BlockState neighborState
    ) {
        if (XRAY_TARGET_FLUID.get()) return false;
        return net.minecraft.world.phys.shapes.Shapes.blockOccudes(
                net.minecraft.world.phys.shapes.Shapes.box(0, 0, 0, 1, height, 1),
                neighborState.getFaceOcclusionShape(direction.getOpposite()),
                direction
        );
    }
}