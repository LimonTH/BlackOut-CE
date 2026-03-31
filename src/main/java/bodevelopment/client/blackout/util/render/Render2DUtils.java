package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.ShaderRenderer;
import bodevelopment.client.blackout.rendering.shader.Shader;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.function.Consumer;

public class Render2DUtils {
    public static final long initTime = System.currentTimeMillis();
    public static final PoseStack emptyStack = new PoseStack();

    /** Extra pixel padding around shader quads for anti-aliasing coverage */
    private static final float SHADER_QUAD_PADDING = 1.0F;
    private static final Matrix4f lastProjMat = new Matrix4f();
    private static final Matrix4f lastModelViewMat = new Matrix4f();
    private static Vec3 lastCamPos = Vec3.ZERO;

    private static final int SKEET_LIGHT = new Color(30, 30, 30, 255).getRGB();
    private static final int SKEET_MID = new Color(20, 20, 20, 255).getRGB();
    private static final int SKEET_BG = new Color(10, 10, 10, 255).getRGB();

    public static boolean insideRounded(double mx, double my, double x, double y, double width, double height, double rad) {
        double offsetX = mx - x;
        double offsetY = my - y;
        double dx = offsetX - Mth.clamp(offsetX, 0.0, width);
        double dy = offsetY - Mth.clamp(offsetY, 0.0, height);
        return dx * dx + dy * dy <= rad * rad;
    }

    public static void onRender() {
        Minecraft mc = BlackOut.mc;
        if (mc.level == null || mc.player == null) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        lastCamPos = camera.getPosition();
        lastModelViewMat.set(new Matrix4f().rotation(camera.rotation().conjugate()));
        float fov = mc.gameRenderer.getFov(camera, tickDelta, true);
        lastProjMat.set(mc.gameRenderer.getProjectionMatrix(fov));
    }

    public static Vec2 getCoords(double x, double y, double z, boolean checkVisible) {
        Minecraft mc = BlackOut.mc;
        Vector4f pos = new Vector4f(
                (float) (x - lastCamPos.x),
                (float) (y - lastCamPos.y),
                (float) (z - lastCamPos.z),
                1.0F
        );
        pos.mul(lastModelViewMat);
        pos.mul(lastProjMat);

        if (pos.w() <= 0.0f && checkVisible) {
            return null;
        }

        float w = Math.abs(pos.w());
        float ndcX = pos.x() / w;
        float ndcY = pos.y() / w;
        float screenX = (ndcX + 1.0F) * 0.5F * (float) mc.getWindow().getScreenWidth();
        float screenY = (1.0F - ndcY) * 0.5F * (float) mc.getWindow().getScreenHeight();
        return new Vec2(screenX, screenY);
    }

    public static void renderItem(PoseStack stack, ItemStack itemStack, float x, float y, float scale, float zOffset, boolean overlay) {
        if (itemStack.isEmpty()) return;

        Matrix4f matrix = stack.last().pose();
        float absX = matrix.get(3, 0) + x * matrix.get(0, 0);
        float absY = matrix.get(3, 1) + y * matrix.get(1, 1);
        float absZ = matrix.get(3, 2);
        float hudScale = matrix.get(0, 0);

        GuiGraphics context = new GuiGraphics(BlackOut.mc, BlackOut.mc.renderBuffers().bufferSource());
        context.pose().pushPose();

        float totalScale = hudScale * (scale / 16.0F);
        context.pose().translate(0, 0, absZ + zOffset);

        float scaledX = absX / totalScale;
        float scaledY = absY / totalScale;
        float offsetX = scaledX - (int) scaledX;
        float offsetY = scaledY - (int) scaledY;

        context.pose().scale(totalScale, totalScale, totalScale);
        context.pose().translate(offsetX, offsetY, 0);

        int drawX = (int) scaledX;
        int drawY = (int) scaledY;

        context.renderItem(itemStack, drawX, drawY);
        if (overlay) {
            context.renderItemDecorations(BlackOut.mc.font, itemStack, drawX, drawY);
        }

        context.pose().popPose();
    }

    public static void blurBufferBW(String name, int strength) {
        loadBlur(name, Managers.FRAME_BUFFER.getBuffer(name).getTexture(), strength, Shaders.bloomblur);
    }

