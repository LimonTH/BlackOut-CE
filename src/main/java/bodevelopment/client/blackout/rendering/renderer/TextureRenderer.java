package bodevelopment.client.blackout.rendering.renderer;

import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import net.minecraft.util.ARGB;
import bodevelopment.client.blackout.rendering.shader.Shader;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

public class TextureRenderer extends Renderer {
    private static final TextureRenderer INSTANCE = new TextureRenderer("rur");
    private final String name;
    private PoseStack renderMatrix = new PoseStack();
    private int id;
    private int width = 0;
    private int height = 0;
    private float renderX;
    private float renderY;
    private float renderW;
    private float renderH;
    private float renderBlur;
    private float renderRed;
    private float renderGreen;
    private float renderBlue;
    private float renderAlpha;
    private float u1;
    private float v1;
    private float u2;
    private float v2;

    public TextureRenderer(String name) {
        this.name = name;
    }

    public static TextureRenderer getInstance() {
        return INSTANCE;
    }

    public static void renderRounded(
            PoseStack stack, float x, float y, float width, float height, float u1, float v1, float u2, float v2, float rad, int steps, BOTextures.Texture texture
    ) {
        INSTANCE.setTexture(texture.getId());
        INSTANCE.startRender(
                stack, x - rad, y - rad, width + rad * 2.0F, height + rad * 2.0F, u1, v1, u2, v2, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.TRIANGLE_FAN
        );
        INSTANCE.rounded(x, y, width, height, rad, steps);
        INSTANCE.endRender();
    }

    public static void renderFitRounded(
            PoseStack stack, float x, float y, float width, float height, float u1, float v1, float u2, float v2, float rad, int steps, BOTextures.Texture texture
    ) {
        renderFitRounded(stack, x, y, width, height, u1, v1, u2, v2, rad, steps, texture.getId());
    }

    public static void renderFitRounded(
            PoseStack stack, float x, float y, float width, float height, float u1, float v1, float u2, float v2, float rad, int steps, int id
    ) {
        INSTANCE.setTexture(id);
        INSTANCE.startRender(stack, x, y, width, height, u1, v1, u2, v2, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.TRIANGLE_FAN);
        INSTANCE.fitRounded(x, y, width, height, rad, steps);
        INSTANCE.endRender();
    }

    public static void renderCircle(PoseStack stack, float x, float y, float u1, float v1, float u2, float v2, float rad, int steps, BOTextures.Texture texture) {
        renderCircle(stack, x, y, u1, v1, u2, v2, rad, steps, texture.getId());
    }

    public static void renderCircle(PoseStack stack, float x, float y, float u1, float v1, float u2, float v2, float rad, int steps, int id) {
        INSTANCE.setTexture(id);
        INSTANCE.startRender(stack, x - rad, y - rad, rad * 2.0F, rad * 2.0F, u1, v1, u2, v2, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.TRIANGLE_FAN);
        INSTANCE.circle(x, y, rad, steps);
        INSTANCE.endRender();
    }

