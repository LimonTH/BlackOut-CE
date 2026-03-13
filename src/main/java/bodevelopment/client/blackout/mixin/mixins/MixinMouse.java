package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.keys.MouseButtons;
import bodevelopment.client.blackout.util.SharedFeatures;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public abstract class MixinMouse {
    @Shadow
    private boolean mouseGrabbed;
    @Shadow
    private double xpos;
    @Shadow
    private double ypos;
    @Shadow
    private boolean ignoreFirstMove;

    @Shadow
    public abstract void grabMouse();

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void onClick(long window, int button, int action, int mods, CallbackInfo ci) {
        MouseButtonEvent event = MouseButtonEvent.get(button, action == 1);
        if (BlackOut.EVENT_BUS.post(event).isCancelled()) {
            ci.cancel();
        }

        MouseButtons.set(button, action == 1);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MouseScrollEvent event = MouseScrollEvent.get(horizontal, vertical);
        if (BlackOut.EVENT_BUS.post(event).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "isMouseGrabbed", at = @At("HEAD"), cancellable = true)
    private void locked(CallbackInfoReturnable<Boolean> cir) {
        if (SharedFeatures.shouldSilentScreen()) {
            this.lockWithoutClose();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void lockWithoutClose() {
        if (BlackOut.mc.isWindowActive()) {
            if (!this.mouseGrabbed) {
                if (!Minecraft.ON_OSX) {
                    KeyMapping.setAll();
                }

                this.mouseGrabbed = true;
                this.ignoreFirstMove = true;
                this.xpos = BlackOut.mc.getWindow().getScreenWidth() / 2.0;
                this.ypos = BlackOut.mc.getWindow().getScreenHeight() / 2.0;
                InputConstants.grabOrReleaseMouse(BlackOut.mc.getWindow().getWindow(), 212995, this.xpos, this.ypos);
            }
        }
    }
}