    public static void blurBuffer(String name, int strength) {
        loadBlur(name, Managers.FRAME_BUFFER.getBuffer(name).getTexture(), strength, Shaders.screenblur);
    }

    public static void loadBlur(String name, int strength) {
        loadBlur(name, BlackOut.mc.getMainRenderTarget().getColorTextureId(), strength, Shaders.screenblur);
    }

    public static void loadBlur(String name, int from, int strength, Shader shader) {
        emptyStack.pushPose();
        unGuiScale(emptyStack);
        float alpha = Renderer.getAlpha();
        Renderer.setAlpha(1.0F);

        for (int dist = 1; dist <= strength; dist++) {
            FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer(dist == strength ? name : "screenblur" + dist);
            buffer.clear();
            buffer.bind(true);
            int tex;
            if (dist == 1) {
                tex = from;
            } else {
                tex = Managers.FRAME_BUFFER.getBuffer("screenblur" + (dist - 1)).getTexture();
            }

            drawBlur(tex, dist, shader);
        }

        Renderer.setAlpha(alpha);
        BlackOut.mc.getMainRenderTarget().bindWrite(true);
        emptyStack.popPose();
    }

    public static void drawLoadedBlur(String name, PoseStack stack, Consumer<ShaderRenderer> consumer) {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer(name);
        BlackOut.mc.getMainRenderTarget().bindWrite(true);
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(buffer.getTexture(), 0);

        try (RenderState state = renderer.begin(stack, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION, Shaders.screentex, new ShaderSetup(setup -> {
            setup.set("uTexture", 0);
            setup.set("alpha", 1.0F);
        }))) {
            consumer.accept(renderer);
        }
    }

    public static void renderBufferWith(String frameBuffer, Shader shader, ShaderSetup setup) {
        renderBufferWith(Managers.FRAME_BUFFER.getBuffer(frameBuffer), shader, setup);
    }

    public static void renderBufferWith(FrameBuffer frameBuffer, Shader shader, ShaderSetup setup) {
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(frameBuffer.getTexture(), 0);
        emptyStack.pushPose();
        unGuiScale(emptyStack);
        Renderer.setMatrices(emptyStack);
        float alpha = Renderer.getAlpha();
        Renderer.setAlpha(1.0F);
        try (RenderState state = renderer.begin(emptyStack, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION, shader, setup)) {
            renderer.quadShape(0.0F, 0.0F, BlackOut.mc.getWindow().getScreenWidth(), BlackOut.mc.getWindow().getScreenHeight());
        }
        Renderer.setAlpha(alpha);
        emptyStack.popPose();
    }

    public static void renderBufferOverlay(FrameBuffer frameBuffer, int id) {
        BlackOut.mc.getMainRenderTarget().bindWrite(true);
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(frameBuffer.getTexture(), 0);
        Renderer.setTexture(id, 1);

        emptyStack.pushPose();
        unGuiScale(emptyStack);
        Renderer.setMatrices(emptyStack);
        float alpha = Renderer.getAlpha();
        Renderer.setAlpha(1.0F);

        float w = (float) BlackOut.mc.getWindow().getScreenWidth();
        float h = (float) BlackOut.mc.getWindow().getScreenHeight();

        try (RenderState state = renderer.begin(emptyStack, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION, Shaders.screentexoverlay, new ShaderSetup(setup -> {
            setup.set("uTexture0", 0);
            setup.set("uTexture1", 1);
        }))) {
            renderer.vertex2D(0, h);
            renderer.vertex2D(w, h);
            renderer.vertex2D(w, 0);
            renderer.vertex2D(0, 0);
        }

        emptyStack.popPose();
    }

    public static void drawWithBlur(PoseStack stack, int strength, Consumer<ShaderRenderer> consumer) {
        loadBlur("temp", strength);
        drawLoadedBlur("temp", stack, consumer);
    }

