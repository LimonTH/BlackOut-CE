package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IAbstractContainerScreen;
import bodevelopment.client.blackout.module.modules.visual.misc.ShulkerViewer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen implements IAbstractContainerScreen {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Override
    public Slot blackout_Client$getFocusedSlot() {
        return this.hoveredSlot;
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawTooltipPre(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
        ShulkerViewer module = ShulkerViewer.getInstance();
        if (module != null && module.enabled && module.isHoveringShulker()) {
            module.renderOnTop(context, mouseX, mouseY);
            ci.cancel();
        }
    }
}
