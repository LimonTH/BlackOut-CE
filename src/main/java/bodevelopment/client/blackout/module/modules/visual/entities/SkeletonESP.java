package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public class SkeletonESP extends Module {
    private static final MultiBufferSource.BufferSource bufSrc = MultiBufferSource.immediate(new ByteBufferBuilder(256));
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Neutral Color", new BlackOutColor(255, 0, 0, 255), "The color applied to the bone structure of standard players.");
    private final Setting<BlackOutColor> friendColor = this.sgGeneral.colorSetting("Friendship Color", new BlackOutColor(0, 255, 255, 255), "The color applied to the bone structure of players on your friend list.");

    public SkeletonESP() {
        super("Skeleton ESP", "Renders a simplified stick-figure representation of other players' skeletal structures by connecting their joint positions.", SubCategory.ENTITIES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.level == null) return;

        Camera camera = BlackOut.mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();

        for (AbstractClientPlayer player : BlackOut.mc.level.players()) {
            if (player == BlackOut.mc.player || player.isInvisible()) continue;

            PoseStack stack = event.stack;
            stack.pushPose();

            double x = net.minecraft.util.Mth.lerp(event.tickDelta, player.xOld, player.getX()) - camPos.x;
            double y = net.minecraft.util.Mth.lerp(event.tickDelta, player.yOld, player.getY()) - camPos.y;
            double z = net.minecraft.util.Mth.lerp(event.tickDelta, player.zOld, player.getZ()) - camPos.z;

            stack.setIdentity();
            stack.mulPose(new Quaternionf(camera.rotation()).conjugate());
            stack.translate((float) x, (float) y, (float) z);

            WireframeRenderer.ModelData data = new WireframeRenderer.ModelData(player, event.tickDelta);

            PoseStack modelTransformStack = new PoseStack();
            modelTransformStack.setIdentity();

            WireframeRenderer.provider.consumer.start();
            BlackOutColor dummy = new BlackOutColor(0, 0, 0, 0);

            WireframeRenderer.renderModel(modelTransformStack, player, data, dummy, dummy, RenderShape.Outlines);
            WireframeRenderer.provider.consumer.nextPart();

            List<List<Vec3>> parts = WireframeRenderer.provider.consumer.parts;
            if (parts.size() >= 6) {
                Render3DUtils.start();
                RenderSystem.lineWidth(1.5F);
                GlStateManager._disableDepthTest();

                VertexConsumer builder = bufSrc.getBuffer(RenderType.lines());

                BlackOutColor color = (Managers.FRIENDS.isFriend(player) ? this.friendColor : this.lineColor).get();
                float r = color.red / 255.0F, g = color.green / 255.0F, b = color.blue / 255.0F, a = color.alpha / 255.0F;

                Matrix4f matrix = stack.last().pose();

                Vec3 headCenter = getCenter(parts.get(0));
                Vec3 bodyTop = getExtremum(parts.get(1), true);
                Vec3 bodyBottom = getExtremum(parts.get(1), false);

                Vec3 neckVector = headCenter.subtract(bodyTop);
                Vec3 extendedHeadPoint = bodyTop.add(neckVector.scale(1.6));

                line(matrix, builder, bodyTop, extendedHeadPoint, r, g, b, a);
                line(matrix, builder, bodyTop, bodyBottom, r, g, b, a);

                renderLimb(matrix, builder, parts.get(2), bodyTop, r, g, b, a);
                renderLimb(matrix, builder, parts.get(3), bodyTop, r, g, b, a);
                renderLimb(matrix, builder, parts.get(4), bodyBottom, r, g, b, a);
                renderLimb(matrix, builder, parts.get(5), bodyBottom, r, g, b, a);

                bufSrc.endBatch();
                GlStateManager._enableDepthTest();
                Render3DUtils.end();
            }
            stack.popPose();
        }
    }

    private void renderLimb(Matrix4f matrix, VertexConsumer builder, List<Vec3> vertices, Vec3 attach, float r, float g, float b, float a) {
        if (vertices.isEmpty()) return;
        Vec3 joint = getStableJoint(vertices, attach);

        Vec3 endPoint = getExtremumFromPoint(vertices, joint);

        line(matrix, builder, attach, joint, r, g, b, a);
        line(matrix, builder, joint, endPoint, r, g, b, a);
    }

    private Vec3 getStableJoint(List<Vec3> vertices, Vec3 attach) {
        Vec3 closest = vertices.getFirst();
        double minDist = Double.MAX_VALUE;

        for (Vec3 v : vertices) {
            double d = v.distanceToSqr(attach);
            if (d < minDist) {
                minDist = d;
                closest = v;
            }
        }

        double avgX = 0, avgY = 0, avgZ = 0;
        int count = 0;
        for (Vec3 v : vertices) {
            if (v.distanceToSqr(closest) < 0.25) {
                avgX += v.x; avgY += v.y; avgZ += v.z;
                count++;
            }
        }

        return count > 0 ? new Vec3(avgX / count, avgY / count, avgZ / count) : closest;
    }

    private Vec3 getExtremumFromPoint(List<Vec3> vertices, Vec3 joint) {
        if (vertices.isEmpty()) return Vec3.ZERO;

        double maxDistSqr = 0;
        for (Vec3 v : vertices) {
            maxDistSqr = Math.max(maxDistSqr, v.distanceToSqr(joint));
        }

        double threshold = maxDistSqr * 0.8;
        double avgX = 0, avgY = 0, avgZ = 0;
        int count = 0;

        for (Vec3 v : vertices) {
            if (v.distanceToSqr(joint) >= threshold) {
                avgX += v.x; avgY += v.y; avgZ += v.z;
                count++;
            }
        }

        return count > 0 ? new Vec3(avgX / count, avgY / count, avgZ / count) : vertices.getFirst();
    }

    private Vec3 getExtremum(List<Vec3> vertices, boolean top) {
        if (vertices.isEmpty()) return Vec3.ZERO;
        Vec3 extremum = vertices.getFirst();
        for (Vec3 v : vertices) {
            if (top ? (v.y > extremum.y) : (v.y < extremum.y)) extremum = v;
        }
        double avgX = 0, avgZ = 0;
        for (Vec3 v : vertices) { avgX += v.x; avgZ += v.z; }
        return new Vec3(avgX / vertices.size(), extremum.y, avgZ / vertices.size());
    }

    private Vec3 getCenter(List<Vec3> vertices) {
        if (vertices.isEmpty()) return Vec3.ZERO;
        double x = 0, y = 0, z = 0;
        for (Vec3 v : vertices) {
            x += v.x;
            y += v.y;
            z += v.z;
        }
        return new Vec3(x / vertices.size(), y / vertices.size(), z / vertices.size());
    }

    private void line(Matrix4f matrix, VertexConsumer builder, Vec3 pos, Vec3 pos2, float red, float green, float blue, float alpha) {
        float dx = (float) (pos2.x - pos.x);
        float dy = (float) (pos2.y - pos.y);
        float dz = (float) (pos2.z - pos.z);

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len < 0.001F) {
            dx = 0;
            dy = 1;
            dz = 0;
        } else {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(red, green, blue, alpha)
                .setNormal(dx, dy, dz);

        builder.addVertex(matrix, (float) pos2.x, (float) pos2.y, (float) pos2.z)
                .setColor(red, green, blue, alpha)
                .setNormal(dx, dy, dz);
    }
}