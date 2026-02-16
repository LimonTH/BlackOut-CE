package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.gui.menu.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    protected void onInit(CallbackInfo ci) {
        this.clearChildren();
        ci.cancel();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen != this) {
            return;
        }

        ci.cancel();
        MainMenu.getInstance().set((TitleScreen) (Object) this);
        MainMenu.getInstance().render(mouseX, mouseY, delta);
    }

    @Inject(method = "initWidgetsNormal", at = @At("HEAD"), cancellable = true)
    public void initWidgetsNormal(CallbackInfo ci) {
        ci.cancel();
    }
}
