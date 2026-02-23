package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.util.math.MathHelper;

public class Zoomify extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> zoomValue = this.sgGeneral.d("Zoom Value", 3.0, 1.0, 50.0, 0.1, "Base magnification level.");
    private final Setting<Double> smoothSpeed = this.sgGeneral.d("Smooth Speed", 0.15, 0.01, 1.0, 0.01, "The speed of the zoom transition.");
    private final Setting<Boolean> cinematic = this.sgGeneral.b("Cinematic Camera", true, "Enables vanilla smooth camera while zooming.");
    private final Setting<Boolean> mouseModifier = this.sgGeneral.b("Mouse Modifier", true, "Reduces mouse sensitivity as you zoom in.");

    private final Setting<Boolean> scroll = this.sgGeneral.b("Allow Scroll", true, "Allows changing magnification with the scroll wheel.");
    private final Setting<Double> scrollSpeed = this.sgGeneral.d("Scroll Speed", 1.1, 1.01, 2.0, 0.01, "Exponential multiplier for scrolling.", this.scroll::get);

    private double currentZoom = 1.0;
    private double scrollMultiplier = 1.0;
    private boolean wasCinematic = false;
    private float lastMouseSens = -1;

    private static Zoomify INSTANCE;

    public Zoomify() {
        super("Zoomify", "Advanced zoom with cinematic effects.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        this.currentZoom = 1.0;
        this.scrollMultiplier = 1.0;
        this.wasCinematic = BlackOut.mc.options.smoothCameraEnabled;
        this.lastMouseSens = BlackOut.mc.options.getMouseSensitivity().getValue().floatValue();
    }

    @Override
    public void onDisable() {
        BlackOut.mc.options.smoothCameraEnabled = wasCinematic;
        if (lastMouseSens != -1) {
            BlackOut.mc.options.getMouseSensitivity().setValue((double) lastMouseSens);
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null) return;

        double target = zoomValue.get() * scrollMultiplier;
        currentZoom = MathHelper.lerp(smoothSpeed.get(), currentZoom, target);

        if (cinematic.get()) {
            BlackOut.mc.options.smoothCameraEnabled = currentZoom > 1.1;
        }

        if (mouseModifier.get() && currentZoom > 1.1) {
            double sens = lastMouseSens / (currentZoom * 0.5);
            BlackOut.mc.options.getMouseSensitivity().setValue(sens);
        } else if (lastMouseSens != -1) {
            BlackOut.mc.options.getMouseSensitivity().setValue((double) lastMouseSens);
        }
    }

    @Event
    public void onScroll(MouseScrollEvent event) {
        if (!this.scroll.get() || BlackOut.mc.currentScreen != null) return;

        if (event.vertical > 0) {
            scrollMultiplier *= scrollSpeed.get();
        } else {
            scrollMultiplier /= scrollSpeed.get();
        }

        scrollMultiplier = MathHelper.clamp(scrollMultiplier, 0.1, 20.0);
    }

    public float getFov(float fov) {
        if (!this.enabled) return fov;
        return (float) (fov / currentZoom);
    }

    public static Zoomify getInstance() {
        return INSTANCE;
    }
}