    public static void blur(int strength, float alpha) {
        emptyStack.pushPose();
        unGuiScale(emptyStack);
        int prevBuffer = FrameBuffer.getCurrent();
        float prevAlpha = Renderer.getAlpha();
        Renderer.setAlpha(1.0F);

        for (int dist = 1; dist <= strength; dist++) {
            if (dist == strength) {
                FrameBuffer.bind(prevBuffer);
                Renderer.setAlpha(alpha);
            } else {
                Managers.FRAME_BUFFER.getBuffer("screenblur" + dist).bind(true);
            }

            int tex;
            if (dist == 1) {
                tex = BlackOut.mc.getMainRenderTarget().getColorTextureId();
            } else {
                tex = Managers.FRAME_BUFFER.getBuffer("screenblur" + (dist - 1)).getTexture();
            }

            drawBlur(tex, dist, Shaders.screenblur);
        }

        Renderer.setAlpha(prevAlpha);
        emptyStack.popPose();
    }

    private static void drawBlur(int from, int dist, Shader shader) {
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(from, 0);
        renderer.quad(
                emptyStack, 0.0F, 0.0F, BlackOut.mc.getWindow().getScreenWidth(), BlackOut.mc.getWindow().getScreenHeight(), shader, new ShaderSetup(setup -> {
                    setup.set("dist", getBlurDist(dist));
                    setup.set("uTexture", 0);
                }), DefaultVertexFormat.POSITION_COLOR
        );
        Renderer.setTexture(from, 0);
    }

    private static float getBlurDist(int i) {
        return 1.0F + Math.max(0.0F, i - 1.5F) * 2.0F;
    }

    public static void rounded(PoseStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor) {
        rounded(stack, x, y, w, h, radius, shadowRadius, color, shadowColor, RoundedSide.ALL);
    }

    public static void rounded(PoseStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor, RoundedSide side) {
        float ext = radius + shadowRadius;
        float innerX = x, innerY = y, innerW = w, innerH = h;
        float minX = x - ext, maxX = x + w + ext, minY = y - ext, maxY = y + h + ext;

        switch (side) {
            case RIGHT -> { minX = x; innerX = x - radius; innerW = w + radius; }
            case LEFT -> { maxX = x + w; innerW = w + radius; }
            case TOP -> { maxY = y + h; innerH = h + radius; }
            case BOTTOM -> { minY = y; innerY = y - radius; innerH = h + radius; }
            case BOTTOM_LEFT -> { minY = y - radius; innerY = y - radius; innerW = w + radius; innerH = h + radius; }
            default -> {}
        }

        innerRounded(stack, innerX, innerY, innerW, innerH, radius, shadowRadius, color, shadowColor, minX, maxX, minY, maxY);
    }

    private static void prepareAndRender(PoseStack stack, float shadowRadius, float minX, float maxX, float minY, float maxY, ShaderQuadRenderer renderer) {
        minX -= SHADER_QUAD_PADDING;
        maxX += SHADER_QUAD_PADDING;
        minY -= SHADER_QUAD_PADDING;
        maxY += SHADER_QUAD_PADDING;
        Renderer.setMatrices(stack);
        if (shadowRadius > 0.0F) {
            renderer.render(minX, maxX, minY, maxY, true);
        }
        renderer.render(minX, maxX, minY, maxY, false);
    }

    private static void renderShaderQuad(float minX, float maxX, float minY, float maxY, Shader shader, ShaderSetup setup) {
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        try (RenderState state = shaderRenderer.begin(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION, shader, setup)) {
            shaderRenderer.vertex2D(minX, maxY);
            shaderRenderer.vertex2D(maxX, maxY);
            shaderRenderer.vertex2D(maxX, minY);
            shaderRenderer.vertex2D(minX, minY);
        }
    }

    private static void innerRounded(
            PoseStack stack, float x, float y, float w, float h,
            float radius, float shadowRadius, int color, int shadowColor,
            float minX, float maxX, float minY, float maxY
    ) {
        prepareAndRender(stack, shadowRadius, minX, maxX, minY, maxY, (qMinX, qMaxX, qMinY, qMaxY, shadow) ->
                renderShaderQuad(qMinX, qMaxX, qMinY, qMaxY, shadow ? Shaders.roundedshadow : Shaders.rounded, new ShaderSetup(setup -> {
                    setup.set("rad", radius, shadowRadius);
                    if (shadow) {
                        setup.color("shadowClr", shadowColor);
                    } else {
                        setup.color("clr", color);
                    }
                    setup.set("pos", x, y, w, h);
                }))
        );
    }

