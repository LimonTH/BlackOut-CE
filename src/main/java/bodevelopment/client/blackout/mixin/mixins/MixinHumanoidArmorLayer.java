package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class MixinHumanoidArmorLayer {
    @Inject(
            method = "renderArmorPiece",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderArmor(
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            ItemStack stack,
            EquipmentSlot slot,
            int light,
            HumanoidModel<?> armorModel,
            CallbackInfo ci
    ) {
        if (WireframeRenderer.hidden || NoRender.getInstance().ignoreArmor(slot)) {
            ci.cancel();
        }
    }
}
