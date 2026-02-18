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
    public final Setting<Boolean> spawnAnimation = this.sgGeneral.b("Spawn Animation", false, "Try it and see.");
    public final Setting<Double> animationTime = this.sgGeneral.d("Animation Time", 0.5, 0.0, 1.0, 0.01, "Try it and see.", this.spawnAnimation::get);
    public final Setting<Double> scale = this.sgGeneral.d("Scale", 1.0, 0.0, 10.0, 0.1, "Try it and see.");
    public final Setting<Double> bounce = this.sgGeneral.d("Bounce", 0.5, 0.0, 10.0, 0.1, "Try it and see.");
    public final Setting<Double> bounceSpeed = this.sgGeneral.d("Bounce Speed", 1.0, 0.0, 10.0, 0.1, "Try it and see.");
    public final Setting<Double> rotationSpeed = this.sgGeneral.d("Rotation Speed", 1.0, 0.0, 10.0, 0.1, "Try it and see.");
    public final Setting<Double> y = this.sgGeneral.d("Y", 0.0, -5.0, 5.0, 0.1, "Try it and see.");
    public final Setting<RenderShape> coreRenderShape = this.sgGeneral.e("Core Render Shape", RenderShape.Full, "Try it and see.");
    public final Setting<BlackOutColor> coreLineColor = this.sgGeneral.c("Core Line Color", new BlackOutColor(255, 0, 0, 255), "Try it and see.");
    public final Setting<BlackOutColor> coreSideColor = this.sgGeneral.c("Core Side Color", new BlackOutColor(255, 0, 0, 50), "Try it and see.");
    public final Setting<RenderShape> renderShape = this.sgGeneral.e("Middle Render Shape", RenderShape.Full, "Try it and see.");
    public final Setting<BlackOutColor> lineColor = this.sgGeneral.c("Middle Line Color", new BlackOutColor(255, 0, 0, 255), "Try it and see.");
    public final Setting<BlackOutColor> sideColor = this.sgGeneral.c("Middle Side Color", new BlackOutColor(255, 0, 0, 50), "Try it and see.");
    public final Setting<RenderShape> outerRenderShape = this.sgGeneral.e("Outer Render Shape", RenderShape.Full, "Try it and see.");
    public final Setting<BlackOutColor> outerLineColor = this.sgGeneral.c("Outer Line Color", new BlackOutColor(255, 0, 0, 255), "Try it and see.");
    public final Setting<BlackOutColor> outerSideColor = this.sgGeneral.c("Outer Side Color", new BlackOutColor(255, 0, 0, 50), "Try it and see.");
    public final Setting<Boolean> bounceSync = this.sgSync.b("Bounce Sync", false, "Try it and see.");
    public final Setting<Boolean> rotationSync = this.sgSync.b("Rotation Sync", false, "Try it and see.");
    private final Box box = new Box(-0.25, -0.25, -0.25, 0.25, 0.25, 0.25);
    public int age = 0;

    public CrystalChams() {
        super("Crystal Chams", "Modifies the appearance of crystals.", SubCategory.ENTITIES, true);
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

        Render3DUtils.box(stack, box, sideColor, lineColor, shape);
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

