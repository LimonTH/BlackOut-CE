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

import bodevelopment.client.blackout.module.modules.visual.entities.Nametags;
import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import bodevelopment.client.blackout.util.render.misc.RenderEntityCapture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
@Internal
public class MixinEntityRenderer {
    @Inject(
            method = "renderNameTag",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shouldRenderNametag(
            EntityRenderState state,
            Component text,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        Entity entity = RenderEntityCapture.CAPTURED_ENTITY.get();

        if (entity != null && !ShaderESP.ignore && this.shouldCancel(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private void onGetPackedLightCoords(Entity entity, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        Brightness brightness = Brightness.getInstance();
        XRay xray = XRay.getInstance();
        if ((xray != null && xray.enabled) || (brightness != null && brightness.enabled)) {
            if (brightness != null && brightness.enabled && brightness.mode.get() == Brightness.Mode.Luminance) {
                cir.setReturnValue(Brightness.luminanceValue);
            } else {
                cir.setReturnValue(15728880);
            }
        }
    }

    @Unique
    private boolean shouldCancel(Entity entity) {
        if (Nametags.shouldCancelLabel(entity)) {
            return true;
        }

        ShaderESP shaderESP = ShaderESP.getInstance();
        return shaderESP.enabled && shaderESP.shouldRender(entity);
    }
}
