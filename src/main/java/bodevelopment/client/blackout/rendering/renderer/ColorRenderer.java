package bodevelopment.client.blackout.rendering.renderer;

import bodevelopment.client.blackout.rendering.shader.Shaders;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

public class ColorRenderer extends Renderer {
    private static final ColorRenderer INSTANCE = new ColorRenderer();

    public static ColorRenderer getInstance() {
        return INSTANCE;
    }

    public static void renderRounded(MatrixStack stack, float x, float y, float width, float height, float rad, int steps, float r, float g, float b, float a) {
        INSTANCE.startRender(stack, VertexFormat.DrawMode.TRIANGLE_FAN);
        INSTANCE.rounded(x, y, width, height, rad, steps, r, g, b, a);
        INSTANCE.endRender();
    }

    public static void renderFitRounded(MatrixStack stack, float x, float y, float width, float height, float rad, int steps, float r, float g, float b, float a) {
        INSTANCE.startRender(stack, VertexFormat.DrawMode.TRIANGLE_FAN);
        INSTANCE.fitRounded(x, y, width, height, rad, steps, r, g, b, a);
        INSTANCE.endRender();
    }

    public static void renderCircle(MatrixStack stack, float x, float y, float rad, int steps, float r, float g, float b, float a) {
        INSTANCE.startRender(stack, VertexFormat.DrawMode.TRIANGLE_FAN);
        INSTANCE.circle(x, y, 0.0F, r, g, b, a, rad, steps);
        INSTANCE.endRender();
    }

    public void quad(MatrixStack stack, float x, float y, float w, float h, int color) {
        this.quad(stack, x, y, w, h, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF);
    }

    public void quad(MatrixStack stack, float x, float y, float w, float h, float r, float g, float b, float a) {
        this.quad(stack, x, y, 0.0F, w, h, r, g, b, a);
    }

    public void quad(MatrixStack stack, float x, float y, float z, float w, float h) {
        this.quad(stack, x, y, z, w, h, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void quad(MatrixStack stack, float x, float y, float z, float w, float h, int color) {
        this.quad(stack, x, y, z, w, h, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF);
    }

    public void quad(MatrixStack stack, float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        this.startRender(stack, VertexFormat.DrawMode.QUADS);
        this.vertex(x, y, z, r, g, b, a);
        this.vertex(x, y + h, z, r, g, b, a);
        this.vertex(x + w, y + h, z, r, g, b, a);
        this.vertex(x + w, y, z, r, g, b, a);
        this.endRender();
    }

    public void quadOutlineShape(float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        this.vertex(x, y, z, r, g, b, a);
        this.vertex(x, y + h, z, r, g, b, a);
        this.vertex(x + w, y + h, z, r, g, b, a);
        this.vertex(x + w, y, z, r, g, b, a);
        this.vertex(x, y, z, r, g, b, a);
    }

    public void startRender(MatrixStack stack, VertexFormat.DrawMode drawMode) {
        this.startRender(stack, drawMode, VertexFormats.POSITION_COLOR);
    }

    public void startLines(MatrixStack stack) {
        this.startRender(stack, VertexFormat.DrawMode.LINES, VertexFormats.LINES);
    }

    private void startRender(MatrixStack stack, VertexFormat.DrawMode drawMode, VertexFormat format) {
        this.renderMatrix = stack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        this.renderBuffer = Tessellator.getInstance().begin(drawMode, format);
    }

    public void endRender() {
        Shaders.color.render(this.renderBuffer, null);
        RenderSystem.disableBlend();
    }
}
