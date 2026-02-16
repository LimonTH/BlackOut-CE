package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Simulation extends Module {
    private static Simulation INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> autoMine = this.sgGeneral.b("Auto Mine", true, ".");
    private final Setting<Boolean> pickSwitch = this.sgGeneral.b("Pick Switch", true, ".");
    private final Setting<Boolean> quiverShoot = this.sgGeneral.b("Quiver Shoot", true, ".");
    private final Setting<Boolean> hitReset = this.sgGeneral.b("Hit Reset", true, "Resets weapon attack charge when sending an attack packet.");
    private final Setting<Boolean> stopSprint = this.sgGeneral.b("Stop Sprint", false, "Stops sprinting when sending an attack packet.");
    private final Setting<Double> stopManagerContainer = this.sgGeneral.d("Stop Manager Container", 0.5, 0.0, 5.0, 0.05, ".");

    public Simulation() {
        super("Simulation", "Simulates items spawning and blocks breaking when mining.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static Simulation getInstance() {
        return INSTANCE;
    }

    public boolean blocks() {
        return this.e(INSTANCE.autoMine);
    }

    public boolean pickSwitch() {
        return this.e(this.pickSwitch);
    }

    public boolean quiverShoot() {
        return this.e(this.quiverShoot);
    }

    public boolean hitReset() {
        return this.e(this.hitReset);
    }

    public boolean stopSprint() {
        return this.e(this.stopSprint);
    }

    public double managerStop() {
        return !this.enabled ? 0.0 : this.stopManagerContainer.get();
    }

    private boolean e(Setting<Boolean> s) {
        return this.enabled && s.get();
    }
}
