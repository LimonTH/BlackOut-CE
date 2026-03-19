package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        com.mojang.blaze3d.platform.Window window = BlackOut.mc.getWindow();
        float screenWidth = (float) window.getScreenWidth();
        float screenHeight = (float) window.getScreenHeight();
        Renderer.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, screenWidth, screenHeight, 0.0F, -1000.0F, 3000.0F));

        if (!((Object) this instanceof TitleScreen)) {
            float fade = MainMenu.globalFade;
            if (fade < 1.0F) {
                MainMenu.globalFade = Math.min(1.0F, MainMenu.globalFade + 0.015F);
            }
        }
    }
}
