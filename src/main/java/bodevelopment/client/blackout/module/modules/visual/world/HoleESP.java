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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class HoleESP extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> range = this.sgGeneral.doubleSetting("Detection Radius", 8.0, 0.0, 10.0, 0.1, "The maximum distance from the player to scan for defensive holes.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Wireframe Color", new BlackOutColor(255, 0, 0, 255), "The color of the structural edges for the hole highlights.");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Surface Color", new BlackOutColor(255, 0, 0, 50), "The fill color for the faces of the hole highlights.");
    private final Setting<Boolean> bottomLines = this.sgGeneral.booleanSetting("Base Outlines", true, "Renders the wireframe for the floor of the hole.");
    private final Setting<Boolean> bottomSide = this.sgGeneral.booleanSetting("Base Surface", true, "Renders the solid plane for the floor of the hole.");
    private final Setting<Boolean> fadeLines = this.sgGeneral.booleanSetting("Vertical Outlines", true, "Renders rising wireframe edges that fade out vertically.");
    private final Setting<Boolean> fadeSides = this.sgGeneral.booleanSetting("Gradient Sides", true, "Renders rising vertical surfaces with a transparency gradient.");
    private final Setting<Double> minHeight = this.sgGeneral.doubleSetting("Minimum Altitude", 0.5, -1.0, 1.0, 0.05, "The lower bound for the volumetric highlight height.");
    private final Setting<Double> maxHeight = this.sgGeneral.doubleSetting("Maximum Altitude", 1.0, -1.0, 1.0, 0.05, "The upper bound for the volumetric highlight height.");
    private final Setting<Double> breathingSpeed = this.sgGeneral.doubleSetting("Oscillation Frequency", 1.0, 0.0, 10.0, 0.1, "The speed of the 'breathing' animation that fluctuates the highlight height.", () -> !this.minHeight.get().equals(this.maxHeight.get()));

    private final List<Hole> holes = new ArrayList<>();
    private long prevCalc = 0L;

    public HoleESP() {
        super("Hole ESP", "Visualizes safe defensive positions (obsidian/bedrock holes) with customizable volumetric rendering and breathing animations.", SubCategory.WORLD, true);
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
        vertexConsumer.addVertex(matrix4f, x1, y, z1).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix4f, x2, y, z2).setColor(r, g, b, a).setNormal(0.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix4f, x3, y + height, z3).setColor(r, g, b, 0.0F).setNormal(0.0F, 0.0F, 0.0F);
        vertexConsumer.addVertex(matrix4f, x4, y + height, z4).setColor(r, g, b, 0.0F).setNormal(0.0F, 0.0F, 0.0F);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (System.currentTimeMillis() - this.prevCalc > 100L) {
            this.findHoles(BlackOut.mc.player.blockPosition(), (int) Math.ceil(this.range.get()), this.range.get() * this.range.get());
            this.prevCalc = System.currentTimeMillis();
        }

        Render3DUtils.matrices.pushPose();
        Render3DUtils.setRotation(Render3DUtils.matrices);
        Render3DUtils.start();
        if (this.bottomSide.get() || this.fadeSides.get()) {
            this.drawSides();
        }

        if (this.bottomLines.get() || this.fadeLines.get()) {
            this.drawLines();
        }

        Render3DUtils.end();
        Render3DUtils.matrices.popPose();
    }

    private void drawSides() {
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double x = BlackOut.mc.gameRenderer.getMainCamera().getPosition().x;
        double y = BlackOut.mc.gameRenderer.getMainCamera().getPosition().y;
        double z = BlackOut.mc.gameRenderer.getMainCamera().getPosition().z;
        PoseStack.Pose entry = Render3DUtils.matrices.last();
        Matrix4f matrix4f = entry.pose();
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
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private float getHeight(BlockPos pos) {
        double offset = pos.getX() + pos.getZ();
        return (float) Mth.lerp(
                Math.sin(offset / 2.0 + System.currentTimeMillis() * this.breathingSpeed.get() / 500.0) / 2.0 + 0.5, this.minHeight.get(), this.maxHeight.get()
        );
    }

    private void drawLines() {
        RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
        RenderSystem.lineWidth(1.5F);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        Vec3 camPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();
        double x = camPos.x;
        double y = camPos.y;
        double z = camPos.z;

        PoseStack.Pose entry = Render3DUtils.matrices.last();
        Matrix4f matrix4f = entry.pose();

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

            float vx = (float) (hole.pos.getX() - x);
            float vy = (float) (hole.pos.getY() - y);
            float vz = (float) (hole.pos.getZ() - z);

            if (this.bottomLines.get()) {
                this.hline(bufferBuilder, matrix4f, entry, vx, vz, vx, vz + oz, vy, red, green, blue, a);
                this.hline(bufferBuilder, matrix4f, entry, vx + ox, vz, vx + ox, vz + oz, vy, red, green, blue, a);
                this.hline(bufferBuilder, matrix4f, entry, vx, vz, vx + ox, vz, vy, red, green, blue, a);
                this.hline(bufferBuilder, matrix4f, entry, vx, vz + oz, vx + ox, vz + oz, vy, red, green, blue, a);
            }

            if (this.fadeLines.get()) {
                float height = this.getHeight(hole.pos);
                this.fadeLine(matrix4f, entry, bufferBuilder, vx, vz, vy, height, red, green, blue, a);
                this.fadeLine(matrix4f, entry, bufferBuilder, vx + ox, vz, vy, height, red, green, blue, a);
                this.fadeLine(matrix4f, entry, bufferBuilder, vx, vz + oz, vy, height, red, green, blue, a);
                this.fadeLine(matrix4f, entry, bufferBuilder, vx + ox, vz + oz, vy, height, red, green, blue, a);
            }
        });

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    private void hline(
            VertexConsumer consumer, Matrix4f matrix4f, PoseStack.Pose entry, float x, float z, float x2, float z2, float y, float r, float g, float b, float a
    ) {
        float dx = x2 - x;
        float dz = z2 - z;
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        float nx = dx / length;
        float nz = dz / length;
        consumer.addVertex(matrix4f, x, y, z).setColor(r, g, b, a).setNormal(entry, nx, 0.0F, nz);
        consumer.addVertex(matrix4f, x2, y, z2).setColor(r, g, b, a).setNormal(entry, nx, 0.0F, nz);
    }

    public void fadeLine(
            Matrix4f matrix4f, PoseStack.Pose entry, VertexConsumer vertexConsumer, float x, float z, float y, float height, float r, float g, float b, float a
    ) {
        vertexConsumer.addVertex(matrix4f, x, y, z).setColor(r, g, b, a).setNormal(entry, 0.0F, 1.0F, 0.0F);
        vertexConsumer.addVertex(matrix4f, x, y + height, z).setColor(r, g, b, 0.0F).setNormal(entry, 0.0F, 1.0F, 0.0F);
    }

    private double dist(Vec3 vec3d, double x, double y, double z) {
        double dx = vec3d.x - x;
        double dy = vec3d.y - y;
        double dz = vec3d.z - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private float getAlpha(double dist) {
        return (float) Mth.clamp(1.0 - (dist - this.range.get() / 2.0) / (this.range.get() / 2.0), 0.0, 1.0);
    }

    private void findHoles(BlockPos center, int r, double radiusSq) {
        this.holes.clear();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (!(x * x + y * y + z * z > radiusSq)) {
                        BlockPos pos = center.offset(x, y, z);
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
