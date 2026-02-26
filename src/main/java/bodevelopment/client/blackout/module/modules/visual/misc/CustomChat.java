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

    public final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Label Color");
    public final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a blur effect behind the chat window to improve legibility.");
    public final Setting<Boolean> background = this.sgGeneral.booleanSetting("Custom Background", true, "Enables a specialized background plate for the chat history.");
    public final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Drop Shadow", true, "Renders a soft shadow beneath the background plate.", this.background::get);
    public final Setting<BlackOutColor> shadowColor = this.sgGeneral.colorSetting("Shadow Color", new BlackOutColor(0, 0, 0, 100), "The color and opacity of the background shadow.", () -> this.background.get() && this.shadow.get());
    public final Setting<BlackOutColor> bgColor = this.sgGeneral.colorSetting("Plate Color", new BlackOutColor(0, 0, 0, 50), "The base color and transparency of the chat background.", this.background::get);

    public CustomChat() {
        super("Custom Chat", "Enhances the chat interface with customizable background geometry, blur effects, and font rendering options.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static CustomChat getInstance() {
        return INSTANCE;
    }
}
