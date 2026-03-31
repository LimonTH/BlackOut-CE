package bodevelopment.client.blackout.rendering.renderer;

import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.util.render.RenderState;
import net.minecraft.util.ARGB;
import bodevelopment.client.blackout.rendering.shader.Shader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShaderRenderer extends Renderer {
    private static final ShaderRenderer INSTANCE = new ShaderRenderer();

    public static ShaderRenderer getInstance() {
        return INSTANCE;
    }

    public static void renderRounded(
            PoseStack stack, float x, float y, float width, float height, float rad, int steps,
            float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        try (RenderState state = INSTANCE.begin(stack, r, g, b, a, VertexFormat.Mode.TRIANGLE_FAN, format, shader, setup)) {
            INSTANCE.rounded(x, y, width, height, rad, steps);
        }
    }

    public static void renderFitRounded(
            PoseStack stack, float x, float y, float width, float height, float rad, int steps,
            float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        try (RenderState state = INSTANCE.begin(stack, r, g, b, a, VertexFormat.Mode.TRIANGLE_FAN, format, shader, setup)) {
            INSTANCE.fitRounded(x, y, width, height, rad, steps);
        }
    }

    public static void renderCircle(
            PoseStack stack, float x, float y, float rad, int steps, float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        try (RenderState state = INSTANCE.begin(stack, r, g, b, a, VertexFormat.Mode.TRIANGLE_FAN, format, shader, setup)) {
            INSTANCE.circle(x, y, rad, steps);
        }
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, w, h, 1.0F, 1.0F, 1.0F, 1.0F, shader, setup, format);
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, int color, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, w, h, ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color), shader, setup, format);
    }

    public void quad(
            PoseStack stack, float x, float y, float w, float h, float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        this.quad(stack, x, y, 0.0F, w, h, r, g, b, a, shader, setup, format);
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, z, w, h, 1.0F, 1.0F, 1.0F, 1.0F, shader, setup, format);
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h, int color, Shader shader, ShaderSetup setup, VertexFormat format) {
        this.quad(stack, x, y, z, w, h, ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color), shader, setup, format);
    }

    public void quad(
            PoseStack stack, float x, float y, float z, float w, float h, float r, float g, float b, float a, Shader shader, ShaderSetup setup, VertexFormat format
    ) {
        try (RenderState state = this.begin(stack, r, g, b, a, VertexFormat.Mode.QUADS, format, shader, setup)) {
            List<String> attributes = format.getElementAttributeNames();
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
        }
    }

    public RenderState begin(@Nullable PoseStack stack, float r, float g, float b, float a, VertexFormat.Mode drawMode, VertexFormat format, Shader shader, ShaderSetup setup) {
        this.renderMatrix = stack == null ? null : stack.last().pose();
        this.renderRed = r;
        this.renderGreen = g;
        this.renderBlue = b;
        this.renderAlpha = a;
        RenderSystem.enableBlend();
        this.renderBuffer = Tesselator.getInstance().begin(drawMode, format);
        return RenderState.of(() -> {
            if (shader != null) {
                shader.render(this.renderBuffer, setup);
            } else {
                BufferUploader.draw(this.renderBuffer.buildOrThrow());
            }
            RenderSystem.disableBlend();
        });
    }
}
