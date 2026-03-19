package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.modules.misc.Zoomify;
import bodevelopment.client.blackout.module.modules.visual.misc.HandESP;
import bodevelopment.client.blackout.module.modules.visual.misc.SwingModifier;
import bodevelopment.client.blackout.module.modules.visual.misc.ViewModel;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.util.render.DualVertexConsumer;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {
    @Shadow
    public abstract void renderItem(LivingEntity livingEntity, ItemStack itemStack, ItemDisplayContext itemDisplayContext, PoseStack poseStack, MultiBufferSource multiBufferSource, int i);

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void preRender(float f, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer localPlayer, int i, CallbackInfo ci) {
        if (HandESP.getInstance().enabled) {
            HandESP.rendering = true;
            WireframeRenderer.provider.consumer.start();
        }
    }

    @ModifyArgs(method = "renderHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void modifyArmArgs(Args args) {
        InteractionHand hand = args.get(3);

        SwingModifier swingMod = SwingModifier.getInstance();
        if (swingMod.enabled) {
            args.set(6, swingMod.getY(hand));
            args.set(4, swingMod.getSwing(hand));
        }

        if (HandESP.rendering) {
            MultiBufferSource original = args.get(8);
            args.set(8, (MultiBufferSource) renderType ->
                    new DualVertexConsumer(original.getBuffer(renderType), WireframeRenderer.provider.getBuffer(renderType)));
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onPreRenderArm(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swing, ItemStack item, float equip, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        if (Zoomify.getInstance().shouldHideHands()) {
            ci.cancel();
            return;
        }

        ViewModel vm = ViewModel.getInstance();
        if (vm.enabled) {
            if (vm.shouldCancel(hand)) {
                ci.cancel();
            } else {
                vm.transform(matrices, hand);
            }
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("TAIL"))
    private void onPostRenderArm(AbstractClientPlayer player, float tick, float pitch, InteractionHand hand, float swing, ItemStack item, float equip, PoseStack matrices, MultiBufferSource consumers, int light, CallbackInfo ci) {
        if (ViewModel.getInstance().enabled) {
            ViewModel.getInstance().post(matrices);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void onBeforeItemRender(AbstractClientPlayer player, float tick, float pitch, InteractionHand hand, float swing, ItemStack item, float equip, PoseStack matrices, MultiBufferSource consumers, int light, CallbackInfo ci) {
        if (ViewModel.getInstance().enabled) {
            ViewModel.getInstance().scaleAndRotate(matrices, hand);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", shift = At.Shift.AFTER))
    private void onAfterItemRender(AbstractClientPlayer player, float tick, float pitch, InteractionHand hand, float swing, ItemStack item, float equip, PoseStack matrices, MultiBufferSource consumers, int light, CallbackInfo ci) {
        if (ViewModel.getInstance().enabled) {
            ViewModel.getInstance().postRender(matrices);
        }
    }

    @Redirect(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isScoping()Z"))
    private boolean onAuraTransform(AbstractClientPlayer player, AbstractClientPlayer p2, float tick, float pitch, InteractionHand hand, float swing, ItemStack item, float equip, PoseStack matrices, MultiBufferSource consumers, int light) {
        if (player.isScoping()) return true;

        if (hand == InteractionHand.MAIN_HAND && Aura.getInstance().blockTransform(matrices)) {
            this.renderItem(player, item, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, matrices, consumers, light);
            matrices.popPose();
            return true;
        }
        return false;
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void postRender(float f, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, LocalPlayer localPlayer, int i, CallbackInfo ci) {
        if (HandESP.rendering) {
            WireframeRenderer.provider.consumer.fixRemaining();
            FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("handESP");

            buffer.bind(true);
            GlStateManager._enableDepthTest();
            GlStateManager._depthMask(true);
            WireframeRenderer.drawEverything(new org.joml.Matrix4f(), WireframeRenderer.provider.consumer.vertices, 1f, 1f, 1f, 1f);
            buffer.unbind();

            WireframeRenderer.provider.consumer.vertices.clear();
            HandESP.rendering = false;
        }
    }
}