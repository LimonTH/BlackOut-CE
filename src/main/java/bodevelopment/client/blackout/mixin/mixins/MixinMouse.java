package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.keys.MouseButtons;
import bodevelopment.client.blackout.util.SharedFeatures;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mouse.class)
public abstract class MixinMouse {
    @Shadow
    private boolean cursorLocked;
    @Shadow
    private double x;
    @Shadow
    private double y;
    @Shadow
    private boolean hasResolutionChanged;

    @Shadow
    public abstract void lockCursor();

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onClick(long window, int button, int action, int mods, CallbackInfo ci) {
        MouseButtonEvent event = MouseButtonEvent.get(button, action == 1);
        if (BlackOut.EVENT_BUS.post(event).isCancelled()) {
            ci.cancel();
        }

        MouseButtons.set(button, action == 1);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseScrollEvent event = MouseScrollEvent.get(horizontal, vertical);
        if (BlackOut.EVENT_BUS.post(event).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "isCursorLocked", at = @At("HEAD"), cancellable = true)
    private void locked(CallbackInfoReturnable<Boolean> cir) {
        if (SharedFeatures.shouldSilentScreen()) {
            this.lockWithoutClose();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void lockWithoutClose() {
        if (BlackOut.mc.isWindowFocused()) {
            if (!this.cursorLocked) {
                if (!MinecraftClient.IS_SYSTEM_MAC) {
                    KeyBinding.updatePressedStates();
                }

                this.cursorLocked = true;
                this.hasResolutionChanged = true;
                this.x = BlackOut.mc.getWindow().getWidth() / 2.0;
                this.y = BlackOut.mc.getWindow().getHeight() / 2.0;
                InputUtil.setCursorParameters(BlackOut.mc.getWindow().getHandle(), 212995, this.x, this.y);
            }
        }
    }
}
