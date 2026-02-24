package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class AntiHunger extends Module {
    private static AntiHunger INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> sprint = this.sgGeneral.booleanSetting("Cancel Sprint Packets", true, "Suppresses sprint state synchronization with the server to minimize exhaustion.");
    public final Setting<Boolean> moving = this.sgGeneral.booleanSetting("On-Ground Spoofing", true, "Spoofs the player's ground state to prevent the server from calculating movement-based hunger loss.");

    public AntiHunger() {
        super("Anti Hunger", "Reduces or eliminates hunger depletion by intercepting movement and action packets.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static AntiHunger getInstance() {
        return INSTANCE;
    }
}
