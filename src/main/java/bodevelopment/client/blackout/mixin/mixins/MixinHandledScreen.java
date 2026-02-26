package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IHandledScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.misc.ShulkerViewer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreen implements IHandledScreen {
    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Override
    public Slot blackout_Client$getFocusedSlot() {
        return this.focusedSlot;
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("TAIL"))
    private void onDrawTooltipPost(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        ShulkerViewer module = ShulkerViewer.getInstance();
        if (module != null && module.enabled) {
            module.renderOnTop(context, mouseX, mouseY);
        }
    }
}
