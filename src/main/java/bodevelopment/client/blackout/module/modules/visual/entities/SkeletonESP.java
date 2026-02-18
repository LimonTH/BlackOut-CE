package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
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
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class SkeletonESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.c("Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> friendColor = this.sgGeneral.c("Friend Color", new BlackOutColor(0, 255, 255, 255), ".");

    public SkeletonESP() {
        super("Skeleton ESP", ".", SubCategory.ENTITIES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
                if (player != BlackOut.mc.player) {
                    WireframeRenderer.matrixStack.push();
                    Render3DUtils.setRotation(WireframeRenderer.matrixStack);
                    Render3DUtils.start();
                    WireframeRenderer.provider.consumer.start();
                    WireframeRenderer.drawEntity(player, event.tickDelta, WireframeRenderer.provider);
                    List<Vec3d[]> positions = WireframeRenderer.provider.consumer.positions;
                    RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                    RenderSystem.lineWidth(1.5F);
                    RenderSystem.disableDepthTest();

                    RenderSystem.getModelViewStack().pushMatrix();
                    RenderSystem.getModelViewStack().identity();
                    RenderSystem.applyModelViewMatrix();

                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
                    BlackOutColor color = (Managers.FRIENDS.isFriend(player) ? this.friendColor : this.lineColor).get();
                    this.renderBones(positions, builder, color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, color.alpha / 255.0F);
                    BufferRenderer.drawWithGlobalProgram(builder.end());

                    RenderSystem.getModelViewStack().popMatrix();
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.enableDepthTest();

                    Render3DUtils.end();
                    WireframeRenderer.matrixStack.pop();
                }
            }
        }
    }

    private void renderBones(List<Vec3d[]> positions, BufferBuilder builder, float red, float green, float blue, float alpha) {
        Vec3d chest = Vec3d.ZERO;
        Vec3d ass = Vec3d.ZERO;
        if (positions.size() >= 36) {
            for (int i = 0; i < 6; i++) {
                Vec3d boxTop = this.average(positions.get(i * 6));
                Vec3d boxBottom = this.average(positions.get(i * 6 + 1));
                switch (i) {
                    case 0:
                        this.line(builder, boxTop.lerp(boxBottom, 0.25), boxBottom, red, green, blue, alpha);
                        break;
                    case 1:
                        chest = boxTop.lerp(boxBottom, 0.05);
                        ass = boxTop.lerp(boxBottom, 0.95);
                        this.line(builder, boxTop, ass, red, green, blue, alpha);
                        break;
                    case 2:
                    case 3:
                        Vec3d shoulder = boxTop.lerp(boxBottom, 0.1);
                        Vec3d handBottom = boxTop.lerp(boxBottom, 0.9);
                        this.line(builder, shoulder, handBottom, red, green, blue, alpha);
                        this.line(builder, shoulder, chest, red, green, blue, alpha);
                        break;
                    case 4:
                    case 5:
                        Vec3d legBottom = boxTop.lerp(boxBottom, 0.9);
                        this.line(builder, boxTop, legBottom, red, green, blue, alpha);
                        this.line(builder, boxTop, ass, red, green, blue, alpha);
                }
            }
        }
    }

    private void line(BufferBuilder builder, Vec3d pos, Vec3d pos2, float red, float green, float blue, float alpha) {
        Vec3d normal = pos2.subtract(pos).normalize();
        builder.vertex((float) pos.x, (float) pos.y, (float) pos.z)
                .color(red, green, blue, alpha)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                ;
        builder.vertex((float) pos2.x, (float) pos2.y, (float) pos2.z)
                .color(red, green, blue, alpha)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                ;
    }

    private Vec3d average(Vec3d... vecs) {
        Vec3d total = vecs[0];

        for (int i = 1; i < vecs.length; i++) {
            total = total.add(vecs[i]);
        }

        return total.multiply(1.0F / vecs.length);
    }
}
