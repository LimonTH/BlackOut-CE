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
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
@Internal
public class MixinFogRenderer {
    @Unique
    private static final FogParameters INVISIBLE_FOG = new FogParameters(
            Float.MAX_VALUE, Float.MAX_VALUE, FogShape.SPHERE, 0.0F, 0.0F, 0.0F, 1.0F
    );

    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private static void onGetFogColor(Camera camera, float tickDelta, ClientLevel world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyFog.get() && ambience.thickFog.get()) {
            BlackOutColor color = ambience.color.get();
            cir.setReturnValue(new Vector4f(color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, 1.0F));
        }
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void onApplyFog(Camera camera, FogRenderer.FogMode fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<FogParameters> cir) {
        Ambience ambience = Ambience.getInstance();
        NoRender noRender = NoRender.getInstance();
        if (ambience != null && ambience.enabled && ambience.modifyFog(fogType == FogRenderer.FogMode.FOG_TERRAIN)) {
            cir.setReturnValue(FogParameters.NO_FOG);
        } else if (noRender != null && noRender.enabled && noRender.fog.get()) {
            cir.setReturnValue(noRender.clouds.get() ? FogParameters.NO_FOG : INVISIBLE_FOG);
        }
    }
}
