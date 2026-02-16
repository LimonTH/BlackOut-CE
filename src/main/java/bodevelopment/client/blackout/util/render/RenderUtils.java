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
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.function.Consumer;

public class RenderUtils {
    public static final long initTime = System.currentTimeMillis();
    public static final MatrixStack emptyStack = new MatrixStack();
    private static VertexConsumerProvider.Immediate vertexConsumers = null;
    private static Matrix4f projMat;
    private static Matrix4f modelMat;

    public static boolean insideRounded(double mx, double my, double x, double y, double width, double height, double rad) {
        double offsetX = mx - x;
        double offsetY = my - y;
        double dx = offsetX - MathHelper.clamp(offsetX, 0.0, width);
        double dy = offsetY - MathHelper.clamp(offsetY, 0.0, height);
        return dx * dx + dy * dy <= rad * rad;
    }

    private static VertexConsumerProvider.Immediate getVertexConsumers() {
        if (vertexConsumers == null) {
            vertexConsumers = BlackOut.mc.gameRenderer.buffers.getEntityVertexConsumers();
        }

        return vertexConsumers;
    }

    public static void onRender() {
        projMat = RenderSystem.getProjectionMatrix();
        modelMat = RenderSystem.getModelViewMatrix();
    }

    public static Vec2f getCoords(double x, double y, double z, boolean checkVisible) {
        Matrix4f matrix4f = new Matrix4f();
        Camera camera = BlackOut.mc.gameRenderer.getCamera();
        matrix4f.rotate(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix4f.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrix4f.translate(
                (float) (x - camera.getPos().x), (float) (y - camera.getPos().y), (float) (z - camera.getPos().z)
        );
        Vector4f f = matrix4f.transform(new Vector4f(0.0F, 0.0F, 0.0F, 1.0F));
        Vector4f f2 = new Vector4f(f.x, f.y, f.z, 1.0F).mul(modelMat).mul(projMat);
        return f2.z < 0.0F && checkVisible
                ? null
                : new Vec2f(
                (float) ((f2.x / 2.0F / Math.abs(f2.z) + 0.5) * BlackOut.mc.getWindow().getWidth()),
                (float) ((1.0F - f2.y / 2.0F / Math.abs(f2.z) - 0.5) * BlackOut.mc.getWindow().getHeight())
        );
    }

    public static void renderItem(MatrixStack stack, Item item, float x, float y, float scale) {
        ItemStack itemStack = item.getDefaultStack();
        if (!itemStack.isEmpty()) {
            BakedModel bakedModel = BlackOut.mc.getItemRenderer().getModel(itemStack, BlackOut.mc.world, BlackOut.mc.player, 0);
            stack.push();
            stack.translate(x + 8.0F, y + 8.0F, 0.0F);
            stack.multiplyPositionMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
            stack.scale(scale, scale, scale);
            boolean bl = !bakedModel.isSideLit();
            if (bl) {
                DiffuseLighting.disableGuiDepthLighting();
            }

            BlackOut.mc
                    .getItemRenderer()
                    .renderItem(itemStack, ModelTransformationMode.GUI, false, stack, getVertexConsumers(), 15728880, OverlayTexture.DEFAULT_UV, bakedModel);
            getVertexConsumers().draw();
            if (bl) {
                DiffuseLighting.enableGuiDepthLighting();
            }

            stack.pop();
        }
    }

    public static void scissor(float x, float y, float w, float h) {
        // Получаем коэффициент масштабирования интерфейса (например, 2.0, 3.0)
        double scale = BlackOut.mc.getWindow().getScaleFactor();

        // Пересчитываем игровые координаты в реальные пиксели монитора
        int screenX = (int) (x * scale);
        int screenW = (int) (w * scale);
        int screenH = (int) (h * scale);

        // Инвертируем Y: берем полную высоту экрана в пикселях и вычитаем (y + h) * scale
        // Это переносит координату из системы "сверху-вниз" в "снизу-вверх"
        int screenY = (int) ((BlackOut.mc.getWindow().getScaledHeight() - (y + h)) * scale);

        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(screenX, screenY, Math.max(screenW, 0), Math.max(screenH, 0));
    }

    public static void endScissor() {
        GlStateManager._disableScissorTest();
    }

    public static void blurBufferBW(String name, int strength) {
        loadBlur(name, Managers.FRAME_BUFFER.getBuffer(name).getTexture(), strength, Shaders.bloomblur);
    }

    public static void blurBuffer(String name, int strength) {
        loadBlur(name, Managers.FRAME_BUFFER.getBuffer(name).getTexture(), strength, Shaders.screenblur);
    }

    public static void loadBlur(String name, int strength) {
        loadBlur(name, BlackOut.mc.getFramebuffer().getColorAttachment(), strength, Shaders.screenblur);
    }

    public static void loadBlur(String name, int from, int strength, Shader shader) {
        emptyStack.push();
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
        BlackOut.mc.getFramebuffer().beginWrite(true);
        emptyStack.pop();
    }

    public static void drawLoadedBlur(String name, MatrixStack stack, Consumer<ShaderRenderer> consumer) {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer(name);
        BlackOut.mc.getFramebuffer().beginWrite(true);
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(buffer.getTexture(), 0);

        // Переключаемся на QUADS для стабильности
        renderer.startRender(stack, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        consumer.accept(renderer);

        renderer.endRender(Shaders.screentex, new ShaderSetup(setup -> {
            setup.set("uTexture", 0);
            setup.set("alpha", 1.0F);
        }));
    }

    public static void renderBufferWith(String frameBuffer, Shader shader, ShaderSetup setup) {
        renderBufferWith(Managers.FRAME_BUFFER.getBuffer(frameBuffer), shader, setup);
    }

    public static void renderBufferWith(FrameBuffer frameBuffer, Shader shader, ShaderSetup setup) {
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(frameBuffer.getTexture(), 0);
        emptyStack.push();
        unGuiScale(emptyStack);
        renderer.startRender(emptyStack, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
        renderer.quadShape(0.0F, 0.0F, BlackOut.mc.getWindow().getWidth(), BlackOut.mc.getWindow().getHeight());
        renderer.endRender(shader, setup);
        emptyStack.pop();
    }

    public static void renderBufferOverlay(FrameBuffer frameBuffer, int id) {
        BlackOut.mc.getFramebuffer().beginWrite(true);
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(frameBuffer.getTexture(), 0);
        Renderer.setTexture(id, 1);

        emptyStack.push();
        unGuiScale(emptyStack);

        renderer.startRender(emptyStack, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        float w = (float) BlackOut.mc.getWindow().getWidth();
        float h = (float) BlackOut.mc.getWindow().getHeight();

        renderer.vertex2D(0, h);
        renderer.vertex2D(w, h);
        renderer.vertex2D(w, 0);
        renderer.vertex2D(0, 0);

        renderer.endRender(Shaders.screentexoverlay, new ShaderSetup(setup -> {
            setup.set("uTexture0", 0);
            setup.set("uTexture1", 1);
        }));

        emptyStack.pop();
    }

    public static void drawWithBlur(MatrixStack stack, int strength, Consumer<ShaderRenderer> consumer) {
        loadBlur("temp", strength);
        drawLoadedBlur("temp", stack, consumer);
    }

    public static void blur(int strength, float alpha) {
        emptyStack.push();
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
                tex = BlackOut.mc.getFramebuffer().getColorAttachment();
            } else {
                tex = Managers.FRAME_BUFFER.getBuffer("screenblur" + (dist - 1)).getTexture();
            }

            drawBlur(tex, dist, Shaders.screenblur);
        }

        Renderer.setAlpha(prevAlpha);
        emptyStack.pop();
    }

    private static void drawBlur(int from, int dist, Shader shader) {
        ShaderRenderer renderer = ShaderRenderer.getInstance();
        Renderer.setTexture(from, 0);
        renderer.quad(
                emptyStack, 0.0F, 0.0F, BlackOut.mc.getWindow().getWidth(), BlackOut.mc.getWindow().getHeight(), shader, new ShaderSetup(setup -> {
                    setup.set("dist", getBlurDist(dist));
                    setup.set("uTexture", 0);
                }), VertexFormats.POSITION_COLOR
        );
        Renderer.setTexture(from, 0);
    }

    private static float getBlurDist(int i) {
        return 1.0F + Math.max(0.0F, i - 1.5F) * 2.0F;
    }

    public static void roundedRight(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor) {
        float maxX = x + w + radius + shadowRadius;
        float minY = y - radius - shadowRadius;
        float maxY = y + h + radius + shadowRadius;
        innerRounded(stack, x - radius, y, w + radius, h, radius, shadowRadius, color, shadowColor, x, maxX, minY, maxY);
    }

    public static void roundedLeft(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w;
        float minY = y - radius - shadowRadius;
        float maxY = y + h + radius + shadowRadius;
        innerRounded(stack, x, y, w + radius, h, radius, shadowRadius, color, shadowColor, minX, maxX, minY, maxY);
    }

    public static void roundedTop(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w + radius + shadowRadius;
        float minY = y - radius - shadowRadius;
        float maxY = y + h;
        innerRounded(stack, x, y, w, h + radius, radius, shadowRadius, color, shadowColor, minX, maxX, minY, maxY);
    }

    public static void roundedBottom(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w + radius + shadowRadius;
        float maxY = y + h + radius + shadowRadius;
        innerRounded(stack, x, y - radius, w, h + radius, radius, shadowRadius, color, shadowColor, minX, maxX, y, maxY);
    }

    public static void roundedBottomLeft(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w + radius + shadowRadius;
        float minY = y - radius;
        float maxY = y + h + radius + shadowRadius;
        innerRounded(stack, x, y - radius, w + radius, h + radius, radius, shadowRadius, color, shadowColor, minX, maxX, minY, maxY);
    }

    public static void rounded(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int color, int shadowColor) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w + radius + shadowRadius;
        float minY = y - radius - shadowRadius;
        float maxY = y + h + radius + shadowRadius;
        innerRounded(stack, x, y, w, h, radius, shadowRadius, color, shadowColor, minX, maxX, minY, maxY);
    }

    private static void innerRounded(
            MatrixStack stack,
            float x,
            float y,
            float w,
            float h,
            float radius,
            float shadowRadius,
            int color,
            int shadowColor,
            float minX,
            float maxX,
            float minY,
            float maxY
    ) {
        minX--;
        maxX++;
        minY--;
        maxY++;
        Renderer.setMatrices(stack);
        if (shadowRadius > 0.0F) {
            moreInnerRounded(x, y, w, h, radius, shadowRadius, color, shadowColor, minX, maxX, minY, maxY, true);
        }

        moreInnerRounded(x, y, w, h, radius, shadowRadius, color, shadowColor, minX, maxX, minY, maxY, false);
    }

    private static void moreInnerRounded(
            float x,
            float y,
            float w,
            float h,
            float radius,
            float shadowRadius,
            int color,
            int shadowColor,
            float minX,
            float maxX,
            float minY,
            float maxY,
            boolean shadow
    ) {
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        // Начинаем отрисовку полигона
        shaderRenderer.startRender(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        // ПРАВИЛЬНЫЙ ПОРЯДОК:
        shaderRenderer.vertex2D(minX, maxY); // 1. Нижний левый
        shaderRenderer.vertex2D(maxX, maxY); // 2. Нижний правый
        shaderRenderer.vertex2D(maxX, minY); // 3. Верхний правый
        shaderRenderer.vertex2D(minX, minY); // 4. Верхний левый

        shaderRenderer.endRender(shadow ? Shaders.roundedshadow : Shaders.rounded, new ShaderSetup(setup -> {
            setup.set("rad", radius, shadowRadius);
            if (shadow) {
                setup.color("shadowClr", shadowColor);
            } else {
                setup.color("clr", color);
            }
            setup.set("pos", x, y, w, h);
        }));
    }

    public static void fadeRounded(
            MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int clr, int clr2, float frequency, float speed
    ) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w + radius + shadowRadius;
        float minY = y - radius - shadowRadius;
        float maxY = y + h + radius + shadowRadius;
        fadeRounded(stack, x, y, w, h, radius, shadowRadius, clr, clr2, frequency, speed, minX, maxX, minY, maxY);
    }

    private static void fadeRounded(
            MatrixStack stack,
            float x,
            float y,
            float w,
            float h,
            float radius,
            float shadowRadius,
            int clr,
            int clr2,
            float frequency,
            float speed,
            float minX,
            float maxX,
            float minY,
            float maxY
    ) {
        minX--;
        maxX++;
        minY--;
        maxY++;
        Renderer.setMatrices(stack);
        if (shadowRadius > 0.0F) {
            moreFadeRounded(x, y, w, h, radius, shadowRadius, clr, clr2, frequency, speed, minX, maxX, minY, maxY, true);
        }

        moreFadeRounded(x, y, w, h, radius, shadowRadius, clr, clr2, frequency, speed, minX, maxX, minY, maxY, false);
    }

    private static void moreFadeRounded(
            float x,
            float y,
            float w,
            float h,
            float radius,
            float shadowRadius,
            int clr,
            int clr2,
            float frequency,
            float speed,
            float minX,
            float maxX,
            float minY,
            float maxY,
            boolean shadow
    ) {
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        // Начинаем рендеринг квада
        shaderRenderer.startRender(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        // ПРАВИЛЬНЫЙ ПОРЯДОК (Низ-Лево -> Низ-Право -> Верх-Право -> Верх-Лево)
        shaderRenderer.vertex2D(minX, maxY); // 1. Нижний левый
        shaderRenderer.vertex2D(maxX, maxY); // 2. Нижний правый
        shaderRenderer.vertex2D(maxX, minY); // 3. Верхний правый
        shaderRenderer.vertex2D(minX, minY); // 4. Верхний левый

        shaderRenderer.endRender(shadow ? Shaders.shadowfade : Shaders.roundedfade, new ShaderSetup(setup -> {
            setup.set("rad", radius, shadowRadius);
            setup.color("clr", clr);
            setup.color("clr2", clr2);
            setup.set("pos", x, y, w, h);
            setup.set("frequency", frequency * 2.0F);
            setup.set("speed", speed);
            setup.time(initTime);
        }));
    }

    public static void rainbowRounded(
            MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, float saturation, float frequency, float speed
    ) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w + radius + shadowRadius;
        float minY = y - radius - shadowRadius;
        float maxY = y + h + radius + shadowRadius;
        rainbowRounded(stack, x, y, w, h, radius, shadowRadius, saturation, frequency, speed, minX, maxX, minY, maxY);
    }

    private static void rainbowRounded(
            MatrixStack stack,
            float x,
            float y,
            float w,
            float h,
            float radius,
            float shadowRadius,
            float saturation,
            float frequency,
            float speed,
            float minX,
            float maxX,
            float minY,
            float maxY
    ) {
        minX--;
        maxX++;
        minY--;
        maxY++;
        Renderer.setMatrices(stack);
        if (shadowRadius > 0.0F) {
            moreRainbowRounded(x, y, w, h, radius, shadowRadius, saturation, frequency, speed, minX, maxX, minY, maxY, true);
        }

        moreRainbowRounded(x, y, w, h, radius, shadowRadius, saturation, frequency, speed, minX, maxX, minY, maxY, false);
    }

    private static void moreRainbowRounded(
            float x,
            float y,
            float w,
            float h,
            float radius,
            float shadowRadius,
            float saturation,
            float frequency,
            float speed,
            float minX,
            float maxX,
            float minY,
            float maxY,
            boolean shadow
    ) {
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        // Используем VertexFormats.POSITION, так как цвета считает шейдер
        shaderRenderer.startRender(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        // ПРАВИЛЬНЫЙ ПОРЯДОК:
        shaderRenderer.vertex2D(minX, maxY); // 1. Нижний левый
        shaderRenderer.vertex2D(maxX, maxY); // 2. Нижний правый
        shaderRenderer.vertex2D(maxX, minY); // 3. Верхний правый
        shaderRenderer.vertex2D(minX, minY); // 4. Верхний левый

        shaderRenderer.endRender(shadow ? Shaders.shadowrainbow : Shaders.roundedrainbow, new ShaderSetup(setup -> {
            setup.set("rad", radius, shadowRadius);
            setup.set("pos", x, y, w, h);
            setup.set("frequency", frequency);
            setup.set("speed", speed);
            setup.set("saturation", saturation);
            setup.time(initTime);
        }));
    }

    public static void tenaRounded(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRadius, int clr, int clr2, float speed) {
        float minX = x - radius - shadowRadius;
        float maxX = x + w + radius + shadowRadius;
        float minY = y - radius - shadowRadius;
        float maxY = y + h + radius + shadowRadius;
        tenaRounded(stack, x, y, w, h, radius, shadowRadius, clr, clr2, speed, minX, maxX, minY, maxY);
    }

    private static void tenaRounded(
            MatrixStack stack,
            float x,
            float y,
            float w,
            float h,
            float radius,
            float shadowRadius,
            int clr,
            int clr2,
            float speed,
            float minX,
            float maxX,
            float minY,
            float maxY
    ) {
        minX--;
        maxX++;
        minY--;
        maxY++;
        Renderer.setMatrices(stack);
        if (shadowRadius > 0.0F) {
            moreTenaRounded(x, y, w, h, radius, shadowRadius, clr, clr2, speed, minX, maxX, minY, maxY, true);
        }

        moreTenaRounded(x, y, w, h, radius, shadowRadius, clr, clr2, speed, minX, maxX, minY, maxY, false);
    }

    private static void moreTenaRounded(
            float x, float y, float w, float h, float radius, float shadowRadius,
            int clr, int clr2, float speed, float minX, float maxX, float minY, float maxY,
            boolean shadow
    ) {
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        // Начинаем рендер
        shaderRenderer.startRender(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        // ПРАВИЛЬНЫЙ ПОРЯДОК ВЕРШИН (Низ-Лево -> Низ-Право -> Верх-Право -> Верх-Лево)
        shaderRenderer.vertex2D(minX, maxY); // 1. Низ-Лево
        shaderRenderer.vertex2D(maxX, maxY); // 2. Низ-Право
        shaderRenderer.vertex2D(maxX, minY); // 3. Верх-Право
        shaderRenderer.vertex2D(minX, minY); // 4. Верх-Лево

        shaderRenderer.endRender(shadow ? Shaders.tenacityshadow : Shaders.tenacity, new ShaderSetup(setup -> {
            setup.set("rad", radius, shadowRadius);
            setup.color("color1", clr);
            setup.color("color2", clr2);
            setup.set("pos", x, y, w, h);
            setup.set("speed", speed);
            setup.time(initTime);
        }));
    }

    public static void bloom(MatrixStack stack, float x, float y, float radX, float radY, int color) {
        Renderer.setMatrices(stack); // Устанавливаем матрицы ДО начала отрисовки вершин

        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        shaderRenderer.startRender(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        float minX = x - radX;
        float maxX = x + radX;
        float minY = y - radY;
        float maxY = y + radY;

        // ПРАВИЛЬНЫЙ ПОРЯДОК
        shaderRenderer.vertex2D(minX, maxY);
        shaderRenderer.vertex2D(maxX, maxY);
        shaderRenderer.vertex2D(maxX, minY);
        shaderRenderer.vertex2D(minX, minY);

        shaderRenderer.endRender(Shaders.bloom, new ShaderSetup(setup -> {
            setup.color("clr", color);
            setup.set("pos", x, y, radX, radY);
        }));
    }

    public static void rounded(
            MatrixStack stack,
            float x,
            float y,
            float w,
            float h,
            float radius,
            int p,
            int color,
            boolean topLeft,
            boolean topRight,
            boolean bottomLeft,
            boolean bottomRight
    ) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // Добавляем для правильной прозрачности
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        // 1. СТАВИМ ЦЕНТРАЛЬНУЮ ТОЧКУ (Центр веера)
        // Теперь все треугольники будут идти из центра к краям. Это 100% стабильность.
        bufferBuilder.vertex(matrix4f, x + w / 2.0F, y + h / 2.0F, 0.0F).color(r, g, b, a);

        // 2. Рисуем углы
        drawRounded(x, y, w, h, radius, p, r, g, b, a, bufferBuilder, matrix4f, topLeft, topRight, bottomLeft, bottomRight);

        // 3. Замыкаем веер (добавляем самую первую точку угла в конец)
        // Это нужно, чтобы между последним и первым углом не было "дырки"
        if (bottomRight) {
            float rad = (float) Math.toRadians(90);
            bufferBuilder.vertex(matrix4f, (float) (x + w + Math.cos(rad) * radius), (float) (y + h + Math.sin(rad) * radius), 0.0F).color(r, g, b, a);
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawRounded(
            float x, float y, float w, float h, float radius, int p,
            float r, float g, float b, float a,
            BufferBuilder bufferBuilder, Matrix4f matrix4f,
            boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight
    ) {
        // Важно соблюдать круговой обход (например: BR -> TR -> TL -> BL)

        if (bottomRight) {
            corner(x + w, y + h, radius, 90, p, r, g, b, a, bufferBuilder, matrix4f);
        } else {
            bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F).color(r, g, b, a);
        }

        if (topRight) {
            corner(x + w, y, radius, 360, p, r, g, b, a, bufferBuilder, matrix4f);
        } else {
            bufferBuilder.vertex(matrix4f, x + w, y, 0.0F).color(r, g, b, a);
        }

        if (topLeft) {
            corner(x, y, radius, 270, p, r, g, b, a, bufferBuilder, matrix4f);
        } else {
            bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(r, g, b, a);
        }

        if (bottomLeft) {
            corner(x, y + h, radius, 180, p, r, g, b, a, bufferBuilder, matrix4f);
        } else {
            bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).color(r, g, b, a);
        }
    }

    public static void roundedShadow(MatrixStack stack, float x, float y, float w, float h, float radius, float shadowRad, int color) {
        rounded(stack, x, y, w, h, radius, shadowRad, MainMenu.EMPTY_COLOR, color);
    }

    private static void renderCorner(
            float x, float y, float radius, int angle, float p, float r, float g, float b, float a, Tessellator tessellator, Matrix4f matrix4f
    ) {
        // Включаем шейдер и бленд перед отрисовкой, если это отдельный вызов
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();

        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        // Центральная точка угла
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(r, g, b, a);
        // Рисуем дугу
        corner(x, y, radius, angle, p, r, g, b, a, bufferBuilder, matrix4f);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void corner2(
            float x, float y, float radius, int angle, float p, float r, float g, float b, float a, BufferBuilder bufferBuilder, Matrix4f matrix4f
    ) {
        corner(x, y, radius, angle, p, r, g, b, a, bufferBuilder, matrix4f);
    }

    public static void corner(float x, float y, float radius, int angle, float p, float r, float g, float b, float a, BufferBuilder bufferBuilder, Matrix4f matrix4f) {
        // Используем обычный цикл for для точности
        for (int i = 0; i <= p; i++) {
            // Рассчитываем текущий угол от начального до (начальный - 90)
            float currentAngle = angle - (i * 90.0F / p);
            float rad = (float) Math.toRadians(currentAngle);

            bufferBuilder.vertex(matrix4f,
                    (float) (x + Math.cos(rad) * radius),
                    (float) (y + Math.sin(rad) * radius),
                    0.0F
            ).color(r, g, b, a);
        }
    }

    public static void line(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
        line(stack, x1, y1, x2, y2, color, color); // Просто вызываем метод с двумя цветами
    }

    public static void line(MatrixStack stack, float x1, float y1, float x2, float y2, int color, int color2) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a1 = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r1 = ColorHelper.Argb.getRed(color) / 255.0F;
        float g1 = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b1 = ColorHelper.Argb.getBlue(color) / 255.0F;
        float a2 = ColorHelper.Argb.getAlpha(color2) / 255.0F;
        float r2 = ColorHelper.Argb.getRed(color2) / 255.0F;
        float g2 = ColorHelper.Argb.getGreen(color2) / 255.0F;
        float b2 = ColorHelper.Argb.getBlue(color2) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(matrix4f, x1, y1, 0.0F).color(r1, g1, b1, a1);
        bufferBuilder.vertex(matrix4f, x2, y2, 0.0F).color(r2, g2, b2, a2);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void line(MatrixStack stack, float x1, float y1, float x2, float y2, int color, float width) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        // 1. Считаем направление линии (вектор)
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) return; // Чтобы не делить на ноль, если точки совпали

        // 2. Считаем "нормаль" — вектор, перпендикулярный линии, чтобы раздвинуть её в ширину
        float nx = -dy / length * (width / 2.0F);
        float ny = dx / length * (width / 2.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Строим 4 точки прямоугольника вокруг центральной оси линии
        bufferBuilder.vertex(matrix4f, x1 - nx, y1 - ny, 0.0F).color(r, g, b, a); // Левый низ
        bufferBuilder.vertex(matrix4f, x1 + nx, y1 + ny, 0.0F).color(r, g, b, a); // Левый верх
        bufferBuilder.vertex(matrix4f, x2 + nx, y2 + ny, 0.0F).color(r, g, b, a); // Правый верх
        bufferBuilder.vertex(matrix4f, x2 - nx, y2 - ny, 0.0F).color(r, g, b, a); // Правый низ

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void fadeLine(MatrixStack stack, float x1, float y1, float x2, float y2, int color) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(matrix4f, x1, y1, 0.0F).color(r, g, b, 0.0F); // Старт (прозрачно)
        bufferBuilder.vertex(matrix4f, (float) MathHelper.lerp(0.4, x1, x2), (float) MathHelper.lerp(0.4, y1, y2), 0.0F).color(r, g, b, a); // 40% (видно)
        bufferBuilder.vertex(matrix4f, (float) MathHelper.lerp(0.6, x1, x2), (float) MathHelper.lerp(0.6, y1, y2), 0.0F).color(r, g, b, a); // 60% (видно)
        bufferBuilder.vertex(matrix4f, x2, y2, 0.0F).color(r, g, b, 0.0F); // Конец (прозрачно)

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void circle(MatrixStack stack, float x, float y, float radius, int color) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        // ВАЖНО: Первая точка — центр круга
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(r, g, b, a);

        // Затем рисуем точки по окружности
        for (int i = 0; i <= 360; i++) {
            bufferBuilder.vertex(matrix4f, (float) (x + Math.cos(Math.toRadians(i)) * radius), (float) (y + Math.sin(Math.toRadians(i)) * radius), 0.0F)
                    .color(r, g, b, a)
                    ;
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void circle2(MatrixStack stack, float x, float y, float radius, int color) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = stack.peek().getPositionMatrix();
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;

        for (int i = 0; i <= 360; i++) {
            bufferBuilder.vertex(matrix, (float) (x + Math.cos(Math.toRadians(i)) * radius), (float) (y + Math.sin(Math.toRadians(i)) * radius), 0.0F)
                    .color(r, g, b, a)
                    ;
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void circle3(MatrixStack stack, float x, float y, float radius, int color, int angle) {
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = stack.peek().getPositionMatrix();
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;

        for (int i = 0; i <= angle; i++) {
            bufferBuilder.vertex(matrix, (float) (x + Math.cos(Math.toRadians(i)) * radius), (float) (y + Math.sin(Math.toRadians(i)) * radius), 0.0F)
                    .color(r, g, b, a)
                    ;
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void texturedQuad(Identifier identifier, MatrixStack stack, float x, float y, float w, float h, int color) {
        RenderSystem.setShaderTexture(0, identifier);
        // GlStateManager и bindTexture здесь лишние, setShaderTexture делает всё за них
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Используем PositionTexColor если хотим красить текстуру,
        // но раз в твоем буфере POSITION_TEXTURE, оставим его, добавив ShaderColor
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(r, g, b, a); // Вот так цвет применится к текстуре

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        // ПРАВИЛЬНЫЙ ПОРЯДОК + UV
        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).texture(0.0F, 1.0F);     // Низ-Лево
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F).texture(1.0F, 1.0F); // Низ-Право
        bufferBuilder.vertex(matrix4f, x + w, y, 0.0F).texture(1.0F, 0.0F);     // Верх-Право
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F);         // Верх-Лево

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f); // Сбрасываем цвет
        RenderSystem.disableBlend();
    }

    public static void drawTexture(MatrixStack stack, Identifier texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2) {
        int id = BlackOut.mc.getTextureManager().getTexture(texture).getGlId();
        drawTexturedQuad(stack, id, x1, x2, y1, y2, u1, u2, y1, y2);
    }

    public static void drawTexturedQuad(MatrixStack stack, int texture, float x1, float x2, float y1, float y2, float u1, float u2, float v1, float v2) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        // x1, y1 — верхний левый угол
        // x2, y2 — нижний правый угол
        bufferBuilder.vertex(matrix4f, x1, y2, 0.0F).texture(u1, v2); // Низ-Лево
        bufferBuilder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2); // Низ-Право
        bufferBuilder.vertex(matrix4f, x2, y1, 0.0F).texture(u2, v1); // Верх-Право
        bufferBuilder.vertex(matrix4f, x1, y1, 0.0F).texture(u1, v1); // Верх-Лево

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawFontQuad(MatrixStack stack, int texture, float x, float y, float w, float h) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        RenderSystem.setShaderTexture(0, texture);

        // Применяем наш стандартный обход
        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).texture(0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F).texture(1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w, y, 0.0F).texture(1.0F, 0.0F);
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F);

        Shaders.font.render(bufferBuilder, new ShaderSetup());
        RenderSystem.disableBlend();
    }

