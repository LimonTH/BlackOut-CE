package bodevelopment.client.blackout.module.setting.multisettings;

import bodevelopment.client.blackout.enums.BackgroundType;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

public class BackgroundMultiSetting {
    private final Setting<BackgroundType> mode;
    private final Setting<BlackOutColor> roundedColor;
    private final Setting<BlackOutColor> shadowColor;
    private final Setting<BlackOutColor> secondaryColor;
    private final Setting<Boolean> shadow;
    private final Setting<Double> speed;

    private BackgroundMultiSetting(
            SettingGroup sg, BackgroundType dm, BlackOutColor rc, BlackOutColor sc, BlackOutColor shdw, SingleOut<Boolean> visible, String name
    ) {
        String text = name == null ? "Background" : name;

        this.mode = sg.enumSetting(text + " Type", dm,
                "The rendering style for the background: Static (solid) or Animated (gradient shift).");
        this.roundedColor = sg.colorSetting(text + " Color", rc,
                "The primary color of the background element.",
                () -> (this.mode.get() == BackgroundType.Static || this.mode.get() == BackgroundType.Animated) && visible.get());
        this.secondaryColor = sg.colorSetting(text + " Secondary Color", sc,
                "The target color for the animation cycle when using Animated mode.",
                () -> this.mode.get() == BackgroundType.Animated && visible.get());
        this.shadow = sg.booleanSetting(text + " Shadow", true,
                "Whether to render a soft shadow effect around the edges.");
        this.shadowColor = sg.colorSetting(text + " Shadow Color", shdw,
                "The color and transparency of the shadow glow.",
                () -> this.mode.get() == BackgroundType.Static && visible.get() && this.shadow.get());
        this.speed = sg.doubleSetting(text + " Speed", 1.0, 0.1, 10.0, 0.1,
                "How fast the background colors transition in Animated mode.",
                () -> this.mode.get() == BackgroundType.Animated && visible.get());
    }

    public static BackgroundMultiSetting of(SettingGroup sg, String name) {
        return of(sg, () -> true, name);
    }

    public static BackgroundMultiSetting of(SettingGroup sg, SingleOut<Boolean> visible, String name) {
        return of(sg, BackgroundType.Static, visible, name);
    }

    public static BackgroundMultiSetting of(SettingGroup sg, BackgroundType dm, SingleOut<Boolean> visible, String name) {
        return of(sg, dm, new BlackOutColor(0, 0, 0, 50), new BlackOutColor(25, 25, 25, 50), new BlackOutColor(0, 0, 0, 100), visible, name);
    }

    public static BackgroundMultiSetting of(
            SettingGroup sg, BackgroundType dm, BlackOutColor rc, BlackOutColor sc, BlackOutColor shdw, SingleOut<Boolean> visible, String name
    ) {
        return new BackgroundMultiSetting(sg, dm, rc, sc, shdw, visible, name);
    }

    public BlackOutColor getRoundedColor() {
        return this.roundedColor.get();
    }

    public BlackOutColor getSecondaryColor() {
        return this.secondaryColor.get();
    }

    public Boolean isAnimated() {
        return this.mode.get() == BackgroundType.Animated;
    }

    public Boolean isStatic() {
        return this.mode.get() == BackgroundType.Static;
    }

    public void render(MatrixStack stack, float x, float y, float w, float h, float r, float sr) {
        ThemeSettings themeSettings = ThemeSettings.getInstance();
        switch (this.mode.get()) {
            case Static:
                RenderUtils.rounded(stack, x, y, w, h, r, this.shadow.get() ? sr : 0.0F, this.roundedColor.get().getRGB(), this.shadowColor.get().getRGB());
                break;
            case Animated:
                RenderUtils.tenaRounded(
                        stack,
                        x,
                        y,
                        w,
                        h,
                        r,
                        this.shadow.get() ? sr : 0.0F,
                        this.roundedColor.get().getRGB(),
                        this.secondaryColor.get().getRGB(),
                        this.speed.get().floatValue()
                );
        }
    }
}
