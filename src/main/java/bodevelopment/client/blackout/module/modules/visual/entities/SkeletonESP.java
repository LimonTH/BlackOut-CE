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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public class SkeletonESP extends Module {
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
            stack.setIdentity();
            stack.mulPose(new Quaternionf(camera.rotation()).conjugate());

            Vec3 renderPos = player.getPosition(event.tickDelta);
            stack.translate((float) (renderPos.x - camPos.x), (float) (renderPos.y - camPos.y), (float) (renderPos.z - camPos.z));

            WireframeRenderer.ModelData data = new WireframeRenderer.ModelData(player, event.tickDelta);
            PoseStack modelStack = new PoseStack();
            modelStack.setIdentity();

            WireframeRenderer.provider.consumer.start();
            BlackOutColor dummy = new BlackOutColor(0, 0, 0, 0);
            WireframeRenderer.renderModel(modelStack, player, data, dummy, dummy, RenderShape.Outlines);
            WireframeRenderer.provider.consumer.nextPart();

            List<List<Vec3>> parts = WireframeRenderer.provider.consumer.parts;
            if (parts.size() >= 6) {
                Render3DUtils.start();
                RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
                RenderSystem.lineWidth(1.5F);
                RenderSystem.disableDepthTest();

                Tesselator tessellator = Tesselator.getInstance();
                BufferBuilder builder = tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

                BlackOutColor color = (Managers.FRIENDS.isFriend(player) ? this.friendColor : this.lineColor).get();
                float r = color.red / 255.0F, g = color.green / 255.0F, b = color.blue / 255.0F, a = color.alpha / 255.0F;

                Matrix4f matrix = stack.last().pose();

                Vec3 headTop = getExtremum(parts.get(0), true);
                Vec3 headBottom = getExtremum(parts.get(0), false);

                Vec3 bodyTop = getExtremum(parts.get(1), true);
                Vec3 bodyBottom = getExtremum(parts.get(1), false);

                line(matrix, builder, headTop, headBottom, r, g, b, a);

                line(matrix, builder, headBottom, bodyTop, r, g, b, a);

                line(matrix, builder, bodyTop, bodyBottom, r, g, b, a);

                line(matrix, builder, bodyTop, bodyBottom, r, g, b, a);

                renderLimb(matrix, builder, parts.get(2), bodyTop, r, g, b, a);
                renderLimb(matrix, builder, parts.get(3), bodyTop, r, g, b, a);

                renderLimb(matrix, builder, parts.get(4), bodyBottom, r, g, b, a);
                renderLimb(matrix, builder, parts.get(5), bodyBottom, r, g, b, a);

                BufferUploader.drawWithShader(builder.buildOrThrow());
                RenderSystem.enableDepthTest();
                Render3DUtils.end();
            }
            stack.popPose();
        }
    }

    private void renderLimb(Matrix4f matrix, BufferBuilder builder, List<Vec3> vertices, Vec3 attach, float r, float g, float b, float a) {
        Vec3 top = getExtremum(vertices, true);
        Vec3 bottom = getExtremum(vertices, false);
        line(matrix, builder, attach, top, r, g, b, a);
        line(matrix, builder, top, bottom, r, g, b, a);
    }

    private Vec3 getExtremum(List<Vec3> vertices, boolean top) {
        if (vertices.isEmpty()) return Vec3.ZERO;

        double targetY = vertices.getFirst().y;
        for (Vec3 v : vertices) {
            if (top ? (v.y > targetY) : (v.y < targetY)) {
                targetY = v.y;
            }
        }

        double avgX = 0, avgZ = 0;
        int count = 0;
        for (Vec3 v : vertices) {
            if (Math.abs(v.y - targetY) < 0.01) {
                avgX += v.x;
                avgZ += v.z;
                count++;
            }
        }

        if (count == 0) return new Vec3(0, targetY, 0);

        return new Vec3(avgX / count, targetY, avgZ / count);
    }

    private void line(Matrix4f matrix, BufferBuilder builder, Vec3 pos, Vec3 pos2, float red, float green, float blue, float alpha) {
        float dx = (float) (pos2.x - pos.x);
        float dy = (float) (pos2.y - pos.y);
        float dz = (float) (pos2.z - pos.z);

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len; dy /= len; dz /= len;
        }

        builder.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).setColor(red, green, blue, alpha).setNormal(dx, dy, dz);
        builder.addVertex(matrix, (float) pos2.x, (float) pos2.y, (float) pos2.z).setColor(red, green, blue, alpha).setNormal(dx, dy, dz);
    }
}