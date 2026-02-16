package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class WireframeRenderer extends WireframeContext {
    public static final MatrixStack matrixStack = new MatrixStack();
    public static final ModelVertexConsumerProvider provider = new ModelVertexConsumerProvider();
    public static boolean hidden = false;

    public static void renderModel(AbstractClientPlayerEntity player, BlackOutColor lineColor, BlackOutColor sideColor, RenderShape shape, float tickDelta) {
        matrixStack.push();
        Render3DUtils.start();
        provider.consumer.start();

        Render3DUtils.setRotation(matrixStack);
        drawEntity(player, tickDelta, provider);

        List<Vec3d[]> positions = provider.consumer.positions;
        if (shape.sides) {
            drawQuads(positions, sideColor.red / 255.0F, sideColor.green / 255.0F, sideColor.blue / 255.0F, sideColor.alpha / 255.0F);
        }

        if (shape.outlines) {
            drawLines(positions, lineColor.red / 255.0F, lineColor.green / 255.0F, lineColor.blue / 255.0F, lineColor.alpha / 255.0F);
        }

        Render3DUtils.end();
        matrixStack.pop();
    }

    public static void drawLines(List<Vec3d[]> positions, float red, float green, float blue, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(1.5F);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        MatrixStack.Entry entry = new MatrixStack().peek();
        Matrix4f identity = entry.getPositionMatrix();

        List<Vec3d[]> rendered = new ArrayList<>();
        positions.forEach(arr -> {
            for (int i = 0; i < 4; i++) {
                Vec3d[] line = new Vec3d[]{arr[i], arr[(i + 1) % 4]};
                if (!contains(rendered, line)) {
                    Vec3d diff = line[1].subtract(line[0]);
                    double len = diff.length();
                    if (len < 1e-6) len = 1.0;
                    float nx = (float) (diff.x / len);
                    float ny = (float) (diff.y / len);
                    float nz = (float) (diff.z / len);

                    builder.vertex(identity, (float) line[0].x, (float) line[0].y, (float) line[0].z)
                            .color(red, green, blue, alpha)
                            .normal(entry, nx, ny, nz);
                    builder.vertex(identity, (float) line[1].x, (float) line[1].y, (float) line[1].z)
                            .color(red, green, blue, alpha)
                            .normal(entry, nx, ny, nz);
                    rendered.add(line);
                }
            }
        });
        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawQuads(List<Vec3d[]> positions, float red, float green, float blue, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Matrix4f identity = new Matrix4f();

        positions.forEach(arr -> {
            for (Vec3d pos : arr) {
                builder.vertex(identity, (float) pos.x, (float) pos.y, (float) pos.z)
                        .color(red, green, blue, alpha);
            }
        });
        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void drawEntity(AbstractClientPlayerEntity player, float tickDelta, VertexConsumerProvider vertexConsumerProvider) {
        double d = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
        double e = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
        double f = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());
        float yaw = MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw());
        EntityRenderer<? super AbstractClientPlayerEntity> entityRenderer = BlackOut.mc.worldRenderer.entityRenderDispatcher.getRenderer(player);
        Vec3d cameraPos = BlackOut.mc.gameRenderer.getCamera().getPos();
        double x = d - cameraPos.x;
        double y = e - cameraPos.y;
        double z = f - cameraPos.z;
        Vec3d vec3d = entityRenderer.getPositionOffset(player, tickDelta);
        double d2 = x + vec3d.getX();
        double e2 = y + vec3d.getY();
        double f2 = z + vec3d.getZ();
        matrixStack.push();
        matrixStack.translate(d2, e2, f2);
        hidden = true;
        entityRenderer.render(player, yaw, tickDelta, matrixStack, vertexConsumerProvider, 69420);
        hidden = false;
        matrixStack.pop();
    }
}
