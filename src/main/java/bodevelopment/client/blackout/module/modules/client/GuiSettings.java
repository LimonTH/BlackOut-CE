package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.enums.TextColorMode;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;

public class GuiSettings extends SettingsModule {
    private static GuiSettings INSTANCE;
    private final SettingGroup sgStyle = this.addGroup("Style");
    private final SettingGroup sgOpen = this.addGroup("Open");
    private final SettingGroup sgClosed = this.addGroup("Closed");

    public final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgStyle, TextColorMode.Wave, () -> true,
            "Text");
    public final Setting<Boolean> selectorBar = this.sgStyle.booleanSetting("Selector Bar", false,
            "Displays a decorative bar next to the currently selected module or setting.");
    public final Setting<Integer> selectorGlow = this.sgStyle.intSetting("Selector Glow", 0, 0, 5, 1,
            "The intensity of the outer glow effect for the selection bar.", this.selectorBar::get);
    public final Setting<BlackOutColor> selectorColor = this.sgStyle.colorSetting("Selector Color", new BlackOutColor(40, 40, 40, 255),
            "The background color of the selection highlight in the GUI.");
    public final Setting<Double> fontScale = this.sgStyle.doubleSetting("Font Scale", 1.0, 0.35, 2.0, 0.01,
            "Global multiplier for text size. Lower values allow for more information, while higher values improve readability.");
    public final Setting<SettingGroupMode> settingGroup = this.sgStyle.enumSetting("Setting Group", SettingGroupMode.Shadow,
            "Determines the visual style and separation method for groups of settings.");
    public final Setting<Double> logoAlpha = this.sgStyle.doubleSetting("Logo Alpha", 0.15, 0.0, 1.0, 0.01,
            "Sets the transparency level of the background logo watermark.");
    public final Setting<Double> logoScale = this.sgStyle.doubleSetting("Logo Scale", 1.0, 0.9, 1.3, 0.01,
            "Adjusts the size of the background logo relative to its original dimensions.");
    public final Setting<Integer> blur = this.sgStyle.intSetting("Blur", 0, 0, 20, 1,
            "Applies a Gaussian blur effect behind the GUI panels to enhance focus on the interface.");

    // TODO: moduleY не используется
    public final Setting<Boolean> centerX = this.sgOpen.booleanSetting("Module Center X", true,
            "Automatically snaps the GUI to the horizontal center of the screen when opened.");
    public final Setting<Double> moduleX = this.sgOpen.doubleSetting("Module X", 0.5, 0.0, 1.0, 0.01,
            "The horizontal screen coordinate for the GUI when not centered.", () -> !this.centerX.get());
    public final Setting<Double> moduleY = this.sgOpen.doubleSetting("Module Y", 0.5, 0.0, 1.0, 0.01,
            "The vertical screen coordinate for the GUI when it is fully opened.");
    public final Setting<Double> moduleScale = this.sgOpen.doubleSetting("Module Scale", 2.0, 0.1, 4.0, 0.1,
            "The overall size of the GUI panels when the menu is active.");
    public final Setting<Double> moduleHeight = this.sgOpen.doubleSetting("Module height", 40.0, 25.0, 100.0, 1.0,
            "The default height for each individual module entry in the list.");

    // TODO: moduleYClosed не используется
    public final Setting<Boolean> centerXClosed = this.sgClosed.booleanSetting("Closed Module Center X", true,
            "Whether the GUI should animate towards the horizontal center of the screen when closing.");
    public final Setting<Double> moduleXClosed = this.sgClosed.doubleSetting("Closed Module X", 0.5, 0.0, 1.0, 0.01,
            "The target X-coordinate for the closing animation when not centered.", () -> !this.centerXClosed.get());
    public final Setting<Double> moduleYClosed = this.sgClosed.doubleSetting("Closed Module Y", 0.5, 0.0, 1.0, 0.01,
            "The target Y-coordinate the GUI moves towards as it fades out.");
    public final Setting<Double> moduleScaleClosed = this.sgClosed.doubleSetting("Closed Module Scale", 2.0, 0.1, 4.0, 0.1,
            "The target scale for the modules when the GUI is closed. Can be used to create a 'shrink' or 'expand' effect.");
    public final Setting<Double> moduleHeightClosed = this.sgClosed.doubleSetting("Closed Module height", 40.0, 25.0, 100.0, 1.0,
            "The height each module entry reaches at the end of the closing animation.");
    public GuiSettings() {
        super("GUI", true, true);
        INSTANCE = this;
    }

    public static GuiSettings getInstance() {
        return INSTANCE;
    }

    public enum SettingGroupMode {
        Line(40.0F),
        Shadow(45.0F),
        Quad(50.0F),
        None(40.0F);

        private final float height;

        SettingGroupMode(float height) {
            this.height = height;
        }

        public float getHeight() {
            return this.height;
        }
    }
}
