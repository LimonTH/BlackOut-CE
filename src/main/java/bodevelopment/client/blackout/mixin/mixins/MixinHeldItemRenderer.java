package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.modules.misc.Zoomify;
import bodevelopment.client.blackout.module.modules.visual.misc.SwingModifier;
import bodevelopment.client.blackout.module.modules.visual.misc.ViewModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {
    @Unique
    private PoseStack matrices;
    @Unique
    private ItemStack item;
    @Unique
    private boolean mainHand;
    @Unique
    private MultiBufferSource vertexConsumers;
    @Unique
    private int light;

    @Shadow public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack matrices, MultiBufferSource vertexConsumers, int light);

    @ModifyArgs(
            method = "renderHandsWithItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void setArgs(Args args) {
        this.matrices = args.get(7);
        this.item = args.get(5);
        this.mainHand = args.get(3) == InteractionHand.MAIN_HAND;
        this.vertexConsumers = args.get(8);
        this.light = args.get(9);

        SwingModifier module = SwingModifier.getInstance();
        if (module.enabled) {
            args.set(6, module.getY(args.get(3)));
            args.set(4, module.getSwing(args.get(3)));
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void preRenderItem(
            AbstractClientPlayer player,
            float tickDelta,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (Zoomify.getInstance().shouldHideHands()) {
            ci.cancel();
            return;
        }
        ViewModel viewModel = ViewModel.getInstance();
        if (viewModel.enabled) {
            if (viewModel.shouldCancel(hand)) {
                ci.cancel();
            } else {
                viewModel.transform(matrices, hand);
            }
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("TAIL"))
    private void postRenderItem(
            AbstractClientPlayer player,
            float tickDelta,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        ViewModel viewModel = ViewModel.getInstance();
        if (viewModel.enabled) {
            viewModel.post(matrices);
        }
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void onRenderItem(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.getInstance();
        if (viewModel.enabled) {
            viewModel.scaleAndRotate(matrices, hand);
        }
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRenderItemPost(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, MultiBufferSource vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = ViewModel.getInstance();
        if (viewModel.enabled) {
            viewModel.postRender(matrices);
        }
    }

    @Redirect(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isScoping()Z")
    )
    private boolean redirectRiptideTransform(AbstractClientPlayer instance) {
        if (instance.isScoping()) {
            return true;
        } else if (this.mainHand && Aura.getInstance().blockTransform(this.matrices)) {
            this.renderItem(
                    instance, this.item, this.mainHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !this.mainHand, this.matrices, this.vertexConsumers, this.light
            );
            this.matrices.popPose();
            return true;
        } else {
            return false;
        }
    }
}
