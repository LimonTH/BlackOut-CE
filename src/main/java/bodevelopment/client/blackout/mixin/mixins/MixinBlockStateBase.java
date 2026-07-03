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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
@Internal
public class MixinBlockStateBase {

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void onIsSolidRender(CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
    private void onGetOpacity(CallbackInfoReturnable<Integer> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(0);
    }

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetAO(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(1.0f);
    }

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void onGetLuminance(CallbackInfoReturnable<Integer> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        Block block = ((BlockBehaviour.BlockStateBase) (Object) this).getBlock();
        if (xray.isTarget(block)) {
            cir.setReturnValue(15);
        }
    }

    @Inject(method = "isCollisionShapeFullBlock", at = @At("HEAD"), cancellable = true)
    private void onIsFullCube(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void onIsOpaque(CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        Block block = ((BlockBehaviour.BlockStateBase) (Object) this).getBlock();
        if (!xray.isTarget(block) && xray.opacity.get() <= 0) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}