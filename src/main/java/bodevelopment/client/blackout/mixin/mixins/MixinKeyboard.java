package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.keys.Keys;
import bodevelopment.client.blackout.util.SharedFeatures;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {
    @Inject(method = "onKey", at = @At("HEAD"))
    public void onKeyPressed(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key >= 0 && action == 0 || action == 1) {
            Keys.set(key, action == 1);
        }
    }

    @Redirect(
            method = "onKey",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", opcode = Opcodes.GETFIELD)
    )
    private Screen redirectScreen(MinecraftClient instance) {
        return SharedFeatures.shouldSilentScreen() ? null : instance.currentScreen;
    }
}
