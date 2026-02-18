package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.gui.menu.MainMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof TitleScreen)) {
            float fade = MainMenu.globalFade;
            if (fade < 1.0F) {
                MainMenu.globalFade = Math.min(1.0F, MainMenu.globalFade + 0.015F);
                RenderSystem.setShaderColor(fade, fade, fade, 1.0F);
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}