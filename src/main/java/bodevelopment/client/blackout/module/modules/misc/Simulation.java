package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Simulation extends Module {
    private static Simulation INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> autoMine = this.sgGeneral.booleanSetting("Auto Mine", true, ".");
    private final Setting<Boolean> pickSwitch = this.sgGeneral.booleanSetting("Pick Switch", true, ".");
    private final Setting<Boolean> quiverShoot = this.sgGeneral.booleanSetting("Quiver Shoot", true, ".");
    private final Setting<Boolean> hitReset = this.sgGeneral.booleanSetting("Hit Reset", true, "Resets weapon attack charge when sending an attack packet.");
    private final Setting<Boolean> stopSprint = this.sgGeneral.booleanSetting("Stop Sprint", false, "Stops sprinting when sending an attack packet.");
    private final Setting<Double> stopManagerContainer = this.sgGeneral.doubleSetting("Stop Manager Container", 0.5, 0.0, 5.0, 0.05, ".");

    public Simulation() {
        super("Simulation", "Simulates items spawning and blocks breaking when mining.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static Simulation getInstance() {
        return INSTANCE;
    }

    public boolean blocks() {
        return this.enumSetting(INSTANCE.autoMine);
    }

    public boolean pickSwitch() {
        return this.enumSetting(this.pickSwitch);
    }

    public boolean quiverShoot() {
        return this.enumSetting(this.quiverShoot);
    }

    public boolean hitReset() {
        return this.enumSetting(this.hitReset);
    }

    public boolean stopSprint() {
        return this.enumSetting(this.stopSprint);
    }

    public double managerStop() {
        return !this.enabled ? 0.0 : this.stopManagerContainer.get();
    }

    private boolean enumSetting(Setting<Boolean> s) {
        return this.enabled && s.get();
    }
}
