package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ExtrapolationMap;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtrapolationSettings extends SettingsModule {
    private static ExtrapolationSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgLag = this.addGroup("Lag");
    private final SettingGroup sgRender = this.addGroup("Render");

    public final Setting<Boolean> stepPredict = this.sgGeneral.booleanSetting("Step Predict", true,
            "Predicts target's upward movement. Helps Aura and CrystalAura track targets as they step up blocks.");
    public final Setting<Double> minStep = this.sgGeneral.doubleSetting("Min Step", 0.6, 0.6, 3.0, 0.1,
            "The minimum height (in blocks) required for the algorithm to start calculating upward prediction.");
    public final Setting<Integer> stepTicks = this.sgGeneral.intSetting("Step Ticks", 40, 10, 100, 1,
            "The number of ticks to look ahead during upward movement. Higher values result in longer predictions.");
    public final Setting<Boolean> reverseStepPredict = this.sgGeneral.booleanSetting("Reverse Step Predict", true,
            "Predicts sudden downward movement. Essential for tracking targets falling off ledges or stairs.");
    public final Setting<Double> minReverseStep = this.sgGeneral.doubleSetting("Min Reverse Step", 0.6, 0.6, 3.0, 0.1,
            "The minimum vertical drop required to trigger downward prediction logic.");
    public final Setting<Integer> reverseStepTicks = this.sgGeneral.intSetting("Reverse Step Ticks", 20, 10, 100, 1,
            "The number of ticks to look ahead during downward movement.");
    public final Setting<Boolean> jumpPredict = this.sgGeneral.booleanSetting("Jump Predict", true,
            "Calculates the target's jump arc. Required for accurate aim while the target is airborne.");
    public final Setting<Integer> maxLag = this.sgLag.intSetting("Max Lag", 5, 0, 10, 1,
            "How long (in seconds) the client continues to predict movement after the server stops sending updates.");
    public final Setting<Boolean> extraExtrapolation = this.sgLag.booleanSetting("Extra Extrapolation", true,
            "Enables more aggressive calculation methods. Helps on low-TPS servers but may reduce visual smoothness.");
    private final Setting<Boolean> renderExtrapolation = this.sgRender.booleanSetting("Render Extrapolation", false,
            "Renders a line showing the predicted path in the world. Useful for debugging and fine-tuning your config.");
    private final Setting<Boolean> dashedLine = this.sgRender.booleanSetting("Dashed Line", false,
            "Changes the prediction line style from solid to dashed for better visibility.");
    private final Setting<BlackOutColor> lineColor = this.sgRender.colorSetting("Line Color", new BlackOutColor(255, 255, 255, 255),
            "The color of the extrapolation path line.");

    private final ExtrapolationMap extrapolationMap = new ExtrapolationMap();
    private final MatrixStack stack = new MatrixStack();

    public ExtrapolationSettings() {
        super("Extrapolation", false, true);
        INSTANCE = this;
    }

    public static ExtrapolationSettings getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null && this.renderExtrapolation.get()) {
            Map<Entity, Box> map = this.extrapolationMap.getMap();
            Map<Entity, List<Vec3d>> feet = new HashMap<>();
            map.clear();
            Managers.EXTRAPOLATION.getDataMap().forEach((player, data) -> {
                if (player.isAlive()) {
                    List<Vec3d> list = new ArrayList<>();
                    Box box = data.extrapolate(player, 20, b -> list.add(BoxUtils.feet(b)));
                    feet.put(player, list);
                    map.put(player, box);
                }
            });
            this.stack.push();
            Render3DUtils.setRotation(this.stack);
            Render3DUtils.start();
            feet.values().forEach(this::renderList);
            Render3DUtils.end();
            this.stack.pop();
        }
    }

    private void renderList(List<Vec3d> list) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(this.dashedLine.get() ? VertexFormat.DrawMode.DEBUG_LINES : VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix4f = this.stack.peek().getPositionMatrix();
        Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
        float red = this.lineColor.get().red / 255.0F;
        float green = this.lineColor.get().green / 255.0F;
        float blue = this.lineColor.get().blue / 255.0F;
        float alpha = this.lineColor.get().alpha / 255.0F;
        list.forEach(
                vec -> bufferBuilder.vertex(
                                matrix4f, (float) (vec.x - camPos.x), (float) (vec.y - camPos.y), (float) (vec.z - camPos.z)
                        )
                        .color(red, green, blue, alpha)
                        
        );
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
}
