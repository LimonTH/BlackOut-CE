package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Reach extends Module {
    private static Reach INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Double> entityReach = this.sgGeneral.doubleSetting("Entity Reach", 3.0, 0.0, 10.0, 0.1, ".");
    public final Setting<Double> blockReach = this.sgGeneral.doubleSetting("Block Reach", 4.5, 0.0, 10.0, 0.1, ".");

    public Reach() {
        super("Reach", "Modifies interaction range for blocks and entities.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static Reach getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return String.format("E: %.1f B: %.1f", this.entityReach.get(), this.blockReach.get());
    }
}
