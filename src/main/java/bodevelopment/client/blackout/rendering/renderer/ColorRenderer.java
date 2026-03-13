package bodevelopment.client.blackout.rendering.renderer;

import bodevelopment.client.blackout.rendering.shader.Shaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ColorRenderer extends Renderer {
    private static final ColorRenderer INSTANCE = new ColorRenderer();

    public static ColorRenderer getInstance() {
        return INSTANCE;
    }

    public static void renderRounded(PoseStack stack, float x, float y, float width, float height, float rad, int steps, float r, float g, float b, float a) {
        INSTANCE.startRender(stack, VertexFormat.Mode.TRIANGLE_FAN);
        INSTANCE.rounded(x, y, width, height, rad, steps, r, g, b, a);
        INSTANCE.endRender();
    }

    public static void renderFitRounded(PoseStack stack, float x, float y, float width, float height, float rad, int steps, float r, float g, float b, float a) {
        INSTANCE.startRender(stack, VertexFormat.Mode.TRIANGLE_FAN);
        INSTANCE.fitRounded(x, y, width, height, rad, steps, r, g, b, a);
        INSTANCE.endRender();
    }

    public static void renderCircle(PoseStack stack, float x, float y, float rad, int steps, float r, float g, float b, float a) {
        INSTANCE.startRender(stack, VertexFormat.Mode.TRIANGLE_FAN);
        INSTANCE.circle(x, y, 0.0F, r, g, b, a, rad, steps);
        INSTANCE.endRender();
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, int color) {
        this.quad(stack, x, y, w, h, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF);
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, float r, float g, float b, float a) {
        this.quad(stack, x, y, 0.0F, w, h, r, g, b, a);
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h) {
        this.quad(stack, x, y, z, w, h, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h, int color) {
        this.quad(stack, x, y, z, w, h, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF);
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        this.startRender(stack, VertexFormat.Mode.QUADS);
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

    public void startRender(PoseStack stack, VertexFormat.Mode drawMode) {
        this.startRender(stack, drawMode, DefaultVertexFormat.POSITION_COLOR);
    }

    public void startLines(PoseStack stack) {
        this.startRender(stack, VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    }

    private void startRender(PoseStack stack, VertexFormat.Mode drawMode, VertexFormat format) {
        this.renderMatrix = stack.last().pose();
        RenderSystem.enableBlend();
        this.renderBuffer = Tesselator.getInstance().begin(drawMode, format);
    }

    public void endRender() {
        Shaders.color.render(this.renderBuffer, null);
        RenderSystem.disableBlend();
    }
}