    public static void quad(MatrixStack stack, float x, float y, float w, float h, int color) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // Добавь это на всякий случай
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).color(r, g, b, a);         // 1. Нижний левый
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F).color(r, g, b, a);     // 2. Нижний правый
        bufferBuilder.vertex(matrix4f, x + w, y, 0.0F).color(r, g, b, a);         // 3. Верхний правый
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(r, g, b, a);             // 4. Верхний левый

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end()); // Используй drawWithGlobalProgram для новых версий
        RenderSystem.disableBlend();
    }

    public static void inQuadShadow(MatrixStack stack, float x, float y, float w, float h, float shadow, int color) {
        bottomFade(stack, x, y, w, shadow, color);
        topFade(stack, x, y + h - shadow, w, shadow, color);
        rightFade(stack, x, y, shadow, h, color);
        leftFade(stack, x + w - shadow, y, shadow, h, color);
    }

    public static void quad2(MatrixStack stack, float x, float y, float w, float h, int color) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        // Последовательно обходим по периметру:
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(r, g, b, a);         // Верх-Лево
        bufferBuilder.vertex(matrix4f, x + w, y, 0.0F).color(r, g, b, a);     // Верх-Право
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F).color(r, g, b, a); // Низ-Право
        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).color(r, g, b, a);     // Низ-Лево
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(r, g, b, a);         // Замыкаем в Верх-Лево

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void shaderQuad(MatrixStack stack, Shader shader, ShaderSetup setup, float x, float y, float w, float h) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        // ПРАВИЛЬНЫЙ ПОРЯДОК:
        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F);     // 1. Низ-Лево
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F); // 2. Низ-Право
        bufferBuilder.vertex(matrix4f, x + w, y, 0.0F);     // 3. Верх-Право
        bufferBuilder.vertex(matrix4f, x, y, 0.0F);         // 4. Верх-Лево

        shader.render(bufferBuilder, setup);
        RenderSystem.disableBlend();
    }

    public static void skeet(MatrixStack stack, float x, float y, float w, float h, float saturation, float frequency, float speed) {
        float maxX = x + w;
        float maxY = y + h - 1.0F;
        moreSkeet(stack, x, y, w, h, saturation, frequency, speed, x, maxX, y, maxY);
    }

    private static void moreSkeet(
            MatrixStack stack, float x, float y, float w, float h, float radius, float frequency, float speed, float minX, float maxX, float minY, float maxY
    ) {
        minX--;
        maxX++;
        minY--;
        maxY++;
        Renderer.setMatrices(stack);
        skeetSkeet(x, y, w, h, radius, frequency, speed, minX, maxX, minY, maxY);
    }

    private static void skeetSkeet(
            float x, float y, float w, float h, float saturation, float frequency, float speed, float minX, float maxX, float minY, float maxY
    ) {
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        // Мы передаем VertexFormats.POSITION, значит используем только координаты
        shaderRenderer.startRender(null, 1.0F, 1.0F, 1.0F, 1.0F, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        // ПРАВИЛЬНЫЙ ПОРЯДОК:
        shaderRenderer.vertex2D(minX, maxY); // 1. Нижний левый
        shaderRenderer.vertex2D(maxX, maxY); // 2. Нижний правый
        shaderRenderer.vertex2D(maxX, minY); // 3. Верхний правый
        shaderRenderer.vertex2D(minX, minY); // 4. Верхний левый

        shaderRenderer.endRender(Shaders.skeet, new ShaderSetup(setup -> {
            setup.set("frequency", frequency);
            setup.set("speed", speed);
            setup.set("saturation", saturation);
            // Добавь проверку initTime, чтобы анимация была плавной
            setup.time(initTime);
        }));
    }

    public static void drawSkeetBox(MatrixStack stack, float x, float y, float width, float height, boolean drawLine) {
        int skeetLight = new Color(30, 30, 30, 255).getRGB();
        int skeet = new Color(20, 20, 20, 255).getRGB();
        int skeetBG = new Color(10, 10, 10, 255).getRGB();
        quad(stack, x, y, width, height, skeetLight);
        quad(stack, x + 1.0F, y + 1.0F, width - 2.0F, height - 2.0F, skeet);
        quad(stack, x + 2.0F, y + 2.0F, width - 4.0F, height - 4.0F, skeetLight);
        quad(stack, x + 3.0F, y + 3.0F, width - 6.0F, height - 6.0F, skeetBG);
        if (drawLine) {
            skeet(stack, x + 3.0F, y + 3.0F, width - 6.0F, 0.1F, 0.6F, 0.7F, 0.1F);
        }
    }

    public static void rightFade(MatrixStack stack, float x, float y, float w, float h, int color) {
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;

        quad(stack, x, y, w, h,
                r, g, b, a,    // Top Left (Видно)
                r, g, b, a,    // Bottom Left (Видно)
                r, g, b, 0.0F, // Top Right (Прозрачно)
                r, g, b, 0.0F  // Bottom Right (Прозрачно)
        );
    }

    public static void leftFade(MatrixStack stack, float x, float y, float w, float h, int color) {
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;
        // Левые углы (TL, BL) - 0.0f альфа, правые (TR, BR) - полная альфа
        quad(stack, x, y, w, h,
                r, g, b, 0.0F, // TL
                r, g, b, 0.0F, // BL
                r, g, b, a,    // TR
                r, g, b, a     // BR
        );
    }

    public static void topFade(MatrixStack stack, float x, float y, float w, float h, int color) {
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;
        // Верхние (TL, TR) - 0.0f, Нижние (BL, BR) - полная альфа
        quad(stack, x, y, w, h,
                r, g, b, 0.0F, // TL
                r, g, b, a,    // BL
                r, g, b, 0.0F, // TR
                r, g, b, a     // BR
        );
    }

    public static void bottomFade(MatrixStack stack, float x, float y, float w, float h, int color) {
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;
        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;

        // Порядок в новом quad: TL, BL, TR, BR
        quad(stack, x, y, w, h,
                r, g, b, a,    // Top Left (Видно)
                r, g, b, 0.0F, // Bottom Left (Прозрачно)
                r, g, b, a,    // Top Right (Видно)
                r, g, b, 0.0F  // Bottom Right (Прозрачно)
        );
    }

    public static void quad(
            MatrixStack stack,
            float x,
            float y,
            float w,
            float h,
            float tlr, float tlg, float tlb, float tla, // Top Left
            float blr, float blg, float blb, float bla, // Bottom Left
            float trr, float trg, float trb, float tra, // Top Right
            float brr, float brg, float brb, float bra  // Bottom Right
    ) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // Важно для корректной прозрачности градиентов
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // ПРАВИЛЬНЫЙ ВАНИЛЬНЫЙ ПОРЯДОК ВЕРШИН
        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).color(blr, blg, blb, bla);         // Низ-Лево
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F).color(brr, brg, brb, bra);     // Низ-Право
        bufferBuilder.vertex(matrix4f, x + w, y, 0.0F).color(trr, trg, trb, tra);         // Верх-Право
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(tlr, tlg, tlb, tla);             // Верх-Лево

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void startClickGui(MatrixStack stack, float unscaled, float scale, float width, float height, float x, float y) {
        stack.push();
        stack.scale(scale, scale, 1.0F);
        stack.translate(width / -2.0F, height / -2.0F, 0.0F);
        stack.translate((BlackOut.mc.getWindow().getWidth() / 2.0 + x) / unscaled, (BlackOut.mc.getWindow().getHeight() / 2.0 + y) / unscaled, 0.0);
    }

    public static void unGuiScale(MatrixStack stack) {
        float scale = getScale();
        stack.scale(1.0F / scale, 1.0F / scale, 1.0F);
    }

    public static float getScale() {
        return (float) BlackOut.mc.getWindow().getScaleFactor();
    }
}
