package bodevelopment.client.blackout.rendering.renderer;

import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.shader.Shader;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;

import java.util.List;

public class ShaderRenderer extends Renderer {
    private static final ShaderRenderer INSTANCE = new ShaderRenderer();
    private boolean usingTexture = false;

    public static ShaderRenderer getInstance() {
        return INSTANCE;
    }

    public static void renderRounded(
            MatrixStack stack,
            float x,
            float y,
            float width,
            float height,
            float rad,
            int steps,
            float r,
            float g,
            float b,
            float a,
            Shader shader,
            ShaderSetup setup,
            VertexFormat format
    ) {
        INSTANCE.startRender(stack, r, g, b, a, VertexFormat.DrawMode.TRIANGLE_FAN, format);
        INSTANCE.rounded(x, y, width, height, rad, steps);
        INSTANCE.endRender(shader, setup);
    }

    public static void renderFitRounded(
            MatrixStack stack,
            float x,
            float y,
            float width,
            float height,
            float rad,
            int steps,
            float r,
            float g,
            float b,
            float a,
            Shader shader,
            ShaderSetup setup,
            VertexFormat format
    ) {
        INSTANCE.startRender(stack, r, g, b, a, VertexFormat.DrawMode.TRIANGLE_FAN, format);
        INSTANCE.fitRounded(x, y, width, height, rad, steps);
        INSTANCE.endRender(shader, setup);
    }

    public static void renderCircle(
            MatrixStack stack, float x, float y, float rad, int steps, float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        INSTANCE.startRender(stack, r, g, b, a, VertexFormat.DrawMode.TRIANGLE_FAN, format);
        INSTANCE.circle(x, y, rad, steps);
        INSTANCE.endRender(shader, setup);
    }

    public void quad(MatrixStack stack, float x, float y, float w, float h, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, w, h, 1.0F, 1.0F, 1.0F, 1.0F, shader, setup, format);
    }

    public void quad(MatrixStack stack, float x, float y, float w, float h, int color, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, w, h, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF, shader, setup, format);
    }

    public void quad(
            MatrixStack stack, float x, float y, float w, float h, float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        this.quad(stack, x, y, 0.0F, w, h, r, g, b, a, shader, setup, format);
    }

    public void quad(MatrixStack stack, float x, float y, float z, float w, float h, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, z, w, h, 1.0F, 1.0F, 1.0F, 1.0F, shader, setup, format);
    }

    public void quad(MatrixStack stack, float x, float y, float z, float w, float h, int color, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, z, w, h, color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF, shader, setup, format);
    }

    public void quad(
            MatrixStack stack, float x, float y, float z, float w, float h, float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        this.startRender(stack, r, g, b, a, VertexFormat.DrawMode.QUADS, format);
        List<String> attributes = format.getAttributeNames();
        if (attributes.contains("Color")) {
            this.vertex(x, y, z, r, g, b, a);
            this.vertex(x, y + h, z, r, g, b, a);
            this.vertex(x + w, y + h, z, r, g, b, a);
            this.vertex(x + w, y, z, r, g, b, a);
        } else {
            this.vertex(x, y, z);
            this.vertex(x, y + h, z);
            this.vertex(x + w, y + h, z);
            this.vertex(x + w, y, z);
        }

        this.endRender(shader, setup);
    }

    public void startColor(MatrixStack stack, float r, float g, float b, float a, VertexFormat.DrawMode drawMode) {
        this.startRender(stack, r, g, b, a, drawMode, VertexFormats.POSITION_COLOR);
    }

    public void startTexture(MatrixStack stack, float r, float g, float b, float a, VertexFormat.DrawMode drawMode, BOTextures.Texture texture) {
        GL13C.glActiveTexture(33984);
        GL11C.glBindTexture(3553, texture.getId());
        this.usingTexture = true;
        this.startRender(stack, r, g, b, a, drawMode, VertexFormats.POSITION_TEXTURE);
    }

    public void startRender(@Nullable MatrixStack stack, float r, float g, float b, float a, VertexFormat.DrawMode drawMode, VertexFormat format) {
        this.renderMatrix = stack == null ? null : stack.peek().getPositionMatrix();
        this.renderRed = r;
        this.renderGreen = g;
        this.renderBlue = b;
        this.renderAlpha = a;
        RenderSystem.enableBlend();
        this.renderBuffer = Tessellator.getInstance().begin(drawMode, format);
    }

    public void endRender(Shader shader, ShaderSetup setup) {
        if (this.usingTexture) {
            this.usingTexture = false;
            GL11C.glBindTexture(3553, 0);
            GL13C.glActiveTexture(33984);
        }

        if (shader != null) {
            shader.render(this.renderBuffer, setup);
        } else {
            BufferRenderer.draw(this.renderBuffer.end());
        }

        RenderSystem.disableBlend();
    }
}
