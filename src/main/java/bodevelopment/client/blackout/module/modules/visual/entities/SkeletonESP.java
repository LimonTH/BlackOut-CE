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
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public class SkeletonESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> friendColor = this.sgGeneral.colorSetting("Friend Color", new BlackOutColor(0, 255, 255, 255), ".");

    public SkeletonESP() {
        super("Skeleton ESP", ".", SubCategory.ENTITIES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        Camera camera = BlackOut.mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player == BlackOut.mc.player || player.isInvisible()) continue;

            MatrixStack stack = event.stack;
            stack.push();
            stack.loadIdentity();
            stack.multiply(new Quaternionf(camera.getRotation()).conjugate());
            Vec3d renderPos = player.getLerpedPos(event.tickDelta);
            double x = renderPos.x - camPos.x;
            double y = renderPos.y - camPos.y;
            double z = renderPos.z - camPos.z;

            stack.translate((float) x, (float) y, (float) z);
            WireframeRenderer.ModelData data = new WireframeRenderer.ModelData(player, event.tickDelta);
            MatrixStack modelStack = new MatrixStack();
            modelStack.loadIdentity();

            WireframeRenderer.provider.consumer.start();
            renderModelData(modelStack, player, data);

            List<Vec3d[]> positions = WireframeRenderer.provider.consumer.positions;

            if (!positions.isEmpty()) {
                Render3DUtils.start();
                RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                RenderSystem.lineWidth(1.5F);
                RenderSystem.disableDepthTest();

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

                BlackOutColor color = (Managers.FRIENDS.isFriend(player) ? this.friendColor : this.lineColor).get();

                Matrix4f matrix = stack.peek().getPositionMatrix();
                this.renderBones(matrix, positions, builder, color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, color.alpha / 255.0F);

                BufferRenderer.drawWithGlobalProgram(builder.end());
                RenderSystem.enableDepthTest();
                Render3DUtils.end();
            }

            stack.pop();
        }
    }

    private void renderModelData(MatrixStack stack, AbstractClientPlayerEntity player, WireframeRenderer.ModelData data) {
        WireframeRenderer.renderModel(stack, player, data,
                new BlackOutColor(0,0,0,0), new BlackOutColor(0,0,0,0), RenderShape.Outlines);
    }

    private void renderBones(Matrix4f matrix, List<Vec3d[]> positions, BufferBuilder builder, float red, float green, float blue, float alpha) {
        if (positions.size() < 36) return;

        Vec3d bodyTop = this.average(positions.get(6));
        Vec3d bodyBottom = this.average(positions.get(7));

        Vec3d chest = bodyTop.lerp(bodyBottom, 0.15);
        Vec3d ass = bodyTop.lerp(bodyBottom, 0.85);

        for (int i = 0; i < 6; i++) {
            Vec3d boxTop = this.average(positions.get(i * 6));
            Vec3d boxBottom = this.average(positions.get(i * 6 + 1));

            switch (i) {
                case 0:
                    this.line(matrix, builder, boxTop.lerp(boxBottom, 0.25), boxBottom, red, green, blue, alpha);
                    break;
                case 1:
                    this.line(matrix, builder, chest, ass, red, green, blue, alpha);
                    break;
                case 2:
                case 3:
                    Vec3d shoulder = boxTop.lerp(boxBottom, 0.1);
                    Vec3d handBottom = boxTop.lerp(boxBottom, 0.9);

                    this.line(matrix, builder, shoulder, handBottom, red, green, blue, alpha);

                    this.line(matrix, builder, shoulder, chest, red, green, blue, alpha);
                    break;
                case 4:
                case 5:
                    Vec3d legTop = boxTop.lerp(boxBottom, 0.1);
                    Vec3d legBottom = boxTop.lerp(boxBottom, 0.9);
                    this.line(matrix, builder, legTop, legBottom, red, green, blue, alpha);
                    this.line(matrix, builder, legTop, ass, red, green, blue, alpha);
                    break;
            }
        }
    }

    private void line(Matrix4f matrix, BufferBuilder builder, Vec3d pos, Vec3d pos2, float red, float green, float blue, float alpha) {
        float dx = (float) (pos2.x - pos.x);
        float dy = (float) (pos2.y - pos.y);
        float dz = (float) (pos2.z - pos.z);

        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len; dy /= len; dz /= len;
        }

        builder.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(red, green, blue, alpha).normal(dx, dy, dz);
        builder.vertex(matrix, (float) pos2.x, (float) pos2.y, (float) pos2.z).color(red, green, blue, alpha).normal(dx, dy, dz);
    }

    private Vec3d average(Vec3d... vecs) {
        double x = 0, y = 0, z = 0;
        for (Vec3d v : vecs) {
            x += v.x; y += v.y; z += v.z;
        }
        return new Vec3d(x / vecs.length, y / vecs.length, z / vecs.length);
    }
}