    public static void renderQuad(PoseStack stack, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int id) {
        INSTANCE.setTexture(id);
        INSTANCE.quadUV(stack, x, y, width, height, u1, v1, u2, v2, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public String getName() {
        return this.name;
    }

    public void load(BOTextures.UploadData data) {
        this.id = data.id();
        this.width = data.width();
        this.height = data.height();
    }

    public void quad(PoseStack stack, float x, float y, float w, float h) {
        this.quad(stack, x, y, w, h, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, int color) {
        this.quad(stack, x, y, w, h, 0.0F, color);
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, float blur, int color) {
        this.quad(stack, x, y, w, h, blur, ARGB.red(color) / 255.0F, ARGB.green(color) / 255.0F, ARGB.blue(color) / 255.0F, ARGB.alpha(color) / 255.0F);
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, float blur, float r, float g, float b, float a) {
        this.quadUV(stack, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, blur, r, g, b, a);
    }

    public void quadUV(PoseStack stack, float x, float y, float w, float h, float u1, float v1, float u2, float v2) {
        this.quadUV(stack, x, y, w, h, u1, v1, u2, v2, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void quadUV(PoseStack stack, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int color) {
        this.quadUV(stack, x, y, w, h, u1, v1, u2, v2, 0.0F, color);
    }

    public void quadUV(PoseStack stack, float x, float y, float w, float h, float u1, float v1, float u2, float v2, float blur, int color) {
        this.quadUV(
                stack, x, y, w, h, u1, v1, u2, v2, blur, ARGB.red(color) / 255.0F, ARGB.green(color) / 255.0F, ARGB.blue(color) / 255.0F, ARGB.alpha(color) / 255.0F
        );
    }

    public void quadUV(
            PoseStack stack, float x, float y, float w, float h, float u1, float v1, float u2, float v2, float blur, float r, float g, float b, float a
    ) {
        this.startRender(stack, x, y, w, h, u1, v1, u2, v2, blur, r, g, b, a, VertexFormat.Mode.QUADS);
        this.vertex(x, y);
        this.vertex(x, y + h);
        this.vertex(x + w, y + h);
        this.vertex(x + w, y);
        this.endRender();
    }

    public void quadUV(
            PoseStack stack,
            float x,
            float y,
            float w,
            float h,
            float u1,
            float v1,
            float u2,
            float v2,
            float blur,
            float r,
            float g,
            float b,
            float a,
            Shader shader,
            ShaderSetup setup
    ) {
        this.startRender(stack, x, y, w, h, u1, v1, u2, v2, blur, r, g, b, a, VertexFormat.Mode.QUADS);
        this.vertex(x, y);
        this.vertex(x, y + h);
        this.vertex(x + w, y + h);
        this.vertex(x + w, y);
        this.endRender(shader, setup);
    }

    public void setTexture(int id) {
        this.id = id;
    }

    public void verticalFlip() {
        float v = this.v1;
        this.v1 = this.v2;
        this.v2 = v;
    }

    public void horizontalFlip() {
        float u = this.u1;
        this.u1 = this.u2;
        this.u2 = u;
    }

    public void startRender(PoseStack stack, float x, float y, float w, float h, float blur, float r, float g, float b, float a, VertexFormat.Mode drawMode) {
        this.startRender(stack, x, y, w, h, 0.0F, 0.0F, 1.0F, 1.0F, blur, r, g, b, a, drawMode);
    }

    public void startRender(
            PoseStack stack,
            float x,
            float y,
            float w,
            float h,
            float u1,
            float v1,
            float u2,
            float v2,
            float blur,
            float r,
            float g,
            float b,
            float a,
            VertexFormat.Mode drawMode
    ) {
        this.renderMatrix = stack;
        this.renderX = x;
        this.renderY = y;
        this.renderW = w;
        this.renderH = h;
        this.renderBlur = blur;
        this.renderRed = r;
        this.renderGreen = g;
        this.renderBlue = b;
        this.renderAlpha = a;
        this.u1 = u1;
        this.u2 = u2;
        this.v1 = v1;
        this.v2 = v2;
        RenderSystem.enableBlend();
        setTexture(this.id, 0);
        this.renderBuffer = Tesselator.getInstance().begin(drawMode, DefaultVertexFormat.POSITION);
    }

    @Override
    public void vertex(float x, float y) {
        this.renderBuffer.addVertex(x, y, 0.0F);
    }

    @Override
    public void vertex(float x, float y, float z) {
        this.renderBuffer.addVertex(x, y, z);
    }

    @Override
    public void vertex(float x, float y, float z, float r, float g, float b, float a) {
        this.renderBuffer.addVertex(x, y, z).setColor(r, g, b, a);
    }

    public void endRender() {
        setMatrices(this.renderMatrix);
        this.endRender(this.renderBlur > 0.0F ? Shaders.blurUV : Shaders.textureUV, new ShaderSetup(setup -> {
            if (this.renderBlur > 0.0F) {
                setup.set("blur", this.renderBlur);
            }

            setup.set("pos", this.renderX, this.renderY, this.renderX + this.renderW, this.renderY + this.renderH);
            setup.set("uv", this.u1, this.v1, this.u2, this.v2);
        }));
    }

    public void endRender(Shader shader, ShaderSetup setup) {
        shader.set("clr", this.renderRed, this.renderGreen, this.renderBlue, this.renderAlpha);
        shader.set("uTexture", 0);
        shader.render(this.renderBuffer, setup);
        RenderSystem.disableBlend();
    }
}
