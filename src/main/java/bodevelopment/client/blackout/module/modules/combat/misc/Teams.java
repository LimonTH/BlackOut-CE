package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TextColor;

public class Teams extends Module {
    private static Teams INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> colorCheck = this.sgGeneral.booleanSetting("Color Check", true, "Checks if the players tab color is the same as yours");

    public Teams() {
        super("Teams", "Tries to prevent hitting teammates", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static Teams getInstance() {
        return INSTANCE;
    }

    public boolean isTeammate(PlayerEntity player) {
        if (this.colorCheck.get()) {
            TextColor localColor = BlackOut.mc.player.getDisplayName().getStyle().getColor();
            TextColor playerColor = player.getDisplayName().getStyle().getColor();
            return localColor == playerColor;
        } else {
            return false;
        }
    }
}