    public static void fadeRounded(
            PoseStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int clr, int clr2, float frequency, float speed
    ) {
        float ext = radius + shadowRadius;
        prepareAndRender(stack, shadowRadius, x - ext, x + w + ext, y - ext, y + h + ext, (qMinX, qMaxX, qMinY, qMaxY, shadow) ->
                renderShaderQuad(qMinX, qMaxX, qMinY, qMaxY, shadow ? Shaders.shadowfade : Shaders.roundedfade, new ShaderSetup(setup -> {
                    setup.set("rad", radius, shadowRadius);
                    setup.color("clr", clr);
                    setup.color("clr2", clr2);
                    setup.set("pos", x, y, w, h);
                    setup.set("frequency", frequency * 2.0F);
                    setup.set("speed", speed);
                    setup.time(initTime);
                }))
        );
    }

    public static void rainbowRounded(
            PoseStack stack, float x, float y, float w, float h, float radius, float shadowRadius, float saturation, float frequency, float speed
    ) {
        float ext = radius + shadowRadius;
        prepareAndRender(stack, shadowRadius, x - ext, x + w + ext, y - ext, y + h + ext, (qMinX, qMaxX, qMinY, qMaxY, shadow) ->
                renderShaderQuad(qMinX, qMaxX, qMinY, qMaxY, shadow ? Shaders.shadowrainbow : Shaders.roundedrainbow, new ShaderSetup(setup -> {
                    setup.set("rad", radius, shadowRadius);
                    setup.set("pos", x, y, w, h);
                    setup.set("frequency", frequency);
                    setup.set("speed", speed);
                    setup.set("saturation", saturation);
                    setup.time(initTime);
                }))
        );
    }

    public static void tenaRounded(PoseStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int clr, int clr2, float speed) {
        float ext = radius + shadowRadius;
        prepareAndRender(stack, shadowRadius, x - ext, x + w + ext, y - ext, y + h + ext, (qMinX, qMaxX, qMinY, qMaxY, shadow) ->
                renderShaderQuad(qMinX, qMaxX, qMinY, qMaxY, shadow ? Shaders.tenacityshadow : Shaders.tenacity, new ShaderSetup(setup -> {
                    setup.set("rad", radius, shadowRadius);
                    setup.color("color1", clr);
                    setup.color("color2", clr2);
                    setup.set("pos", x, y, w, h);
                    setup.set("speed", speed);
                    setup.time(initTime);
                }))
        );
    }

    public static void bloom(PoseStack stack, float x, float y, float radX, float radY, int color) {
        Renderer.setMatrices(stack);
        renderShaderQuad(x - radX, x + radX, y - radY, y + radY, Shaders.bloom, new ShaderSetup(setup -> {
            setup.color("clr", color);
            setup.set("pos", x, y, radX, radY);
        }));
    }

    public static void roundedShadow(PoseStack stack, float x, float y, float w, float h, float radius, float shadowRad, int color) {
        rounded(stack, x, y, w, h, radius, shadowRad, MainMenu.EMPTY_COLOR, color);
    }

    public static void line(PoseStack stack, float x1, float y1, float x2, float y2, int color) {
        line(stack, x1, y1, x2, y2, color, color);
    }

