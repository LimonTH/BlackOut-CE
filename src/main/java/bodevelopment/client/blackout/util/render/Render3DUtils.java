package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class Render3DUtils {
    private static final MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(256));
    public static PoseStack matrices = new PoseStack();

    public static void box(AABB box, BlackOutColor sideColor, BlackOutColor lineColor, RenderShape shape) {
        box(box, sideColor == null ? 0 : sideColor.getRGB(), lineColor == null ? 0 : lineColor.getRGB(), shape);
    }

    public static void box(AABB box, int sideColor, int lineColor, RenderShape shape) {
        matrices.pushPose();
        transformToCameraRotation(matrices);

        drawBoxRaw(matrices, box, sideColor, lineColor, shape);
        matrices.popPose();
    }

    public static void box(PoseStack stack, AABB box, BlackOutColor sideColor, BlackOutColor lineColor, RenderShape shape) {
        start();
        if (shape.sides && sideColor != null) {
            renderSides(stack, box, sideColor.getRGB());
        }
        if (shape.outlines && lineColor != null) {
            renderOutlines(stack, box, lineColor.getRGB());
        }
        end();
    }

    public static void drawBoxRaw(PoseStack stack, AABB box, int sideColor, int lineColor, RenderShape shape) {
        drawBoxRaw(stack, box, sideColor, lineColor, shape, true);
    }

    public static void drawBoxRaw(PoseStack stack, AABB box, int sideColor, int lineColor, RenderShape shape, boolean manageState) {
        if (manageState) {
            start();
        }
        stack.pushPose();

        if (shape.sides) {
            renderSides(stack, box, sideColor);
        }
        if (shape.outlines) {
            renderOutlines(stack, box, lineColor);
        }

        stack.popPose();

        if (manageState) {
            end();
        }
    }

    public static void boxEntity(PoseStack stack, AABB box, BlackOutColor sideColor, BlackOutColor lineColor, RenderShape shape) {
        start();
        if (shape.sides && sideColor != null) {
            renderEntitySides(stack, box, sideColor.getRGB());
        }
        if (shape.outlines && lineColor != null) {
            renderEntityOutlines(stack, box, lineColor.getRGB());
        }
        end();
    }

    public static void renderEntityOutlines(PoseStack stack, AABB box, int color) {
        RenderSystem.lineWidth(1.5F);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;
        float a = ARGB.alpha(color) / 255.0F;

        drawOutlines(stack, vertexConsumer,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                r, g, b, a);

        bufferSource.endBatch();
    }

    public static void renderEntitySides(PoseStack stack, AABB box, int color) {
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugQuads());

        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;
        float a = ARGB.alpha(color) / 255.0F;

        drawSides(stack, vertexConsumer,
                (float) box.minX, (float) box.minY, (float) box.minZ,
                (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                r, g, b, a);

        bufferSource.endBatch();
    }

    public static void renderOutlines(PoseStack stack, AABB box, int color) {
        Vec3 camPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();

        RenderSystem.lineWidth(1.5F);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        float minX = (float) (box.minX - camPos.x);
        float minY = (float) (box.minY - camPos.y);
        float minZ = (float) (box.minZ - camPos.z);
        float maxX = (float) (box.maxX - camPos.x);
        float maxY = (float) (box.maxY - camPos.y);
        float maxZ = (float) (box.maxZ - camPos.z);

        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;
        float a = ARGB.alpha(color) / 255.0F;

        drawOutlines(stack, vertexConsumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);

        bufferSource.endBatch();
    }

    public static void renderSides(PoseStack stack, AABB box, int color) {
        Vec3 camPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugQuads());

        float minX = (float) (box.minX - camPos.x);
        float minY = (float) (box.minY - camPos.y);
        float minZ = (float) (box.minZ - camPos.z);
        float maxX = (float) (box.maxX - camPos.x);
        float maxY = (float) (box.maxY - camPos.y);
        float maxZ = (float) (box.maxZ - camPos.z);

        float r = ARGB.red(color) / 255.0F;
        float g = ARGB.green(color) / 255.0F;
        float b = ARGB.blue(color) / 255.0F;
        float a = ARGB.alpha(color) / 255.0F;

        drawSides(stack, vertexConsumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, a);

        bufferSource.endBatch();
    }

    public static void drawOutlines(PoseStack stack, VertexConsumer vertexConsumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        PoseStack.Pose entry = stack.last();
        Matrix4f matrix = entry.pose();

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

    public static void drawSides(PoseStack stack, VertexConsumer vertexConsumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        Matrix4f matrix = stack.last().pose();

        vertexConsumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);

        vertexConsumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);

        vertexConsumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);

        vertexConsumer.addVertex(matrix, maxX, minY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, maxY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);

        vertexConsumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, minY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, maxX, maxY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);

        vertexConsumer.addVertex(matrix, minX, minY, minZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, minX, minY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, minX, maxY, maxZ).setColor(r, g, b, a);
        vertexConsumer.addVertex(matrix, minX, maxY, minZ).setColor(r, g, b, a);
    }

    public static void circle(PoseStack stack, Vec3 pos, double radius, int color, int angle, Orientation orientation) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        if (a <= 0.0F) return;
        Vec3 camPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();

        start();

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugLineStrip(1.5));

        Matrix4f matrix = stack.last().pose();

        float x = (float) (pos.x - camPos.x);
        float y = (float) (pos.y - camPos.y);
        float z = (float) (pos.z - camPos.z);

        for (int i = 0; i <= angle; i++) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad) * (float) radius;
            float sin = (float) Math.sin(rad) * (float) radius;

            switch (orientation) {
                case XY -> vertexConsumer.addVertex(matrix, x + cos, y + sin, z).setColor(r, g, b, a);
                case XZ -> vertexConsumer.addVertex(matrix, x + cos, y, z + sin).setColor(r, g, b, a);
                case YZ -> vertexConsumer.addVertex(matrix, x, y + cos, z + sin).setColor(r, g, b, a);
            }
        }

        bufferSource.endBatch();

        end();
    }

    public static void filledCircle(PoseStack stack, Vec3 pos, double radius, int color, int angle, Orientation orientation) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        if (a <= 0.0F) return;

        Vec3 camPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();

        start();

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugTriangleFan());

        Matrix4f matrix = stack.last().pose();

        float x = (float) (pos.x - camPos.x);
        float y = (float) (pos.y - camPos.y);
        float z = (float) (pos.z - camPos.z);

        vertexConsumer.addVertex(matrix, x, y, z).setColor(r, g, b, a);

        for (int i = 0; i <= angle; i += 10) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad) * (float) radius;
            float sin = (float) Math.sin(rad) * (float) radius;

            switch (orientation) {
                case XY -> vertexConsumer.addVertex(matrix, x + cos, y + sin, z).setColor(r, g, b, a);
                case XZ -> vertexConsumer.addVertex(matrix, x + cos, y, z + sin).setColor(r, g, b, a);
                case YZ -> vertexConsumer.addVertex(matrix, x, y + cos, z + sin).setColor(r, g, b, a);
            }
        }

        bufferSource.endBatch();

        end();
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
        vertexConsumer.addVertex(matrix4f, x1, y1, z1).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix4f, x2, y2, z2).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix4f, x3, y3, z3).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix4f, x4, y4, z4).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 0.0F);
    }

    private static void line(Matrix4f matrix, PoseStack.Pose entry, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len < 1e-6) len = 1.0F;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        vertexConsumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(entry, nx, ny, nz);
        vertexConsumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(entry, nx, ny, nz);
    }

    public static void start() {
        GlStateManager._enableBlend();
        GlStateManager.glBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager._disableCull();
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
    }

    public static void end() {
        GlStateManager._enableCull();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        GlStateManager._disableBlend();
    }

    public enum Orientation {
        XY,
        XZ,
        YZ
    }

    public static void transformToCameraRotation(PoseStack stack) {
        stack.mulPose(Axis.XP.rotationDegrees(BlackOut.mc.gameRenderer.getMainCamera().getXRot()));
        stack.mulPose(Axis.YP.rotationDegrees(BlackOut.mc.gameRenderer.getMainCamera().getYRot() + 180.0F));
    }

    public static void text(String string, Vec3 pos, int color, float scale) {
        text(matrices, string, pos, color, scale);
    }

    public static void text(PoseStack stack, String string, Vec3 pos, int color, float scale) {
        Camera camera = BlackOut.mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        stack.pushPose();

        Quaternionf invRotation = new Quaternionf(camera.rotation()).conjugate();
        stack.mulPose(invRotation);

        stack.translate((float)(pos.x - camPos.x), (float)(pos.y - camPos.y), (float)(pos.z - camPos.z));
        stack.translate(0, 0, RenderLayer.WORLD);

        stack.mulPose(camera.rotation());
        stack.mulPose(Axis.YP.rotationDegrees(180.0f));

        stack.scale(-scale * 0.25F, -scale * 0.25F, scale * 0.25F);

        start();

        BlackOut.FONT.text(stack, string, 1.0F, 0.0F, 0.0F, color, true, true);

        end();

        stack.popPose();
    }
}