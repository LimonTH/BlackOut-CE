package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Simulation extends Module {
    private static Simulation INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> autoMine = this.sgGeneral.booleanSetting("Predict Mining", true, "Simulates block breaking and item drops client-side for a more responsive mining experience.");
    private final Setting<Boolean> pickSwitch = this.sgGeneral.booleanSetting("Predict Tool Swap", true, "Simulates the tool switching process to reduce perceived visual latency.");
    private final Setting<Boolean> quiverShoot = this.sgGeneral.booleanSetting("Predict Quiver", true, "Simulates arrow consumption and projectile launching when using Quiver.");
    private final Setting<Boolean> hitReset = this.sgGeneral.booleanSetting("Attack Reset", true, "Resets the visual weapon swing and attack charge locally when a packet is sent.");
    private final Setting<Boolean> stopSprint = this.sgGeneral.booleanSetting("Sprint Stop", false, "Simulates the cessation of sprinting when executing an attack.");
    private final Setting<Double> stopManagerContainer = this.sgGeneral.doubleSetting("Manager Pause Delay", 0.5, 0.0, 5.0, 0.05, "The duration in seconds to suspend the Manager module after interacting with a container.");

    public Simulation() {
        super("Simulation", "Manages client-side predictions and local state simulations to synchronize visuals with server-side actions.", SubCategory.MISC, false);
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
