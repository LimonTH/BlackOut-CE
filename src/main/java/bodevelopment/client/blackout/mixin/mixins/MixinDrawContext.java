package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.ShulkerViewer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public class MixinDrawContext {

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void onDrawItemTooltip(Font textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (shouldCancel()) ci.cancel();
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
    private void onDrawTooltipComponent(Font textRenderer, List<ClientTooltipComponent> components, Optional<TooltipComponent> data, int x, int y, CallbackInfo ci) {
        if (shouldCancel()) ci.cancel();
    }

    @Inject(method = "renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", at = @At("HEAD"), cancellable = true)
    private void onDrawTooltipList(Font textRenderer, List<FormattedCharSequence> text, int x, int y, CallbackInfo ci) {
        if (shouldCancel()) ci.cancel();
    }

    @Unique
    private boolean shouldCancel() {
        ShulkerViewer module = ShulkerViewer.getInstance();
        return module != null && module.enabled && module.isHoveringShulker();
    }
}