package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;

public class CustomChat extends Module {
    private static CustomChat INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Text");
    public final Setting<Boolean> blur = this.sgGeneral.b("Blur", true, ".");
    public final Setting<Boolean> background = this.sgGeneral.b("Background", true, ".");
    public final Setting<Boolean> shadow = this.sgGeneral.b("Shadow", true, ".", this.background::get);
    public final Setting<BlackOutColor> shadowColor = this.sgGeneral
            .c("Shadow Color", new BlackOutColor(0, 0, 0, 100), ".", () -> this.background.get() && this.shadow.get());
    public final Setting<BlackOutColor> bgColor = this.sgGeneral.c("Background Color", new BlackOutColor(0, 0, 0, 50), ".", this.background::get);

    public CustomChat() {
        super("Custom Chat", "Modifies Chat.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static CustomChat getInstance() {
        return INSTANCE;
    }
}
