package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class WireframeRenderer extends WireframeContext {
    public static final ModelVertexConsumerProvider provider = new ModelVertexConsumerProvider();
    public static boolean hidden = false;

    public static void renderServerPlayer(PoseStack stack, AbstractClientPlayer player,
                                          ModelData data, BlackOutColor lineColor,
                                          BlackOutColor sideColor, RenderShape shape,
                                          float progress, double yOffset, float maxScale) {

        Render3DUtils.start();
        provider.consumer.start();

        PoseStack modelStack = new PoseStack();
        modelStack.setIdentity();

        data.scale = Mth.lerp(progress, 1.0F, maxScale);

        drawStaticPlayerModel(modelStack, player, data);

        List<Vec3[]> positions = provider.consumer.positions;

        stack.pushPose();

        stack.translate(0, yOffset * progress, 0);

        Matrix4f matrix = stack.last().pose();

        float alphaMult = 1.0F - progress;

        if (shape.sides) {
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            drawQuads(matrix, positions,
                    sideColor.red / 255.0F,
                    sideColor.green / 255.0F,
                    sideColor.blue / 255.0F,
                    (sideColor.alpha / 255.0F) * alphaMult);
        }

        if (shape.outlines) {
            RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
            drawLines(matrix, positions,
                    lineColor.red / 255.0F,
                    lineColor.green / 255.0F,
                    lineColor.blue / 255.0F,
                    (lineColor.alpha / 255.0F) * alphaMult);
        }

        stack.popPose();
        Render3DUtils.end();
    }

    public static void renderModel(PoseStack stack, AbstractClientPlayer player,
                                   ModelData data, BlackOutColor lineColor,
                                   BlackOutColor sideColor, RenderShape shape) {
        RenderSystem.enableDepthTest();
        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0f, -1.0f);
        Render3DUtils.start();
        provider.consumer.start();
        PoseStack modelStack = new PoseStack();
        modelStack.setIdentity();

        drawStaticPlayerModel(modelStack, player, data);
        List<Vec3[]> positions = provider.consumer.positions;
        Matrix4f matrix = stack.last().pose();
        if (shape.sides) {
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            drawQuads(matrix, positions, sideColor.red / 255.0F, sideColor.green / 255.0F, sideColor.blue / 255.0F, sideColor.alpha / 255.0F);
        }

        if (shape.outlines) {
            RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
            drawLines(matrix, positions, lineColor.red / 255.0F, lineColor.green / 255.0F, lineColor.blue / 255.0F, lineColor.alpha / 255.0F);
        }

        Render3DUtils.end();
        RenderSystem.disablePolygonOffset();
    }

    @SuppressWarnings("unchecked")
    private static void drawStaticPlayerModel(PoseStack stack, AbstractClientPlayer player, ModelData data) {
        EntityRenderer<? super AbstractClientPlayer, ?> entityRenderer =
                BlackOut.mc.getEntityRenderDispatcher().getRenderer(player);

        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer)) return;

        EntityModel<?> rawModel = livingRenderer.getModel();
        if (!(rawModel instanceof HumanoidModel<?>)) return;

        HumanoidModel<PlayerRenderState> model = (HumanoidModel<PlayerRenderState>) rawModel;

        PlayerRenderState state = new PlayerRenderState();
        state.isPassenger = data.riding;
        state.isBaby = false;
        state.attackTime = data.swingProgress;
        state.swinging = data.swingProgress > 0;

        state.capeFlap = data.limbPos;
        state.capeLean = data.limbSpeed;
        state.capeLean2 = data.animationProgress;

        state.yRot = data.headYaw;
        state.xRot = data.pitch;
        state.isDiscrete = player.isShiftKeyDown();
        state.isFallFlying = player.isFallFlying();
        state.isVisuallySwimming = player.isVisuallySwimming();
        state.skin = player.getSkin();

        stack.pushPose();

        float s = data.scale * 0.9375F;
        stack.scale(s, -s, -s);
        stack.translate(0.0F, -1.501F, 0.0F);

        if (data.sleeping && data.sleepDir != null) {
            stack.translate((float) (-data.sleepDir.getStepX()) * data.eyeHeight, 0.0F, (float) (-data.sleepDir.getStepZ()) * data.eyeHeight);
        }

        float bodyYaw = data.bodyYaw;
        if (data.hasVehicle) {
            float headYawWrap = Mth.wrapDegrees(data.headYaw - data.vehicleYaw);
            float clampedHead = Mth.clamp(headYawWrap, -85.0F, 85.0F);
            bodyYaw = data.headYaw - clampedHead;
            if (clampedHead * clampedHead > 2500.0F) bodyYaw += clampedHead * 0.2F;
        }

        stack.mulPose(Axis.YP.rotationDegrees(bodyYaw));

        if (data.flip) {
            stack.translate(0.0F, 2.125F, 0.0F);
            stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }

        float headYaw = data.headYaw - bodyYaw;
        float pitch = data.pitch;
        if (data.flip) { pitch *= -1.0F; headYaw *= -1.0F; }

        state.yRot = headYaw;
        state.xRot = pitch;

        model.setupAnim(state);

        hidden = true;

        model.renderToBuffer(stack, provider.getBuffer(null), 15728880, 0, -1);

        hidden = false;

        stack.popPose();
    }

    public static void drawLines(Matrix4f matrix, List<Vec3[]> positions, float red, float green, float blue, float alpha) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        positions.forEach(arr -> {
            for (int i = 0; i < 4; i++) {
                Vec3 p1 = arr[i];
                Vec3 p2 = arr[(i + 1) % 4];

                float dx = (float) (p2.x - p1.x);
                float dy = (float) (p2.y - p1.y);
                float dz = (float) (p2.z - p1.z);

                builder.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                        .setColor(red, green, blue, alpha)
                        .setNormal(dx, dy, dz);

                builder.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                        .setColor(red, green, blue, alpha)
                        .setNormal(-dx, -dy, -dz);
            }
        });
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public static void drawQuads(Matrix4f matrix, List<Vec3[]> positions, float red, float green, float blue, float alpha) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        positions.forEach(arr -> {
            for (Vec3 pos : arr) {
                builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(red, green, blue, alpha);
            }
        });
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public static class ModelData {
        public float scale = 1.0f;
        public boolean riding, flip, hasVehicle, sleeping;
        public float bodyYaw, headYaw, vehicleYaw, pitch, eyeHeight, animationProgress, leaningPitch, limbSpeed, limbPos, swingProgress;
        public Direction sleepDir;

        public ModelData(AbstractClientPlayer player, float tickDelta) {
            this.riding = player.isPassenger();
            this.bodyYaw = Mth.lerp(tickDelta, player.yBodyRotO, player.yBodyRot);
            this.headYaw = Mth.lerp(tickDelta, player.yHeadRotO, player.yHeadRot);
            if (player.getVehicle() instanceof LivingEntity living) {
                this.hasVehicle = true;
                this.vehicleYaw = Mth.rotLerp(tickDelta, living.yBodyRotO, living.yBodyRot);
            }
            this.flip = LivingEntityRenderer.isEntityUpsideDown(player);
            this.pitch = Mth.lerp(tickDelta, player.xRotO, player.getXRot());
            this.eyeHeight = player.getEyeHeight(Pose.STANDING) - 0.1F;
            this.animationProgress = player.tickCount + tickDelta;
            this.leaningPitch = player.getSwimAmount(tickDelta);
            this.limbSpeed = player.walkAnimation.speed(tickDelta);
            this.limbPos = player.walkAnimation.position(tickDelta);
            this.swingProgress = player.getAttackAnim(tickDelta);
            this.sleeping = player.hasPose(Pose.SLEEPING);
            this.sleepDir = player.getBedOrientation();
        }
    }
}