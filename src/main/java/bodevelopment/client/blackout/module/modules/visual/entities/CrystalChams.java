package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

public class CrystalChams extends Module {
    private static CrystalChams INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgSync = this.addGroup("Sync");

    public final Setting<Boolean> spawnAnimation = this.sgGeneral.booleanSetting("Growth Animation", false, "Smoothly scales the crystal up upon its initial instantiation.");
    public final Setting<Double> animationTime = this.sgGeneral.doubleSetting("Interpolation Period", 0.5, 0.0, 1.0, 0.01, "The duration in seconds for the spawn animation to complete.", this.spawnAnimation::get);
    public final Setting<Double> scale = this.sgGeneral.doubleSetting("Geometry Scale", 1.0, 0.0, 10.0, 0.1, "The global size multiplier for the End Crystal's rendering cubes.");
    public final Setting<Double> bounce = this.sgGeneral.doubleSetting("Vertical Amplitude", 0.5, 0.0, 10.0, 0.1, "The maximum height of the floating oscillation animation.");
    public final Setting<Double> bounceSpeed = this.sgGeneral.doubleSetting("Oscillation Frequency", 1.0, 0.0, 10.0, 0.1, "The rate at which the crystal moves up and down.");
    public final Setting<Double> rotationSpeed = this.sgGeneral.doubleSetting("Angular Velocity", 1.0, 0.0, 10.0, 0.1, "The speed at which the crystal's interior and exterior layers rotate.");
    public final Setting<Double> y = this.sgGeneral.doubleSetting("Vertical Offset", 0.0, -5.0, 5.0, 0.1, "Adjusts the base Y-level elevation of the entity model.");
    public final Setting<RenderShape> coreRenderShape = this.sgGeneral.enumSetting("Inner Mesh Type", RenderShape.Full, "Defines the primitive mesh rendering mode for the central core.");
    public final Setting<BlackOutColor> coreLineColor = this.sgGeneral.colorSetting("Inner Wireframe Color", new BlackOutColor(255, 0, 0, 255), "The color applied to the edges of the core cube.");
    public final Setting<BlackOutColor> coreSideColor = this.sgGeneral.colorSetting("Inner Face Color", new BlackOutColor(255, 0, 0, 50), "The color applied to the polygon faces of the core cube.");
    public final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Median Mesh Type", RenderShape.Full, "Defines the primitive mesh rendering mode for the middle layer.");
    public final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Median Wireframe Color", new BlackOutColor(255, 0, 0, 255), "The color applied to the edges of the middle cube.");
    public final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Median Face Color", new BlackOutColor(255, 0, 0, 50), "The color applied to the polygon faces of the middle cube.");
    public final Setting<RenderShape> outerRenderShape = this.sgGeneral.enumSetting("Shell Mesh Type", RenderShape.Full, "Defines the primitive mesh rendering mode for the outermost layer.");
    public final Setting<BlackOutColor> outerLineColor = this.sgGeneral.colorSetting("Shell Wireframe Color", new BlackOutColor(255, 0, 0, 255), "The color applied to the edges of the outer shell.");
    public final Setting<BlackOutColor> outerSideColor = this.sgGeneral.colorSetting("Shell Face Color", new BlackOutColor(255, 0, 0, 50), "The color applied to the polygon faces of the outer shell.");

    public final Setting<Boolean> bounceSync = this.sgSync.booleanSetting("Global Oscillation Sync", false, "Synchronizes the vertical bounce timing across all rendered crystals.");
    public final Setting<Boolean> rotationSync = this.sgSync.booleanSetting("Global Angular Sync", false, "Synchronizes the rotation timing across all rendered crystals.");

    private final Box box = new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
    public int age = 0;

    public CrystalChams() {
        super("Crystal Chams", "Customizes the geometric rendering and animations of End Crystal entities for enhanced visibility.", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static CrystalChams getInstance() {
        return INSTANCE;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.age++;
    }

    public void renderBox(MatrixStack stack, int id) {
        BlackOutColor sideColor = this.getSideColor(id);
        BlackOutColor lineColor = this.getLineColor(id);
        RenderShape shape = this.getShape(id);

        Render3DUtils.boxRaw(stack, box, sideColor, lineColor, shape);
    }

    private BlackOutColor getLineColor(int id) {
        return switch (id) {
            case 0 -> this.coreLineColor.get();
            case 1 -> this.lineColor.get();
            default -> this.outerLineColor.get();
        };
    }

    private BlackOutColor getSideColor(int id) {
        return switch (id) {
            case 0 -> this.coreSideColor.get();
            case 1 -> this.sideColor.get();
            default -> this.outerSideColor.get();
        };
    }

    private RenderShape getShape(int id) {
        return switch (id) {
            case 0 -> this.coreRenderShape.get();
            case 1 -> this.renderShape.get();
            default -> this.outerRenderShape.get();
        };
    }
}

