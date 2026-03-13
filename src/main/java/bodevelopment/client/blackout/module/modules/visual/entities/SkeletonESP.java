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
            double x = renderPos.x - camPos.x;
            double y = renderPos.y - camPos.y;
            double z = renderPos.z - camPos.z;

            stack.translate((float) x, (float) y, (float) z);
            WireframeRenderer.ModelData data = new WireframeRenderer.ModelData(player, event.tickDelta);
            PoseStack modelStack = new PoseStack();
            modelStack.setIdentity();

            WireframeRenderer.provider.consumer.start();
            renderModelData(modelStack, player, data);

            List<Vec3[]> positions = WireframeRenderer.provider.consumer.positions;

            if (!positions.isEmpty()) {
                Render3DUtils.start();
                RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
                RenderSystem.lineWidth(1.5F);
                RenderSystem.disableDepthTest();

                Tesselator tessellator = Tesselator.getInstance();
                BufferBuilder builder = tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

                BlackOutColor color = (Managers.FRIENDS.isFriend(player) ? this.friendColor : this.lineColor).get();

                Matrix4f matrix = stack.last().pose();
                this.renderBones(matrix, positions, builder, color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, color.alpha / 255.0F);

                BufferUploader.drawWithShader(builder.buildOrThrow());
                RenderSystem.enableDepthTest();
                Render3DUtils.end();
            }

            stack.popPose();
        }
    }

    private void renderModelData(PoseStack stack, AbstractClientPlayer player, WireframeRenderer.ModelData data) {
        WireframeRenderer.renderModel(stack, player, data,
                new BlackOutColor(0,0,0,0), new BlackOutColor(0,0,0,0), RenderShape.Outlines);
    }

    private void renderBones(Matrix4f matrix, List<Vec3[]> positions, BufferBuilder builder, float red, float green, float blue, float alpha) {
        if (positions.size() < 36) return;

        Vec3 bodyTop = this.average(positions.get(6));
        Vec3 bodyBottom = this.average(positions.get(7));

        Vec3 chest = bodyTop.lerp(bodyBottom, 0.15);
        Vec3 ass = bodyTop.lerp(bodyBottom, 0.85);

        for (int i = 0; i < 6; i++) {
            Vec3 boxTop = this.average(positions.get(i * 6));
            Vec3 boxBottom = this.average(positions.get(i * 6 + 1));

            switch (i) {
                case 0:
                    this.line(matrix, builder, boxTop.lerp(boxBottom, 0.25), boxBottom, red, green, blue, alpha);
                    break;
                case 1:
                    this.line(matrix, builder, chest, ass, red, green, blue, alpha);
                    break;
                case 2:
                case 3:
                    Vec3 shoulder = boxTop.lerp(boxBottom, 0.1);
                    Vec3 handBottom = boxTop.lerp(boxBottom, 0.9);

                    this.line(matrix, builder, shoulder, handBottom, red, green, blue, alpha);

                    this.line(matrix, builder, shoulder, chest, red, green, blue, alpha);
                    break;
                case 4:
                case 5:
                    Vec3 legTop = boxTop.lerp(boxBottom, 0.1);
                    Vec3 legBottom = boxTop.lerp(boxBottom, 0.9);
                    this.line(matrix, builder, legTop, legBottom, red, green, blue, alpha);
                    this.line(matrix, builder, legTop, ass, red, green, blue, alpha);
                    break;
            }
        }
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

    private Vec3 average(Vec3... vecs) {
        double x = 0, y = 0, z = 0;
        for (Vec3 v : vecs) {
            x += v.x; y += v.y; z += v.z;
        }
        return new Vec3(x / vecs.length, y / vecs.length, z / vecs.length);
    }
}