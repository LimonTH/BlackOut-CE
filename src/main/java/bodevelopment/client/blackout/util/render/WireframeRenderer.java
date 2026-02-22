package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public class WireframeRenderer extends WireframeContext {
    public static final ModelVertexConsumerProvider provider = new ModelVertexConsumerProvider();
    public static boolean hidden = false;

    /**
     * Рендер для статических анимаций (PopChams, LogoutSpots) с поддержкой прогресса.
     */
    public static void renderServerPlayer(MatrixStack stack, AbstractClientPlayerEntity player,
                                          ModelData data, BlackOutColor lineColor,
                                          BlackOutColor sideColor, RenderShape shape,
                                          float progress, double yOffset, float maxScale) {

        Render3DUtils.start();
        provider.consumer.start();

        MatrixStack modelStack = new MatrixStack();
        modelStack.loadIdentity();

        data.scale = MathHelper.lerp(progress, 1.0F, maxScale);

        drawStaticPlayerModel(modelStack, player, data);

        List<Vec3d[]> positions = provider.consumer.positions;

        stack.push();

        stack.translate(0, yOffset * progress, 0);

        Matrix4f matrix = stack.peek().getPositionMatrix();

        float alphaMult = 1.0F - progress;

        if (shape.sides) {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            drawQuads(matrix, positions,
                    sideColor.red / 255.0F,
                    sideColor.green / 255.0F,
                    sideColor.blue / 255.0F,
                    (sideColor.alpha / 255.0F) * alphaMult);
        }

        if (shape.outlines) {
            RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
            drawLines(matrix, positions,
                    lineColor.red / 255.0F,
                    lineColor.green / 255.0F,
                    lineColor.blue / 255.0F,
                    (lineColor.alpha / 255.0F) * alphaMult);
        }

        stack.pop();
        Render3DUtils.end();
    }

    /**
     * Обычный рендер модели без прогресса и анимаций.
     */
    public static void renderModel(MatrixStack stack, AbstractClientPlayerEntity player,
                                   ModelData data, BlackOutColor lineColor,
                                   BlackOutColor sideColor, RenderShape shape) {

        Render3DUtils.start();
        provider.consumer.start();

        MatrixStack modelStack = new MatrixStack();
        modelStack.loadIdentity();

        drawStaticPlayerModel(modelStack, player, data);

        List<Vec3d[]> positions = provider.consumer.positions;
        Matrix4f matrix = stack.peek().getPositionMatrix();

        if (shape.sides) {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            drawQuads(matrix, positions, sideColor.red / 255.0F, sideColor.green / 255.0F, sideColor.blue / 255.0F, sideColor.alpha / 255.0F);
        }

        if (shape.outlines) {
            RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
            drawLines(matrix, positions, lineColor.red / 255.0F, lineColor.green / 255.0F, lineColor.blue / 255.0F, lineColor.alpha / 255.0F);
        }

        Render3DUtils.end();
    }

    @SuppressWarnings("unchecked")
    private static void drawStaticPlayerModel(MatrixStack stack, AbstractClientPlayerEntity player, ModelData data) {
        EntityRenderer<? super AbstractClientPlayerEntity> entityRenderer = BlackOut.mc.getEntityRenderDispatcher().getRenderer(player);
        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer)) return;

        EntityModel<?> rawModel = livingRenderer.getModel();
        if (!(rawModel instanceof BipedEntityModel<?>)) return;

        BipedEntityModel<AbstractClientPlayerEntity> model = (BipedEntityModel<AbstractClientPlayerEntity>) rawModel;

        stack.push();
        model.handSwingProgress = data.swingProgress;
        model.riding = data.riding;
        model.child = false;

        float s = data.scale * 0.9375F;
        stack.scale(s, -s, -s);
        stack.translate(0.0F, -1.501F, 0.0F);

        if (data.sleeping && data.sleepDir != null) {
            stack.translate((float) (-data.sleepDir.getOffsetX()) * data.eyeHeight, 0.0F, (float) (-data.sleepDir.getOffsetZ()) * data.eyeHeight);
        }

        float bodyYaw = data.bodyYaw;
        if (data.hasVehicle) {
            float headYawWrap = MathHelper.wrapDegrees(data.headYaw - data.vehicleYaw);
            float clampedHead = MathHelper.clamp(headYawWrap, -85.0F, 85.0F);
            bodyYaw = data.headYaw - clampedHead;
            if (clampedHead * clampedHead > 2500.0F) bodyYaw += clampedHead * 0.2F;
        }

        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));

        if (data.flip) {
            stack.translate(0.0F, 2.125F, 0.0F);
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        }

        float headYaw = data.headYaw - bodyYaw;
        float pitch = data.pitch;
        if (data.flip) { pitch *= -1.0F; headYaw *= -1.0F; }

        float limbPos = !data.hasVehicle ? data.limbPos : 0.0F;
        float limbSpeed = !data.hasVehicle ? Math.min(data.limbSpeed, 1.0F) : 0.0F;

        model.leaningPitch = data.leaningPitch;
        model.setAngles(player, limbPos, limbSpeed, data.animationProgress, headYaw, pitch);

        hidden = true;
        model.render(stack, provider.getBuffer(null), 69420, 0, 16777215);
        hidden = false;

        stack.pop();
    }

    public static void drawLines(Matrix4f matrix, List<Vec3d[]> positions, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        positions.forEach(arr -> {
            for (int i = 0; i < 4; i++) {
                Vec3d p1 = arr[i];
                Vec3d p2 = arr[(i + 1) % 4];
                builder.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(red, green, blue, alpha).normal(0, 1, 0);
                builder.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(red, green, blue, alpha).normal(0, 1, 0);
            }
        });
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static void drawQuads(Matrix4f matrix, List<Vec3d[]> positions, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        positions.forEach(arr -> {
            for (Vec3d pos : arr) {
                builder.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(red, green, blue, alpha);
            }
        });
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static class ModelData {
        public float scale = 1.0f;
        public boolean riding, flip, hasVehicle, sleeping;
        public float bodyYaw, headYaw, vehicleYaw, pitch, eyeHeight, animationProgress, leaningPitch, limbSpeed, limbPos, swingProgress;
        public Direction sleepDir;

        public ModelData(AbstractClientPlayerEntity player, float tickDelta) {
            this.riding = player.hasVehicle();
            this.bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
            this.headYaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
            if (player.getVehicle() instanceof LivingEntity living) {
                this.hasVehicle = true;
                this.vehicleYaw = MathHelper.lerpAngleDegrees(tickDelta, living.prevBodyYaw, living.bodyYaw);
            }
            this.flip = LivingEntityRenderer.shouldFlipUpsideDown(player);
            this.pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
            this.eyeHeight = player.getEyeHeight(EntityPose.STANDING) - 0.1F;
            this.animationProgress = player.age + tickDelta;
            this.leaningPitch = player.getLeaningPitch(tickDelta);
            this.limbSpeed = player.limbAnimator.getSpeed(tickDelta);
            this.limbPos = player.limbAnimator.getPos(tickDelta);
            this.swingProgress = player.getHandSwingProgress(tickDelta);
            this.sleeping = player.isInPose(EntityPose.SLEEPING);
            this.sleepDir = player.getSleepingDirection();
        }
    }
}