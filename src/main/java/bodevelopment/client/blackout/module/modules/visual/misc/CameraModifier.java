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
    public final Setting<Boolean> clip = this.sgGeneral.booleanSetting("Clip", true, ".");
    public final Setting<Double> cameraDist = this.sgGeneral.doubleSetting("Camera Dist", 4.0, 0.0, 20.0, 0.2, ".");
    public final Setting<Double> smoothTime = this.sgGeneral.doubleSetting("Smooth Time", 0.5, 0.0, 5.0, 0.05, ".");
    public final Setting<Boolean> noInverse = this.sgGeneral.booleanSetting("No Inverse", true, ".");
    public final Setting<Boolean> lockY = this.sgGeneral.booleanSetting("Lock Y", false, ".");
    public final Setting<Double> minY = this.sgGeneral.doubleSetting("Min Y", 0.0, -64.0, 300.0, 1.0, ".", this.lockY::get);
    public final Setting<Double> maxY = this.sgGeneral.doubleSetting("Max Y", 5.0, -64.0, 300.0, 1.0, ".", this.lockY::get);
    public final Setting<Boolean> smoothMove = this.sgGeneral.booleanSetting("Smooth Move", false, ".");
    public final Setting<Double> smoothSpeed = this.sgGeneral.doubleSetting("Smooth Speed", 5.0, 1.0, 10.0, 0.1, ".", this.smoothMove::get);
    public final Setting<Boolean> smoothF5 = this.sgGeneral.booleanSetting("Smooth F5", false, "Only is smooth in f5.");
    public double distProgress = 0.0;

    public CameraModifier() {
        super("Camera Modifier", ".", SubCategory.MISC_VISUAL, true);
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
