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
    public final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgStyle, TextColorMode.Wave, () -> true, "Text");
    public final Setting<Boolean> selectorBar = this.sgStyle.b("Selector Bar", false, "");
    public final Setting<Integer> selectorGlow = this.sgStyle.i("Selector Glow", 0, 0, 5, 1, ".", this.selectorBar::get);
    public final Setting<BlackOutColor> selectorColor = this.sgStyle
            .c("Selector Color", new BlackOutColor(40, 40, 40, 255), "Color for the selector background");
    public final Setting<Double> fontScale = this.sgStyle.d("Font Scale", 1.0, 0.35, 2.0, 0.01, ".");
    public final Setting<SettingGroupMode> settingGroup = this.sgStyle.e("Setting Group", SettingGroupMode.Shadow, "");
    public final Setting<Double> logoAlpha = this.sgStyle.d("Logo Alpha", 0.15, 0.0, 1.0, 0.01, "");
    public final Setting<Double> logoScale = this.sgStyle.d("Logo Scale", 1.0, 0.9, 1.3, 0.01, "");
    public final Setting<Integer> blur = this.sgStyle.i("Blur", 0, 0, 20, 1, "");
    private final SettingGroup sgOpen = this.addGroup("Open");
    public final Setting<Boolean> centerX = this.sgOpen.b("Module Center X", true, "");
    public final Setting<Double> moduleX = this.sgOpen.d("Module X", 0.5, 0.0, 1.0, 0.01, "", () -> !this.centerX.get());
    public final Setting<Double> moduleY = this.sgOpen.d("Module Y", 0.5, 0.0, 1.0, 0.01, "");
    public final Setting<Double> moduleScale = this.sgOpen.d("Module Scale", 2.0, 0.1, 4.0, 0.1, "");
    public final Setting<Double> moduleHeight = this.sgOpen.d("Module height", 40.0, 25.0, 100.0, 1.0, "");
    private final SettingGroup sgClosed = this.addGroup("Closed");
    public final Setting<Boolean> centerXClosed = this.sgClosed.b("Closed Module Center X", true, "");
    public final Setting<Double> moduleXClosed = this.sgClosed.d("Closed Module X", 0.5, 0.0, 1.0, 0.01, "", () -> !this.centerXClosed.get());
    public final Setting<Double> moduleYClosed = this.sgClosed.d("Closed Module Y", 0.5, 0.0, 1.0, 0.01, "");
    public final Setting<Double> moduleScaleClosed = this.sgClosed.d("Closed Module Scale", 2.0, 0.1, 4.0, 0.1, "");
    public final Setting<Double> moduleHeightClosed = this.sgClosed.d("Closed Module height", 40.0, 25.0, 100.0, 1.0, "");

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
