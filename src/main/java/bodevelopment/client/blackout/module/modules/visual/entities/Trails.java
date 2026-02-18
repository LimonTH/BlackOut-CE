package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.functional.DoubleFunction;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trails extends Module {
    private static Trails INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");
    public final Setting<ColorMode> colorMode = this.sgColor.e("Color Mode", ColorMode.Custom, "What color to use");
    private final Setting<Double> speed = this.sgColor.d("Wave Speed", 1.0, 0.1, 10.0, 0.1, ".", () -> this.colorMode.get() == ColorMode.Wave);
    private final Setting<Double> saturation = this.sgColor
            .d("Rainbow Saturation", 0.8, 0.0, 1.0, 0.1, ".", () -> this.colorMode.get() == ColorMode.Rainbow);
    private final Setting<BlackOutColor> clr = this.sgColor
            .c("Line Color", new BlackOutColor(255, 255, 255, 255), ".", () -> this.colorMode.get() != ColorMode.Rainbow);
    private final Setting<BlackOutColor> clr1 = this.sgColor
            .c("Wave Color", new BlackOutColor(175, 175, 175, 255), ".", () -> this.colorMode.get() != ColorMode.Rainbow);
    private final Setting<List<EntityType<?>>> entities = this.sgGeneral.el("Entities", "", EntityType.ENDER_PEARL);
    private final Setting<HeightMode> renderHeight = this.sgGeneral.e("Render Height", HeightMode.Feet, "");
    private final Setting<Double> renderTime = this.sgGeneral.d("Render Time", 0.0, 0.0, 10.0, 0.1, ".");
    private final Setting<Double> fadeTime = this.sgGeneral.d("Fade Time", 5.0, 0.0, 10.0, 0.1, ".");
    private final Setting<Double> maxFrequency = this.sgGeneral.d("Max Frequency", 40.0, 1.0, 100.0, 1.0, ".");
    private final Setting<Double> lineWidth = this.sgGeneral.d("Line Width", 2.5, 0.5, 5.0, 0.05, ".");
    private final Map<Entity, Line> map = new HashMap<>();
    private long prevAdd = 0L;

    public Trails() {
        super("Trails", "Renders lines behind entities.", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static Trails getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.addPositions(event.tickDelta);
            MatrixStack stack = Render3DUtils.matrices;
            Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
            stack.push();
            Render3DUtils.setRotation(stack);
            Render3DUtils.start();
            this.map.forEach((entity, line) -> {
                Color lineColor = this.getColor();
                line.render(stack, camPos, lineColor, this.renderTime.get(), this.fadeTime.get());
            });
            Render3DUtils.end();
            stack.pop();
        }
    }

    private void addPositions(double tickDelta) {
        if (!(System.currentTimeMillis() - this.prevAdd < 1000.0 / this.maxFrequency.get())) {
            this.prevAdd = System.currentTimeMillis();
            BlackOut.mc.world.getEntities().forEach(entity -> {
                if (this.entities.get().contains(entity.getType())) {
                    if (this.map.containsKey(entity)) {
                        this.map.get(entity).positions.add(new Pair<>(this.renderHeight.get().function.apply(entity, tickDelta), System.currentTimeMillis()));
                    } else {
                        Line line = new Line();
                        line.positions.add(new Pair<>(this.renderHeight.get().function.apply(entity, tickDelta), System.currentTimeMillis()));
                        this.map.put(entity, line);
                    }
                }
            });
            this.map.entrySet().removeIf(entry -> entry.getValue().positions.isEmpty());
        }
    }

    private Color getColor() {
        Color color = Color.WHITE;
        switch (this.colorMode.get()) {
            case Custom:
                color = this.clr.get().getColor();
                break;
            case Rainbow:
                int rainbowColor = ColorUtils.getRainbow(4.0F, this.saturation.get().floatValue(), 1.0F, 150L);
                color = new Color(rainbowColor >> 16 & 0xFF, rainbowColor >> 8 & 0xFF, rainbowColor & 0xFF, this.clr.get().alpha);
                break;
            case Wave:
                color = ColorUtils.getWave(this.clr.get().getColor(), this.clr1.get().getColor(), this.speed.get(), 1.0, 1);
        }

        return color;
    }

    public enum ColorMode {
        Rainbow,
        Custom,
        Wave
    }

    public enum HeightMode {
        Feet(OLEPOSSUtils::getLerpedPos),
        Middle((entity, tickDelta) -> OLEPOSSUtils.getLerpedPos(entity, tickDelta).add(0.0, entity.getBoundingBox().getLengthY() / 2.0, 0.0));

        private final DoubleFunction<Entity, Double, Vec3d> function;

        HeightMode(DoubleFunction<Entity, Double, Vec3d> function) {
            this.function = function;
        }
    }

    private static class Line {
        private final List<Pair<Vec3d, Long>> positions = new ArrayList<>();

        private void render(MatrixStack stack, Vec3d camPos, Color color, double renderTime, double fadeTime) {
            this.positions.removeIf(pairx -> System.currentTimeMillis() - pairx.getRight() > (renderTime + fadeTime) * 1000.0);
            if (this.positions.size() >= 2) {
                RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
                RenderSystem.lineWidth(Trails.getInstance().lineWidth.get().floatValue());

                RenderSystem.getModelViewStack().pushMatrix();
                RenderSystem.getModelViewStack().identity();
                RenderSystem.applyModelViewMatrix();

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

                MatrixStack.Entry entry = stack.peek();
                Matrix4f matrix4f = entry.getPositionMatrix();
                float r = color.getRed() / 255.0F;
                float g = color.getGreen() / 255.0F;
                float b = color.getBlue() / 255.0F;
                float a = color.getAlpha() / 255.0F;

                for (int i = 0; i < this.positions.size() - 1; i++) {
                    Pair<Vec3d, Long> pair = this.positions.get(i);
                    Pair<Vec3d, Long> nextPair = this.positions.get(i + 1);
                    Vec3d vec = pair.getLeft();
                    Vec3d nextVec = nextPair.getLeft();
                    float alpha = this.getAlpha(pair.getRight(), renderTime, fadeTime) * a;
                    float alpha2 = this.getAlpha(nextPair.getRight(), renderTime, fadeTime) * a;
                    Vec3d diff = nextVec.subtract(vec);
                    Vec3d normal = diff.multiply(1.0 / diff.length());
                    bufferBuilder.vertex(
                                    matrix4f,
                                    (float) (vec.x - camPos.x),
                                    (float) (vec.y - camPos.y),
                                    (float) (vec.z - camPos.z)
                            )
                            .color(r, g, b, alpha)
                            .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
                            ;
                    bufferBuilder.vertex(
                                    matrix4f,
                                    (float) (nextVec.x - camPos.x),
                                    (float) (nextVec.y - camPos.y),
                                    (float) (nextVec.z - camPos.z)
                            )
                            .color(r, g, b, alpha2)
                            .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
                            ;
                }

                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

                RenderSystem.getModelViewStack().popMatrix();
                RenderSystem.applyModelViewMatrix();
            }
        }

        private float getAlpha(long time, double renderTime, double fadeTime) {
            return (float) (1.0 - Math.max((System.currentTimeMillis() - time) / 1000.0 - renderTime, 0.0) / fadeTime);
        }
    }
}
