package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public class MixinArmorFeatureRenderer {

    @Inject(
            method = "renderArmor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderArmor(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            ItemStack stack,
            EquipmentSlot slot,
            int light,
            BipedEntityModel<?> armorModel,
            CallbackInfo ci
    ) {
        if (WireframeRenderer.hidden || NoRender.getInstance().ignoreArmor(slot)) {
            ci.cancel();
        }
    }
}
