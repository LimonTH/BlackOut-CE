package bodevelopment.client.blackout.rendering.font;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.module.modules.misc.Streamer;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.shader.Shader;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.ColorUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13C;

import java.awt.*;

public class CustomFontRenderer {
    private final String name;
    private final long initTime = System.currentTimeMillis();
    public BOFont selectedFont;
    private double offset = 0.0;
    private float scale = 1.0F;

    public CustomFontRenderer(String name) {
        this.name = name;
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        GuiSettings settings = GuiSettings.getInstance();
        this.scale = settings.fontScale.get().floatValue();
        this.offset = (1.0F - this.scale) * 40.0F;
    }

    public void loadFont() {
        this.selectedFont = new BOFont(this.name, 64);
    }

    public float getScale() {
        return this.scale;
    }

    public float getWidth(String string) {
        Streamer streamer = Streamer.getInstance();
        if (streamer.enabled) {
            string = streamer.replace(string);
        }

        int sum = 0;

        for (int i = 0; i < string.length(); i++) {
            sum += this.selectedFont.map.getOrDefault((int) string.charAt(i), new CharInfo(0, 0, 0, 0)).width;
        }

        return sum / 8.0F * this.scale;
    }

    public float getHeight() {
        return 8.0F;
    }

    public void text(MatrixStack stack, String string, float s, float textX, float textY, Color color, boolean xCenter, boolean yCenter) {
        this.textInternal(stack, string, s, textX, textY, color.getRGB(), xCenter, yCenter, Shaders.font, new ShaderSetup());
    }

    public void text(MatrixStack stack, String string, float s, float textX, float textY, int color, boolean xCenter, boolean yCenter) {
        this.textInternal(stack, string, s, textX, textY, color, xCenter, yCenter, Shaders.font, new ShaderSetup());
    }

    public void text(
            MatrixStack stack, String string, float s, float textX, float textY, Color color, boolean xCenter, boolean yCenter, Shader shader, ShaderSetup setup
    ) {
        this.textInternal(stack, string, s, textX, textY, color.getRGB(), xCenter, yCenter, shader, setup);
    }

    public void text(
            MatrixStack stack, String string, float s, float textX, float textY, int color, boolean xCenter, boolean yCenter, Shader shader, ShaderSetup setup
    ) {
        this.textInternal(stack, string, s, textX, textY, color, xCenter, yCenter, shader, setup);
    }

    private void textInternal(
            MatrixStack stack, String string, float s, float textX, float textY, int color, boolean xCenter, boolean yCenter, Shader shader, ShaderSetup setup
    ) {
        stack.push();
        float d = 8.0F / this.getScale();
        float ds = s / d;
        stack.scale(ds, ds, 1.0F);
        float x = (textX / s - (xCenter ? this.getWidth(string) / 2.0F : 0.0F)) * d;
        float y = (textY / s - (yCenter ? this.getHeight() / 2.0F : 0.0F)) * d;
        this.string(string, stack, x, y, color, shader, setup);
        stack.pop();
    }

    public void string(String string, MatrixStack stack, float x, float y, int color) {
        this.renderString(string, stack, x, y, color, Shaders.font, new ShaderSetup());
    }

    public void string(String string, MatrixStack stack, float x, float y, int color, Shader shader, ShaderSetup setup) {
        this.renderString(string, stack, x, y, color, shader, setup);
    }

    private void renderString(String string, MatrixStack stack, float x, float y, int color, Shader shader, ShaderSetup setup) {
        this.innerRenderString(string, stack, x, y, ColorUtils.withAlpha(0, (int) ((color >>> 24) * 100.0F / 255.0F)), Shaders.fontshadow, setup);
        this.innerRenderString(string, stack, x, y, color, shader, setup);
    }

    private void innerRenderString(String string, MatrixStack stack, float x, float y, int color, Shader shader, ShaderSetup setup) {
        this.renderString(string, stack, x, y, shader, setup.append(s -> {
            if (shader == Shaders.fontshadow) {
                s.set("alphaMulti", (color >>> 24) / 255.0F);
            }

            s.colorIf("clr", color);
            s.set("uTexture", 0);
            s.setIf("texRes", this.selectedFont.getWidth(), this.selectedFont.getHeight());
            s.timeIf(this.initTime);
        }));
    }

    private void renderString(String string, MatrixStack stack, float x, float y, Shader shader, ShaderSetup setup) {
        Streamer streamer = Streamer.getInstance();
        if (streamer.enabled) {
            string = streamer.replace(string);
        }

        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        y = (float) (y - (this.selectedFont.getFontSize() * 0.4 - this.offset));
        RenderSystem.enableBlend();
        GL13C.glActiveTexture(33984);
        GL13C.glBindTexture(3553, this.selectedFont.getId());
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            x += this.renderChar(bufferBuilder, matrix4f, this.selectedFont.map.getOrDefault((int) ch, new CharInfo(0, 0, 0, 0)), x, y);
        }

        shader.render(bufferBuilder, setup);
        GL13C.glBindTexture(3553, GlStateManager.TEXTURES[0].boundTexture);
        GL13C.glActiveTexture(33984 | GlStateManager.activeTexture);
        RenderSystem.disableBlend();
    }

    private float renderChar(BufferBuilder bufferBuilder, Matrix4f matrix4f, CharInfo charInfo, float x, float y) {
        bufferBuilder.vertex(matrix4f, x + charInfo.width, y, 0.0F).texture(charInfo.tx + charInfo.tw, charInfo.ty - charInfo.th);
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).texture(charInfo.tx, charInfo.ty - charInfo.th);
        bufferBuilder.vertex(matrix4f, x, y + charInfo.height * 1.5F, 0.0F).texture(charInfo.tx, charInfo.ty + charInfo.th * 0.5F);
        bufferBuilder.vertex(matrix4f, x + charInfo.width, y + charInfo.height * 1.5F, 0.0F)
                .texture(charInfo.tx + charInfo.tw, charInfo.ty + charInfo.th * 0.5F);
        return charInfo.width;
    }
}
