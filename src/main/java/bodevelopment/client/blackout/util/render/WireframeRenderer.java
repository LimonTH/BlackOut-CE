package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

// TODO: Модели предметов и большинство вообщем-то слепков рендерятся не соблюдая своих граней, допустим предметы выглядят как квадраты, а не аккуратные контуры
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
            drawEverything(matrix, vertices,
                    sideColor.red / 255.0F,
                    sideColor.green / 255.0F,
                    sideColor.blue / 255.0F,
                    (sideColor.alpha / 255.0F) * alphaMult);
        }

        if (shape.outlines) {
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
        GlStateManager._enableDepthTest();

        provider.consumer.start();
        PoseStack modelStack = new PoseStack();
        modelStack.setIdentity();

        drawStaticPlayerModel(modelStack, player, data);
        provider.consumer.fixRemaining();

        List<Vec3> vertices = provider.consumer.vertices;
        Matrix4f matrix = stack.last().pose();

        if (shape.sides) {
            drawEverything(matrix, vertices, sideColor.red / 255.0F, sideColor.green / 255.0F, sideColor.blue / 255.0F, sideColor.alpha / 255.0F);
        }

        if (shape.outlines) {
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
        PlayerRenderState state = new PlayerRenderState();
        state.eyeHeight = data.eyeHeight;
        state.isDiscrete = player.isShiftKeyDown();
        state.ageInTicks = data.animationProgress;

        state.bodyRot = data.bodyYaw;
        state.yRot = data.headYaw - data.bodyYaw;;
        state.xRot = data.pitch;
        state.walkAnimationPos = data.limbPos;
        state.walkAnimationSpeed = data.limbSpeed;
        state.scale = data.scale;
        state.isUpsideDown = data.flip;
        state.bedOrientation = data.sleepDir;
        state.pose = data.sleeping ? Pose.SLEEPING : (player.isShiftKeyDown() ? Pose.CROUCHING : Pose.STANDING);

        state.mainArm = player.getMainArm();
        state.rightArmPose = getArmPose(player, InteractionHand.MAIN_HAND);
        state.leftArmPose = getArmPose(player, InteractionHand.OFF_HAND);
        state.isUsingItem = player.isUsingItem();
        state.useItemHand = player.getUsedItemHand();
        state.ticksUsingItem = player.getTicksUsingItem();

        model.setupAnim(state);

        state.isPassenger = data.riding;
        state.attackTime = data.swingProgress;
        state.swimAmount = data.leaningPitch;
        state.isCrouching = player.isShiftKeyDown();
        state.isVisuallySwimming = player.isVisuallySwimming();

        state.skin = player.getSkin();
        state.swinging = data.swingProgress > 0;
        state.showHat = true;
        state.showJacket = true;
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

    private static final MultiBufferSource.BufferSource wireframeBufSrc = MultiBufferSource.immediate(new ByteBufferBuilder(256));

    public static void drawEverything(Matrix4f matrix, List<Vec3> vertices, float red, float green, float blue, float alpha) {
        if (vertices == null || vertices.isEmpty() || alpha <= 0) return;

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        GlStateManager._disableDepthTest();
        GlStateManager._disableCull();

        VertexConsumer builder = wireframeBufSrc.getBuffer(RenderType.debugTriangleFan());

        for (Vec3 pos : vertices) {
            builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                    .setColor(red, green, blue, alpha);
        }

        wireframeBufSrc.endBatch();
        GlStateManager._disablePolygonOffset();
        GlStateManager._enableCull();
        GlStateManager._enableDepthTest();
    }

    private static boolean isSame(Vec3 v1, Vec3 v2) {
        return v1.distanceToSqr(v2) < 0.00001;
    }

    public static void drawLinesFromList(Matrix4f matrix, List<Vec3> vertices, float red, float green, float blue, float alpha) {
        if (vertices == null || vertices.size() < 6 || alpha <= 0) return;

        GlStateManager._disableDepthTest();
        GlStateManager._disableCull();

        VertexConsumer builder = wireframeBufSrc.getBuffer(RenderType.lines());

        for (int i = 0; i < vertices.size() - 5; i += 6) {
            Vec3 v0 = vertices.get(i);
            Vec3 v1 = vertices.get(i + 1);
            Vec3 v2 = vertices.get(i + 2);

            Vec3 v3 = null;
            for (int j = 3; j < 6; j++) {
                Vec3 candidate = vertices.get(i + j);
                if (!isSame(candidate, v0) && !isSame(candidate, v1) && !isSame(candidate, v2)) {
                    v3 = candidate;
                    break;
                }
            }

            if (v3 != null) {
                addLine(builder, matrix, v0, v1, red, green, blue, alpha);
                addLine(builder, matrix, v1, v2, red, green, blue, alpha);
                addLine(builder, matrix, v2, v3, red, green, blue, alpha);
                addLine(builder, matrix, v3, v0, red, green, blue, alpha);
            } else {
                drawTriangleLines(builder, matrix, v0, v1, v2, red, green, blue, alpha);
            }
        }

        wireframeBufSrc.endBatch();
        GlStateManager._disablePolygonOffset();
        GlStateManager._enableCull();
        GlStateManager._enableDepthTest();
    }

    private static void drawTriangleLines(VertexConsumer builder, Matrix4f matrix, Vec3 v0, Vec3 v1, Vec3 v2, float r, float g, float b, float a) {
        if (v0.distanceTo(v1) < 0.001 || v1.distanceTo(v2) < 0.001 || v2.distanceTo(v0) < 0.001) return;

        addLine(builder, matrix, v0, v1, r, g, b, a);
        addLine(builder, matrix, v1, v2, r, g, b, a);
        addLine(builder, matrix, v2, v0, r, g, b, a);
    }

    private static void addLine(VertexConsumer builder, Matrix4f matrix, Vec3 p1, Vec3 p2, float r, float g, float b, float a) {
        float dx = (float) (p2.x - p1.x);
        float dy = (float) (p2.y - p1.y);
        float dz = (float) (p2.z - p1.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len > 0.0001F) {
            dx /= len; dy /= len; dz /= len;
        } else {
            dy = 1.0f;
        }

        builder.addVertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a).setNormal(dx, dy, dz);
        builder.addVertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a).setNormal(dx, dy, dz);
    }

    private static HumanoidModel.ArmPose getArmPose(AbstractClientPlayer player, InteractionHand hand) {
        ItemStack stack = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            return switch (stack.getUseAnimation()) {
                case BLOCK -> HumanoidModel.ArmPose.BLOCK;
                case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
                case SPEAR -> HumanoidModel.ArmPose.THROW_SPEAR;
                case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
                case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
                case BRUSH -> HumanoidModel.ArmPose.BRUSH;
                default -> HumanoidModel.ArmPose.ITEM;
            };
        }
        if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return HumanoidModel.ArmPose.ITEM;
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