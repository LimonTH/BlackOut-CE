package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.misc.HandESP;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.util.render.DualVertexConsumer;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void preRender(float f, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer localPlayer, int i, CallbackInfo ci) {
        if (HandESP.getInstance().enabled) {
            HandESP.rendering = true;
            WireframeRenderer.provider.consumer.start();
        }
    }

    @ModifyArg(
            method = "renderHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            ),
            index = 8
    )
    private MultiBufferSource redirectBuffer(MultiBufferSource original) {
        if (HandESP.rendering) {
            return renderType -> {
                VertexConsumer originalConsumer = original.getBuffer(renderType);
                VertexConsumer espConsumer = WireframeRenderer.provider.getBuffer(renderType);
                return new DualVertexConsumer(originalConsumer, espConsumer);
            };
        }
        return original;
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void postRender(float f, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer localPlayer, int i, CallbackInfo ci) {
        if (HandESP.rendering) {
            WireframeRenderer.provider.consumer.fixRemaining();

            FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("handESP");
            buffer.bind(true);

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            WireframeRenderer.drawEverything(new org.joml.Matrix4f(), WireframeRenderer.provider.consumer.vertices, 1f, 1f, 1f, 1f);
            buffer.unbind();
            WireframeRenderer.provider.consumer.vertices.clear();
            HandESP.rendering = false;
        }
    }
}