    public static void line(PoseStack stack, float x1, float y1, float x2, float y2, int color, int color2) {
        Matrix4f matrix4f = stack.last().pose();
        float a1 = ARGB.alpha(color) / 255.0F;
        float r1 = ARGB.red(color) / 255.0F;
        float g1 = ARGB.green(color) / 255.0F;
        float b1 = ARGB.blue(color) / 255.0F;
        float a2 = ARGB.alpha(color2) / 255.0F;
        float r2 = ARGB.red(color2) / 255.0F;
        float g2 = ARGB.green(color2) / 255.0F;
        float b2 = ARGB.blue(color2) / 255.0F;

        try (RenderState state = RenderState.blend2D()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            bufferBuilder.addVertex(matrix4f, x1, y1, 0.0F).setColor(r1, g1, b1, a1);
            bufferBuilder.addVertex(matrix4f, x2, y2, 0.0F).setColor(r2, g2, b2, a2);

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
    }

    public static void line(PoseStack stack, float x1, float y1, float x2, float y2, int color, float width) {
        Matrix4f matrix4f = stack.last().pose();

        float a = ARGB.alpha(color) / 255.0F;
        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return;
        float nx = -dy / length * (width / 2.0F);
        float ny = dx / length * (width / 2.0F);

        try (RenderState state = RenderState.blend2D()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            bufferBuilder.addVertex(matrix4f, x1 - nx, y1 - ny, 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, x1 + nx, y1 + ny, 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, x2 + nx, y2 + ny, 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, x2 - nx, y2 - ny, 0.0F).setColor(r, g, b, a);

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
    }

    public static void fadeLine(PoseStack stack, float x1, float y1, float x2, float y2, int color) {
        Matrix4f matrix4f = stack.last().pose();
        float a = ARGB.alpha(color) / 255.0F;
        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;

        try (RenderState state = RenderState.blend2D()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            bufferBuilder.addVertex(matrix4f, x1, y1, 0.0F).setColor(r, g, b, 0.0F);
            bufferBuilder.addVertex(matrix4f, (float) Mth.lerp(0.4, x1, x2), (float) Mth.lerp(0.4, y1, y2), 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, (float) Mth.lerp(0.6, x1, x2), (float) Mth.lerp(0.6, y1, y2), 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, x2, y2, 0.0F).setColor(r, g, b, 0.0F);

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
    }

    public static void circle(PoseStack stack, float x, float y, float radius, int color) {
        Matrix4f matrix4f = stack.last().pose();
        float a = ARGB.alpha(color) / 255.0F;
        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;

        try (RenderState state = RenderState.blend2D()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bufferBuilder.addVertex(matrix4f, x, y, 0.0F).setColor(r, g, b, a);

            for (int i = 0; i <= 360; i++) {
                float rad = (float) Math.toRadians(i);
                bufferBuilder.addVertex(matrix4f, x + (float) Math.cos(rad) * radius, y + (float) Math.sin(rad) * radius, 0.0F)
                        .setColor(r, g, b, a);
            }

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
    }

    public static void quad(PoseStack stack, float x, float y, float w, float h, int color) {
        Matrix4f matrix4f = stack.last().pose();
        float a = ARGB.alpha(color) / 255.0F;
        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;

        try (RenderState state = RenderState.blend2D()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            bufferBuilder.addVertex(matrix4f, x, y + h, 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, x + w, y + h, 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, x + w, y, 0.0F).setColor(r, g, b, a);
            bufferBuilder.addVertex(matrix4f, x, y, 0.0F).setColor(r, g, b, a);

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
    }

    public static void shaderQuad(PoseStack stack, Shader shader, ShaderSetup setup, float x, float y, float w, float h) {
        Matrix4f matrix4f = stack.last().pose();

        RenderSystem.enableBlend();
        try {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            bufferBuilder.addVertex(matrix4f, x, y + h, 0.0F);
            bufferBuilder.addVertex(matrix4f, x + w, y + h, 0.0F);
            bufferBuilder.addVertex(matrix4f, x + w, y, 0.0F);
            bufferBuilder.addVertex(matrix4f, x, y, 0.0F);

            shader.render(bufferBuilder, setup);
        } finally {
            RenderSystem.disableBlend();
        }
    }

    public static void skeet(PoseStack stack, float x, float y, float w, float h, float saturation, float frequency, float speed) {
        float maxX = x + w;
        float maxY = y + h - 1.0F;
        moreSkeet(stack, x, y, w, h, saturation, frequency, speed, x, maxX, y, maxY);
    }

    private static void moreSkeet(
            PoseStack stack, float x, float y, float w, float h, float radius, float frequency, float speed, float minX, float maxX, float minY, float maxY
    ) {
        minX -= SHADER_QUAD_PADDING;
        maxX += SHADER_QUAD_PADDING;
        minY -= SHADER_QUAD_PADDING;
        maxY += SHADER_QUAD_PADDING;
        Renderer.setMatrices(stack);
        skeetSkeet(x, y, w, h, radius, frequency, speed, minX, maxX, minY, maxY);
    }

    private static void skeetSkeet(
            float x, float y, float w, float h, float saturation, float frequency, float speed, float minX, float maxX, float minY, float maxY
    ) {
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        try (RenderState state = shaderRenderer.begin(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION, Shaders.skeet, new ShaderSetup(setup -> {
            setup.set("frequency", frequency);
            setup.set("speed", speed);
            setup.set("saturation", saturation);
            setup.time(initTime);
        }))) {
            shaderRenderer.vertex2D(minX, maxY);
            shaderRenderer.vertex2D(maxX, maxY);
            shaderRenderer.vertex2D(maxX, minY);
            shaderRenderer.vertex2D(minX, minY);
        }
    }

    public static void drawSkeetBox(PoseStack stack, float x, float y, float width, float height, boolean drawLine) {
        quad(stack, x, y, width, height, SKEET_LIGHT);
        quad(stack, x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, SKEET_MID);
        quad(stack, x + 2.0F, y + 2.0F, width - 4.0F, height - 4.0F, SKEET_LIGHT);
        quad(stack, x + 3.0F, y + 3.0F, width - 6.0F, height - 6.0F, SKEET_BG);
        if (drawLine) {
            skeet(stack, x + 3.0F, y + 3.0F, width - 6.0F, 0.1F, 0.6F, 0.7F, 0.1F);
        }
    }

    public static void fade(PoseStack stack, float x, float y, float w, float h, int color, FadeSide side) {
        float a = ARGB.alpha(color) / 255.0F;
        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;

        float tl, bl, tr, br;
        switch (side) {
            case RIGHT -> { tl = a; bl = a; tr = 0.0F; br = 0.0F; }
            case LEFT -> { tl = 0.0F; bl = 0.0F; tr = a; br = a; }
            case TOP -> { tl = 0.0F; bl = a; tr = 0.0F; br = a; }
            case BOTTOM -> { tl = a; bl = 0.0F; tr = a; br = 0.0F; }
            default -> { tl = a; bl = a; tr = a; br = a; }
        }

        quad(stack, x, y, w, h,
                r, g, b, tl,
                r, g, b, bl,
                r, g, b, tr,
                r, g, b, br
        );
    }

    public static void quad(
            PoseStack stack,
            float x,
            float y,
            float w,
            float h,
            float tlr, float tlg, float tlb, float tla,
            float blr, float blg, float blb, float bla,
            float trr, float trg, float trb, float tra,
            float brr, float brg, float brb, float bra
    ) {
        Matrix4f matrix4f = stack.last().pose();

        try (RenderState state = RenderState.blend2D()) {
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            bufferBuilder.addVertex(matrix4f, x, y + h, 0.0F).setColor(blr, blg, blb, bla);
            bufferBuilder.addVertex(matrix4f, x + w, y + h, 0.0F).setColor(brr, brg, brb, bra);
            bufferBuilder.addVertex(matrix4f, x + w, y, 0.0F).setColor(trr, trg, trb, tra);
            bufferBuilder.addVertex(matrix4f, x, y, 0.0F).setColor(tlr, tlg, tlb, tla);

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
    }

    public static void startClickGui(PoseStack stack, float unscaled, float scale, float width, float height, float x, float y) {
        stack.pushPose();
        stack.scale(scale, scale, 1.0F);
        stack.translate(width / -2.0F, height / -2.0F, 0.0F);
        stack.translate((BlackOut.mc.getWindow().getScreenWidth() / 2.0 + x) / unscaled, (BlackOut.mc.getWindow().getScreenHeight() / 2.0 + y) / unscaled, 0.0);
    }

    public static void unGuiScale(PoseStack stack) {
        float scale = getScale();
        stack.scale(1.0F / scale, 1.0F / scale, 1.0F);
    }

    public static float getScale() {
        return (float) BlackOut.mc.getWindow().getGuiScale();
    }

    public enum RoundedSide {
        ALL, LEFT, RIGHT, TOP, BOTTOM, BOTTOM_LEFT
    }

    public enum FadeSide {
        RIGHT, LEFT, TOP, BOTTOM
    }

    @FunctionalInterface
    private interface ShaderQuadRenderer {
        void render(float minX, float maxX, float minY, float maxY, boolean shadow);
    }
}
