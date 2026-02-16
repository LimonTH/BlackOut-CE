package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.ColorRenderer;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

public class FeetESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<List<EntityType<?>>> entities = this.sgGeneral.el("Entities", ".", EntityType.PLAYER);
    private final Setting<RenderShape> renderShape = this.sgGeneral.e("Render Shape", RenderShape.Full, "Which parts of boxes should be rendered.");
    private final Setting<BlackOutColor> fill = this.sgGeneral.c("Fill Color", new BlackOutColor(255, 255, 255, 80), "Fill Color");
    private final Setting<BlackOutColor> line = this.sgGeneral.c("Line Color", new BlackOutColor(255, 255, 255, 120), "Fill Color");

    public FeetESP() {
        super("FeetESP", "Shows the feet hitbox does not show feet pictures", SubCategory.ENTITIES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            Render3DUtils.matrices.push();
            Render3DUtils.setRotation(Render3DUtils.matrices);
            Render3DUtils.start();
            Vec3d vec = BlackOut.mc.gameRenderer.getCamera().getPos();
            Render3DUtils.matrices.translate(-vec.x, -vec.y, -vec.z);
            ColorRenderer renderer = ColorRenderer.getInstance();
            if (this.renderShape.get().sides) {
                this.drawVertexes(false, renderer);
            }

            if (this.renderShape.get().outlines) {
                this.drawVertexes(true, renderer);
            }

            Render3DUtils.end();
            Render3DUtils.matrices.pop();
        }
    }

    private void drawVertexes(boolean outline, ColorRenderer renderer) {
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder bufferBuilder = null;

        if (outline) {
            RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
            RenderSystem.lineWidth(1.5F);

            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().identity();
            RenderSystem.applyModelViewMatrix();

            bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        } else {
            renderer.startRender(Render3DUtils.matrices, VertexFormat.DrawMode.QUADS);
        }

        BlackOutColor color = outline ? this.line.get() : this.fill.get();
        float r = color.red / 255.0F;
        float g = color.green / 255.0F;
        float b = color.blue / 255.0F;
        float a = color.alpha / 255.0F;

        final BufferBuilder finalBuilder = bufferBuilder;

        BlackOut.mc.world.getEntities().forEach(entity -> {
            if (this.entities.get().contains(entity.getType())) {
                Box box = entity.getBoundingBox();
                Vec3d pos = new Vec3d(entity.prevX, entity.prevY, entity.prevZ)
                        .lerp(entity.getPos(), BlackOut.mc.getRenderTickCounter().getTickDelta(true));

                float minX = (float) (pos.x - box.getLengthX() / 2.0);
                float maxX = (float) (pos.x + box.getLengthX() / 2.0);
                float minZ = (float) (pos.z - box.getLengthZ() / 2.0);
                float maxZ = (float) (pos.z + box.getLengthZ() / 2.0);
                float y = (float) pos.y;

                if (outline) {
                    Matrix4f matrix4f = Render3DUtils.matrices.peek().getPositionMatrix();
                    Render3DUtils.drawPlane(matrix4f, finalBuilder, minX, y, minZ, minX, y, maxZ, minX, y, maxZ, maxX, y, maxZ, r, g, b, a);
                    Render3DUtils.drawPlane(matrix4f, finalBuilder, maxX, y, maxZ, maxX, y, minZ, maxX, y, minZ, minX, y, minZ, r, g, b, a);
                } else {
                    renderer.vertex(minX, y, minZ, r, g, b, a);
                    renderer.vertex(minX, y, maxZ, r, g, b, a);
                    renderer.vertex(maxX, y, maxZ, r, g, b, a);
                    renderer.vertex(maxX, y, minZ, r, g, b, a);
                }
            }
        });

        if (outline) {
            BuiltBuffer builtBuffer = bufferBuilder.end();
            if (builtBuffer != null) {
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }

            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        } else {
            renderer.endRender();
        }
    }
}
