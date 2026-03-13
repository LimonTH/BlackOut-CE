package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.visual.misc.CustomChat;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

public class CustomChatScreen extends ChatScreen {
    private final PoseStack stack = new PoseStack();

    public CustomChatScreen() {
        super("");
    }

    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        CustomChat customChat = CustomChat.getInstance();

        boolean blink = System.currentTimeMillis() % 1000 < 500;

        String cursor = blink ? "_" : "";
        String text = this.input.getValue() + cursor;

        String measurementText = this.input.getValue() + "_";

        float textScale = 2.2F;
        float fontHeight = BlackOut.FONT.getHeight() * textScale;

        float textWidth = BlackOut.FONT.getWidth(measurementText) * textScale;
        float width = textWidth > 250.0F ? textWidth + 10.0F : 250.0F;

        this.stack.pushPose();
        RenderUtils.unGuiScale(this.stack);

        if (customChat.blur.get()) {
            RenderUtils.drawLoadedBlur(
                    "hudblur",
                    this.stack,
                    renderer -> renderer.rounded(10.0F, BlackOut.mc.getWindow().getScreenHeight() - (fontHeight + 16.0F), width, fontHeight + 4.0F, 6.0F, 10)
            );
            Renderer.onHUDBlur();
        }

        if (customChat.background.get()) {
            RenderUtils.rounded(
                    this.stack,
                    10.0F,
                    BlackOut.mc.getWindow().getScreenHeight() - (fontHeight + 16.0F),
                    width,
                    fontHeight + 4.0F,
                    6.0F,
                    customChat.shadow.get() ? 6.0F : 0.0F,
                    customChat.bgColor.get().getRGB(),
                    customChat.shadowColor.get().getRGB()
            );
        }

        customChat.textColor.render(
                this.stack,
                text,
                textScale,
                15.0F,
                BlackOut.mc.getWindow().getScreenHeight() - (fontHeight + 13.0F),
                false,
                false
        );

        this.stack.popPose();
    }
}
