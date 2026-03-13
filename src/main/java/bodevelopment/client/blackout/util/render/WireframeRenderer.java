package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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
        provider.consumer.fixRemaining();
        List<Vec3> vertices = provider.consumer.vertices;

        stack.pushPose();
        stack.translate(0, yOffset * progress, 0);
        Matrix4f matrix = stack.last().pose();
        float alphaMult = 1.0F - progress;

        if (shape.sides) {
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            drawEverything(matrix, vertices,
                    sideColor.red / 255.0F,
                    sideColor.green / 255.0F,
                    sideColor.blue / 255.0F,
                    (sideColor.alpha / 255.0F) * alphaMult);
        }

        if (shape.outlines) {
            RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
            drawLinesFromList(matrix, vertices,
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

        provider.consumer.start();
        PoseStack modelStack = new PoseStack();
        modelStack.setIdentity();

        drawStaticPlayerModel(modelStack, player, data);
        provider.consumer.fixRemaining();

        List<Vec3> vertices = provider.consumer.vertices;
        Matrix4f matrix = stack.last().pose();

        if (shape.sides) {
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            drawEverything(matrix, vertices, sideColor.red / 255.0F, sideColor.green / 255.0F, sideColor.blue / 255.0F, sideColor.alpha / 255.0F);
        }

        if (shape.outlines) {
            RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
            drawLinesFromList(matrix, vertices, lineColor.red / 255.0F, lineColor.green / 255.0F, lineColor.blue / 255.0F, lineColor.alpha / 255.0F);
        }
    }

    @SuppressWarnings("unchecked")
    private static void drawStaticPlayerModel(PoseStack stack, AbstractClientPlayer player, ModelData data) {
        EntityRenderer<? super AbstractClientPlayer, ?> entityRenderer =
                BlackOut.mc.getEntityRenderDispatcher().getRenderer(player);

        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer)) return;

        EntityModel<?> rawModel = livingRenderer.getModel();
        if (!(rawModel instanceof HumanoidModel<?>)) return;

        HumanoidModel<PlayerRenderState> model = (HumanoidModel<PlayerRenderState>) rawModel;
        // TODO: слепок не принимает анимации родителя, стоит как статуя
        PlayerRenderState state = new PlayerRenderState();
        state.isPassenger = data.riding;
        state.isBaby = false;
        state.attackTime = data.swingProgress;
        state.swinging = data.swingProgress > 0;
        state.isDiscrete = player.isShiftKeyDown();
        state.skin = player.getSkin();
        model.setupAnim(state);

        stack.pushPose();
        float s = data.scale * 0.9375F;
        stack.scale(s, -s, -s);
        stack.translate(0.0F, -1.501F, 0.0F);
        stack.mulPose(Axis.YP.rotationDegrees(data.bodyYaw));

        VertexConsumer consumer = provider.getBuffer(null);
        int light = 15728880;
        int overlay = 0;

        hidden = true;

        model.head.render(stack, consumer, light, overlay);
        provider.consumer.nextPart();

        model.body.render(stack, consumer, light, overlay);
        provider.consumer.nextPart();

        model.rightArm.render(stack, consumer, light, overlay);
        provider.consumer.nextPart();

        model.leftArm.render(stack, consumer, light, overlay);
        provider.consumer.nextPart();

        model.rightLeg.render(stack, consumer, light, overlay);
        provider.consumer.nextPart();

        model.leftLeg.render(stack, consumer, light, overlay);
        provider.consumer.nextPart();

        hidden = false;
        stack.popPose();
    }

    public static void drawEverything(Matrix4f matrix, List<Vec3> vertices, float red, float green, float blue, float alpha) {
        if (vertices == null || vertices.isEmpty() || alpha <= 0) return;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        for (Vec3 pos : vertices) {
            builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                    .setColor(red, green, blue, alpha);
        }
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    public static void drawLinesFromList(Matrix4f matrix, List<Vec3> vertices, float red, float green, float blue, float alpha) {
        if (vertices == null || vertices.size() < 2 || alpha <= 0) return;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        for (int i = 0; i < vertices.size() - 2; i += 3) {
            Vec3 v1 = vertices.get(i);
            Vec3 v2 = vertices.get(i + 1);
            Vec3 v3 = vertices.get(i + 2);

            addLine(builder, matrix, v1, v2, red, green, blue, alpha);
            addLine(builder, matrix, v2, v3, red, green, blue, alpha);
            addLine(builder, matrix, v3, v1, red, green, blue, alpha);
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void addLine(BufferBuilder builder, Matrix4f matrix, Vec3 p1, Vec3 p2, float r, float g, float b, float a) {
        float dx = (float) (p2.x - p1.x);
        float dy = (float) (p2.y - p1.y);
        float dz = (float) (p2.z - p1.z);

        builder.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a).setNormal(dx, dy, dz);
        builder.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a).setNormal(-dx, -dy, -dz);
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