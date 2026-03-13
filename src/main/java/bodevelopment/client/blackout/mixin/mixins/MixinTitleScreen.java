package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.gui.menu.MainMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void onInit(CallbackInfo ci) {
        this.clearWidgets();
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != this) {
            return;
        }

        ci.cancel();

        MainMenu.getInstance().set((TitleScreen) (Object) this);
        MainMenu.getInstance().render(mouseX, mouseY, delta);
    }
}
