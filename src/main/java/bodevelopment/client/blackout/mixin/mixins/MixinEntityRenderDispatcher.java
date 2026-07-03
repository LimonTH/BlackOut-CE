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

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.modules.visual.misc.HandESP;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.util.render.CapeRenderContext;
import bodevelopment.client.blackout.util.Capes;
import bodevelopment.client.blackout.util.CompatUtils;
import bodevelopment.client.blackout.util.render.consumers.DualVertexConsumer;
import bodevelopment.client.blackout.util.render.misc.FramebufferMultiBufferSource;
import bodevelopment.client.blackout.util.render.misc.RenderEntityCapture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
@Internal
public class MixinEntityRenderDispatcher {
    @Unique
    private final FramebufferMultiBufferSource blackout$handEspFboSource = new FramebufferMultiBufferSource();
    @Shadow
    private boolean shouldRenderShadow;

    @ModifyVariable(
            method = "renderShadow",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static float noShadow(float shadowRadius) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.shadows.get()) return 0.0f;
        return shadowRadius;
    }

    @Inject(
            method = "render*",
            at = @At("HEAD")
    )
    private <E extends Entity> void captureEntity(E entity, double x, double y, double z, float yaw, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityCapture.CAPTURED_ENTITY.set(entity);
    }

    @Inject(
            method = "render*",
            at = @At("RETURN")
    )
    private <E extends Entity> void releaseEntity(E entity, double x, double y, double z, float yaw, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        RenderEntityCapture.CAPTURED_ENTITY.remove();
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRenderShadow:Z",
                    opcode = 180
            )
    )
    private boolean shouldRenderShadows(EntityRenderDispatcher instance) {
        return Brightness.getInstance().enabled && Brightness.getInstance().mode.get() != Brightness.Mode.Gamma && this.shouldRenderShadow;
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private <E extends Entity, S extends EntityRenderState> void onRender(
            EntityRenderer<? super E, S> instance, S state, PoseStack matrices, MultiBufferSource vertexConsumers, int light,
            E entity, double x, double y, double z, float tickDelta
    ) {
        ShaderESP esp = ShaderESP.getInstance();

        if (esp.enabled && esp.shouldRender(entity)) {
            esp.onRender(instance, entity, state, matrices, vertexConsumers, light);
            return;
        }

        if (state instanceof net.minecraft.client.renderer.entity.state.PlayerRenderState playerState) {
            ResourceLocation cape = Capes.getCape(playerState);
            if (cape != null) {
                float[] dims = Capes.getDimensionsFor(cape);
                if (dims != null) {
                    CapeRenderContext.set(cape, dims[0], dims[1]);
                } else {
                    CapeRenderContext.set(cape);
                }
            } else {
                CapeRenderContext.clear();
            }
        }

        HandESP handESP = HandESP.getInstance();
        if (CompatUtils.FirstPersonModel.isLoaded() && handESP.enabled && entity == BlackOut.mc.player
                && BlackOut.mc.options.getCameraType() == CameraType.FIRST_PERSON && !FreeCam.getInstance().enabled) {
            FramebufferMultiBufferSource fboSource = this.blackout$handEspFboSource;

            if (handESP.texture.get()) {
                MultiBufferSource dual = renderType ->
                        new DualVertexConsumer(vertexConsumers.getBuffer(renderType), fboSource.getBuffer(renderType));
                instance.render(state, matrices, dual, light);
            } else {
                instance.render(state, matrices, vertexConsumers, light);
                instance.render(state, matrices, fboSource, light);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("handESP");
            fboSource.drawToFramebuffer(buffer);
            CapeRenderContext.clear();
            return;
        }

        instance.render(state, matrices, vertexConsumers, light);
        CapeRenderContext.clear();
    }
}
