package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.ShulkerViewer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

import net.minecraft.text.OrderedText;

@Mixin(DrawContext.class)
public class MixinDrawContext {

    @Inject(method = "drawItemTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawItemTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (shouldCancel()) ci.cancel();
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
    private void onDrawTooltipComponent(TextRenderer textRenderer, List<TooltipComponent> components, Optional<TooltipData> data, int x, int y, CallbackInfo ci) {
        if (shouldCancel()) ci.cancel();
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true)
    private void onDrawTooltipList(TextRenderer textRenderer, List<OrderedText> text, int x, int y, CallbackInfo ci) {
        if (shouldCancel()) ci.cancel();
    }

    @Unique
    private boolean shouldCancel() {
        ShulkerViewer module = ShulkerViewer.getInstance();
        return module != null && module.enabled && module.isHoveringShulker();
    }
}