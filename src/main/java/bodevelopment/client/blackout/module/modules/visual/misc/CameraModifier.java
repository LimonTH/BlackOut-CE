package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;

public class CameraModifier extends Module {
    private static CameraModifier INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> clip = this.sgGeneral.booleanSetting("Camera Clip", true, "Allows the third-person camera to pass through solid blocks instead of colliding.");
    public final Setting<Double> cameraDist = this.sgGeneral.doubleSetting("View Distance", 4.0, 0.0, 20.0, 0.2, "The maximum separation distance between the camera and the player model.");
    public final Setting<Double> smoothTime = this.sgGeneral.doubleSetting("Interpolation Period", 0.5, 0.0, 5.0, 0.05, "The duration of the transition animation when switching view modes.");
    public final Setting<Boolean> noInverse = this.sgGeneral.booleanSetting("Prevent Inversion", true, "Disables the inverted front-facing third-person perspective.");
    public final Setting<Boolean> lockY = this.sgGeneral.booleanSetting("Clamp Vertical Axis", false, "Restricts the camera's height within a specific coordinate range.");
    public final Setting<Double> minY = this.sgGeneral.doubleSetting("Lower Y Bound", 0.0, -64.0, 300.0, 1.0, "The minimum world height the camera is permitted to reach.", this.lockY::get);
    public final Setting<Double> maxY = this.sgGeneral.doubleSetting("Upper Y Bound", 5.0, -64.0, 300.0, 1.0, "The maximum world height the camera is permitted to reach.", this.lockY::get);
    public final Setting<Boolean> smoothMove = this.sgGeneral.booleanSetting("Cinematic Motion", false, "Applies smooth interpolation to camera movement for a fluid visual experience.");
    public final Setting<Double> smoothSpeed = this.sgGeneral.doubleSetting("Motion Sensitivity", 5.0, 1.0, 10.0, 0.1, "Determines the tracking speed of the cinematic camera.", this.smoothMove::get);
    public final Setting<Boolean> smoothF5 = this.sgGeneral.booleanSetting("Perspective Lock", false, "Restricts cinematic motion effects exclusively to third-person view modes.");

    public double distProgress = 0.0;

    public CameraModifier() {
        super("Camera Modifier", "Extends and enhances the third-person camera system with distance control, smoothing, and clipping overrides.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static CameraModifier getInstance() {
        return INSTANCE;
    }

    public void updateDistance(boolean thirdPerson, double delta) {
        this.distProgress = thirdPerson ? Math.min(this.distProgress + delta, this.smoothTime.get()) : 0.0;
    }

    public boolean shouldSmooth(boolean thirdPerson) {
        return this.smoothMove.get() && (!this.smoothF5.get() || thirdPerson);
    }

    public double getCameraDistance() {
        return AnimUtils.easeOutCubic(OLEPOSSUtils.safeDivide(this.distProgress, this.smoothTime.get())) * this.cameraDist.get();
    }
}
