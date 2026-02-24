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
    private static Zoomify INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> zoomValue = this.sgGeneral.doubleSetting("Magnification", 3.0, 1.0, 50.0, 0.1, "The base multiplier for the camera zoom level.");
    private final Setting<Boolean> hideHands = this.sgGeneral.booleanSetting("Hide Hands", true, "Removes your hands and held items from the screen while zooming.");
    private final Setting<Boolean> cleanScreen = this.sgGeneral.booleanSetting("Hide Overlay", false, "Hides the HUD, crosshair, and other screen overlays for a clearer view.");
    private final Setting<Double> smoothSpeed = this.sgGeneral.doubleSetting("Interpolation Speed", 0.15, 0.01, 1.0, 0.01, "The speed at which the camera transitions to the target zoom level.");
    private final Setting<Boolean> cinematic = this.sgGeneral.booleanSetting("Cinematic Interpolation", true, "Enables smooth, weighted camera movement to prevent jerky motion while magnified.");
    private final Setting<Boolean> mouseModifier = this.sgGeneral.booleanSetting("Sensitivity Scaling", true, "Dynamically lowers mouse sensitivity based on the current zoom level to improve aiming precision.");
    private final Setting<Boolean> scroll = this.sgGeneral.booleanSetting("Variable Zoom", true, "Allows you to adjust the magnification level in real-time using the mouse scroll wheel.");
    private final Setting<Double> scrollSpeed = this.sgGeneral.doubleSetting("Scroll Sensitivity", 1.1, 1.01, 2.0, 0.01, "The factor by which the zoom level changes per scroll tick.", this.scroll::get);

    private double currentZoom = 1.0;
    private double scrollMultiplier = 1.0;
    private boolean wasCinematic = false;
    private float lastMouseSens = -1;

    public Zoomify() {
        super("Zoomify", "Provides advanced camera magnification features with smooth transitions and cinematic controls.", SubCategory.MISC, true);
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
        if (this.scroll.get() || this.cleanScreen.get()) {
            event.cancel();
        }
        if (!this.scroll.get() || BlackOut.mc.currentScreen != null) return;

        if (event.vertical > 0) {
            scrollMultiplier *= scrollSpeed.get();
        } else {
            scrollMultiplier /= scrollSpeed.get();
        }

        scrollMultiplier = MathHelper.clamp(scrollMultiplier, 0.1, 20.0);
    }

    public float getZoomFactor(float fov) {
        if (!this.enabled) return fov;
        return (float) (fov / currentZoom);
    }

    public boolean shouldHideHands() {
        return this.enabled && hideHands.get() && currentZoom > 1.1;
    }

    public boolean isCleanScreen() {
        return this.enabled && cleanScreen.get() && currentZoom > 1.1;
    }

    public static Zoomify getInstance() {
        return INSTANCE;
    }
}