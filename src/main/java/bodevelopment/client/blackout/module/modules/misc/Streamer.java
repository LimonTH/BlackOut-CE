package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.client.gui.screen.TitleScreen;

public class Streamer extends Module {
    private static Streamer INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<String> spoofedName = this.sgGeneral.s("Spoofed Name", "Luhposu", "");
    public final Setting<Boolean> skin = this.sgGeneral.b("Skin", true, ".");

    public Streamer() {
        super("Streamer", "Spoofs stuff to not reveal your account.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static Streamer getInstance() {
        return INSTANCE;
    }

    public String replace(String string) {
        if (BlackOut.mc.currentScreen instanceof TitleScreen
                && MainMenu.getInstance().getSwitchProgress() > 0.5F) {
            return string;
        }

        String currentName = BlackOut.mc.getSession().getUsername();
        if (currentName == null || string == null) return string;

        return string.replace(currentName, this.spoofedName.get());
    }
}
