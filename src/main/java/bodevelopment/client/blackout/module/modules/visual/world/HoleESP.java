package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.HoleType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.Hole;
import bodevelopment.client.blackout.util.HoleUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class HoleESP extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Double> range = this.sgGeneral.d("Range", 8.0, 0.0, 10.0, 0.1, "Maximum range to a hole.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.c("Line Color", new BlackOutColor(255, 0, 0, 255), "Line color of rendered boxes.");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.c("Side Color", new BlackOutColor(255, 0, 0, 50), "Side color of rendered boxes.");
    private final Setting<Boolean> bottomLines = this.sgGeneral.b("Bottom Lines", true, "Renders lines in hole bottom.");
    private final Setting<Boolean> bottomSide = this.sgGeneral.b("Bottom Side", true, "Renders bottom plane.");
    private final Setting<Boolean> fadeLines = this.sgGeneral.b("Fade Lines", true, "Renders lines in fade.");
    private final Setting<Boolean> fadeSides = this.sgGeneral.b("Fade Sides", true, "Renders sides in fade.");
    private final Setting<Double> minHeight = this.sgGeneral.d("Min Height", 0.5, -1.0, 1.0, 0.05, ".");
    private final Setting<Double> maxHeight = this.sgGeneral.d("Max Height", 1.0, -1.0, 1.0, 0.05, "How tall should the fade be.");
    private final Setting<Double> breathingSpeed = this.sgGeneral
            .d("Breathing Speed", 1.0, 0.0, 10.0, 0.1, ".", () -> !this.minHeight.get().equals(this.maxHeight.get()));
    private final List<Hole> holes = new ArrayList<>();
    private long prevCalc = 0L;

    public HoleESP() {
        super("Hole ESP", "Highlights holes near you.", SubCategory.WORLD, true);
    }

    public static void fadePlane(
            Matrix4f matrix4f,
            VertexConsumer vertexConsumer,
            float x1,
            float z1,
            float x2,
            float z2,
            float x3,
            float z3,
            float x4,
            float z4,
            float y,
            float height,
            float r,
            float g,
            float b,
            float a
    ) {
        vertexConsumer.vertex(matrix4f, x1, y, z1).color(r, g, b, a).normal(0.0F, 0.0F, 0.0F);
        vertexConsumer.vertex(matrix4f, x2, y, z2).color(r, g, b, a).normal(0.0F, 0.0F, 0.0F);
        vertexConsumer.vertex(matrix4f, x3, y + height, z3).color(r, g, b, 0.0F).normal(0.0F, 0.0F, 0.0F);
        vertexConsumer.vertex(matrix4f, x4, y + height, z4).color(r, g, b, 0.0F).normal(0.0F, 0.0F, 0.0F);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (System.currentTimeMillis() - this.prevCalc > 100L) {
            this.findHoles(BlackOut.mc.player.getBlockPos(), (int) Math.ceil(this.range.get()), this.range.get() * this.range.get());
            this.prevCalc = System.currentTimeMillis();
        }

        Render3DUtils.matrices.push();
        Render3DUtils.setRotation(Render3DUtils.matrices);
        Render3DUtils.start();
        if (this.bottomSide.get() || this.fadeSides.get()) {
            this.drawSides();
        }

        if (this.bottomLines.get() || this.fadeLines.get()) {
            this.drawLines();
        }

        Render3DUtils.end();
        Render3DUtils.matrices.pop();
    }

    private void drawSides() {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        double x = BlackOut.mc.gameRenderer.getCamera().getPos().x;
        double y = BlackOut.mc.gameRenderer.getCamera().getPos().y;
        double z = BlackOut.mc.gameRenderer.getCamera().getPos().z;
        MatrixStack.Entry entry = Render3DUtils.matrices.peek();
        org.joml.Matrix4f matrix4f = entry.getPositionMatrix();
        float red = this.sideColor.get().red / 255.0F;
        float green = this.sideColor.get().green / 255.0F;
        float blue = this.sideColor.get().blue / 255.0F;
        float alpha = this.sideColor.get().alpha / 255.0F;
        this.holes
                .forEach(
                        hole -> {
                            int ox = switch (hole.type) {
                                case DoubleX, Quad -> 2;
                                default -> 1;
                            };

                            int oz = switch (hole.type) {
                                case Quad, DoubleZ -> 2;
                                default -> 1;
                            };
                            float a = this.getAlpha(this.dist(hole.middle, x, y, z)) * alpha;
                            Vector3f v = new Vector3f((float) (hole.pos.getX() - x), (float) (hole.pos.getY() - y), (float) (hole.pos.getZ() - z));
                            if (this.bottomSide.get()) {
                                Render3DUtils.drawPlane(
                                        matrix4f, bufferBuilder, v.x, v.y, v.z, v.x + ox, v.y, v.z, v.x + ox, v.y, v.z + oz, v.x, v.y, v.z + oz, red, green, blue, a
                                );
                            }

                            if (this.fadeSides.get()) {
                                float height = this.getHeight(hole.pos);
                                fadePlane(matrix4f, bufferBuilder, v.x, v.z, v.x, v.z + oz, v.x, v.z + oz, v.x, v.z, v.y, height, red, green, blue, a);
                                fadePlane(matrix4f, bufferBuilder, v.x + ox, v.z, v.x + ox, v.z + oz, v.x + ox, v.z + oz, v.x + ox, v.z, v.y, height, red, green, blue, a);
                                fadePlane(matrix4f, bufferBuilder, v.x, v.z, v.x + ox, v.z, v.x + ox, v.z, v.x, v.z, v.y, height, red, green, blue, a);
                                fadePlane(matrix4f, bufferBuilder, v.x, v.z + oz, v.x + ox, v.z + oz, v.x + ox, v.z + oz, v.x, v.z + oz, v.y, height, red, green, blue, a);
                            }
                        }
                );
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private float getHeight(BlockPos pos) {
        double offset = pos.getX() + pos.getZ();
        return (float) MathHelper.lerp(
                Math.sin(offset / 2.0 + System.currentTimeMillis() * this.breathingSpeed.get() / 500.0) / 2.0 + 0.5, this.minHeight.get(), this.maxHeight.get()
        );
    }

    private void drawLines() {
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(1.5F);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        double x = BlackOut.mc.gameRenderer.getCamera().getPos().x;
        double y = BlackOut.mc.gameRenderer.getCamera().getPos().y;
        double z = BlackOut.mc.gameRenderer.getCamera().getPos().z;
        Matrix4f matrix4f = Render3DUtils.matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = Render3DUtils.matrices.peek();
        float red = this.lineColor.get().red / 255.0F;
        float green = this.lineColor.get().green / 255.0F;
        float blue = this.lineColor.get().blue / 255.0F;
        float alpha = this.lineColor.get().alpha / 255.0F;
        this.holes.forEach(hole -> {
            int ox = switch (hole.type) {
                case DoubleX, Quad -> 2;
                default -> 1;
            };

            int oz = switch (hole.type) {
                case Quad, DoubleZ -> 2;
                default -> 1;
            };
            float a = this.getAlpha(this.dist(hole.middle, x, y, z)) * alpha;
            Vector3f v = new Vector3f((float) (hole.pos.getX() - x), (float) (hole.pos.getY() - y), (float) (hole.pos.getZ() - z));
            if (this.bottomLines.get()) {
                this.hline(bufferBuilder, matrix4f, entry, v.x, v.z, v.x, v.z + oz, v.y, red, green, blue, a);
                this.hline(bufferBuilder, matrix4f, entry, v.x + ox, v.z, v.x + ox, v.z + oz, v.y, red, green, blue, a);
                this.hline(bufferBuilder, matrix4f, entry, v.x, v.z, v.x + ox, v.z, v.y, red, green, blue, a);
                this.hline(bufferBuilder, matrix4f, entry, v.x, v.z + oz, v.x + ox, v.z + oz, v.y, red, green, blue, a);
            }

            if (this.fadeLines.get()) {
                float height = this.getHeight(hole.pos);
                this.fadeLine(matrix4f, entry, bufferBuilder, v.x, v.z, v.y, height, red, green, blue, a);
                this.fadeLine(matrix4f, entry, bufferBuilder, v.x + ox, v.z, v.y, height, red, green, blue, a);
                this.fadeLine(matrix4f, entry, bufferBuilder, v.x, v.z + oz, v.y, height, red, green, blue, a);
                this.fadeLine(matrix4f, entry, bufferBuilder, v.x + ox, v.z + oz, v.y, height, red, green, blue, a);
            }
        });
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private void hline(
            VertexConsumer consumer, Matrix4f matrix4f, MatrixStack.Entry entry, float x, float z, float x2, float z2, float y, float r, float g, float b, float a
    ) {
        float dx = x2 - x;
        float dz = z2 - z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        float nx = dx / length;
        float nz = dz / length;
        consumer.vertex(matrix4f, x, y, z).color(r, g, b, a).normal(entry, nx, 0.0F, nz);
        consumer.vertex(matrix4f, x2, y, z2).color(r, g, b, a).normal(entry, nx, 0.0F, nz);
    }

    public void fadeLine(
            Matrix4f matrix4f, MatrixStack.Entry entry, VertexConsumer vertexConsumer, float x, float z, float y, float height, float r, float g, float b, float a
    ) {
        vertexConsumer.vertex(matrix4f, x, y, z).color(r, g, b, a).normal(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.vertex(matrix4f, x, y + height, z).color(r, g, b, 0.0F).normal(entry, 0.0F, 1.0F, 0.0F);
    }

    private double dist(Vec3d vec3d, double x, double y, double z) {
        double dx = vec3d.x - x;
        double dy = vec3d.y - y;
        double dz = vec3d.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private float getAlpha(double dist) {
        return (float) MathHelper.clamp(1.0 - (dist - this.range.get() / 2.0) / (this.range.get() / 2.0), 0.0, 1.0);
    }

    private void findHoles(BlockPos center, int r, double radiusSq) {
        this.holes.clear();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (!(x * x + y * y + z * z > radiusSq)) {
                        BlockPos pos = center.add(x, y, z);
                        Hole h = HoleUtils.getHole(pos, 3, true);
                        if (h.type != HoleType.NotHole) {
                            this.holes.add(h);
                        }
                    }
                }
            }
        }
    }
}
