package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.visual.misc.CustomChat;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;

public class CustomChatScreen extends ChatScreen {
    private final MatrixStack stack = new MatrixStack();

    public CustomChatScreen() {
        super("");
    }

    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        CustomChat customChat = CustomChat.getInstance();
        String text = this.chatField.getText() + "_";
        double f = Math.sin(System.currentTimeMillis() / 1000.0 * 2.0) + 1.0;
        float textScale = 2.2F;
        float fontHeight = BlackOut.FONT.getHeight() * textScale;
        float width = BlackOut.FONT.getWidth(text) * textScale > 250.0F ? BlackOut.FONT.getWidth(text) * textScale + 2.0F : 250.0F;
        this.stack.push();
        RenderUtils.unGuiScale(this.stack);
        if (customChat.blur.get()) {
            RenderUtils.drawLoadedBlur(
                    "hudblur",
                    this.stack,
                    renderer -> renderer.rounded(8.0F, BlackOut.mc.getWindow().getHeight() - (fontHeight + 14.0F), width, fontHeight + 4.0F, 6.0F, 10)
            );
            Renderer.onHUDBlur();
        }

        if (customChat.background.get()) {
            RenderUtils.rounded(
                    this.stack,
                    10.0F,
                    BlackOut.mc.getWindow().getHeight() - (fontHeight + 16.0F),
                    width,
                    fontHeight + 4.0F,
                    6.0F,
                    customChat.shadow.get() ? 6.0F : 0.0F,
                    customChat.bgColor.get().getRGB(),
                    customChat.shadowColor.get().getRGB()
            );
        }

        customChat.textColor.render(this.stack, text, textScale, 10.0F, BlackOut.mc.getWindow().getHeight() - (fontHeight + 13.0F), false, false);
        this.stack.pop();
    }
}
