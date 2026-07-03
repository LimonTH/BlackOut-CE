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

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
@Internal
public class MixinEffectRenderer {
    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void onWallOverlay(TextureAtlasSprite textureAtlasSprite, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.wallOverlay.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void onWaterOverlay(Minecraft minecraft, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.waterOverlay.get()) {
            ci.cancel();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onFireOverlay(PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.fireOverlay.get()) {
            ci.cancel();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(515);
        }
    }
}
