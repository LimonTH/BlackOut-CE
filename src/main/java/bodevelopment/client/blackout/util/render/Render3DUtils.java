package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class Render3DUtils { // TODO: В одиночном мире рендер работает идеально, но при если игре на сервере было замечено странное поведение рендера (дёрганые элементы, пытающиеся прилепится к чему-либо, а также их части, по типу углов, скачущие на небольшие координаты туда сюда(в районе 0.1(x,y,z)). Ещё хочется сказать про невероятное дёрганье при движении этих рендерингуемых объектов
    public static MatrixStack matrices = new MatrixStack();

    public static void box(Box box, BlackOutColor sideColor, BlackOutColor lineColor, RenderShape shape) {
        box(box, sideColor == null ? 0 : sideColor.getRGB(), lineColor == null ? 0 : lineColor.getRGB(), shape);
    }

    public static void box(Box box, int sideColor, int lineColor, RenderShape shape) {
        Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();

        matrices.push();
        setRotation(matrices);

        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        drawBoxRaw(matrices, box, sideColor, lineColor, shape);

        matrices.pop();
    }

    public static void box(MatrixStack stack, Box box, BlackOutColor sideColor, BlackOutColor lineColor, RenderShape shape) {
        if (shape.sides && sideColor != null) {
            renderSides(stack, box, sideColor.getRGB());
        }
        if (shape.outlines && lineColor != null) {
            renderOutlines(stack, box, lineColor.getRGB());
        }
    }

    public static void drawBoxRaw(MatrixStack stack, Box box, int sideColor, int lineColor, RenderShape shape) {
        drawBoxRaw(stack, box, sideColor, lineColor, shape, true);
    }

    public static void drawBoxRaw(MatrixStack stack, Box box, int sideColor, int lineColor, RenderShape shape, boolean manageState) {
        if (manageState) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableDepthTest();
        }

        stack.push();

        if (shape.sides) {
            renderSides(stack, box, sideColor);
        }
        if (shape.outlines) {
            renderOutlines(stack, box, lineColor);
        }

        stack.pop();

        if (manageState) {
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    public static void renderOutlines(MatrixStack stack, Box box, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(1.5F);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(stack.peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;

        drawOutlines(new MatrixStack(), bufferBuilder,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawOutlines(MatrixStack stack, VertexConsumer vertexConsumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        MatrixStack.Entry entry = stack.peek();
        Matrix4f matrix = entry.getPositionMatrix();

        line(matrix, entry, vertexConsumer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(matrix, entry, vertexConsumer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
    }

    private static void line(Matrix4f matrix, MatrixStack.Entry entry, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len < 1e-6) len = 1.0F;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        vertexConsumer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(entry, nx, ny, nz);
        vertexConsumer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(entry, nx, ny, nz);
    }

    public static void renderSides(MatrixStack stack, Box box, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().mul(stack.peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float r = ColorHelper.Argb.getRed(color) / 255.0F;
        float g = ColorHelper.Argb.getGreen(color) / 255.0F;
        float b = ColorHelper.Argb.getBlue(color) / 255.0F;
        float a = ColorHelper.Argb.getAlpha(color) / 255.0F;

        drawSides(new MatrixStack(), bufferBuilder, (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawSides(MatrixStack stack, VertexConsumer vertexConsumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        Matrix4f matrix = stack.peek().getPositionMatrix();

        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);

        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);

        vertexConsumer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);

        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

        vertexConsumer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        vertexConsumer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
    }

    public static void drawPlane(
            Matrix4f matrix4f,
            VertexConsumer vertexConsumer,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            float r,
            float g,
            float b,
            float a
    ) {
        vertexConsumer.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).normal(0.0F, 0.0F, 0.0F);
        vertexConsumer.vertex(matrix4f, x2, y2, z2).color(r, g, b, a).normal(0.0F, 0.0F, 0.0F);
        vertexConsumer.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).normal(0.0F, 0.0F, 0.0F);
        vertexConsumer.vertex(matrix4f, x4, y4, z4).color(r, g, b, a).normal(0.0F, 0.0F, 0.0F);
    }

    public static void start() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void end() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void setRotation(MatrixStack stack) {
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(BlackOut.mc.gameRenderer.getCamera().getPitch()));
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(BlackOut.mc.gameRenderer.getCamera().getYaw() + 180.0F));
    }

    public static void text(String string, Vec3d pos, int color, float scale) {
        text(matrices, string, pos, color, scale);
    }

    public static void text(MatrixStack stack, String string, Vec3d pos, int color, float scale) {
        Camera camera = BlackOut.mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        stack.push();

        Quaternionf invRotation = new Quaternionf(camera.getRotation()).conjugate();
        stack.multiply(invRotation);

        stack.translate((float)(pos.x - camPos.x), (float)(pos.y - camPos.y), (float)(pos.z - camPos.z));

        stack.multiply(camera.getRotation());
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));

        stack.scale(-scale * 0.25F, -scale * 0.25F, scale * 0.25F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BlackOut.FONT.text(stack, string, 1.0F, 0.0F, 0.0F, color, true, true);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        stack.pop();
    }
}