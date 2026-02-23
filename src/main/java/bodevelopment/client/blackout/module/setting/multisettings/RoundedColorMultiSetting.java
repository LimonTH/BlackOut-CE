package bodevelopment.client.blackout.module.setting.multisettings;

import bodevelopment.client.blackout.enums.RoundedColorMode;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

public class RoundedColorMultiSetting {
    private final Setting<RoundedColorMode> mode;
    private final Setting<BlackOutColor> roundedColor;
    private final Setting<BlackOutColor> shadowColor;
    private final Setting<BlackOutColor> waveColor;
    private final Setting<Double> saturation;
    private final Setting<Double> frequency;
    private final Setting<Double> speed;

    private RoundedColorMultiSetting(SettingGroup sg, RoundedColorMode dm, BlackOutColor dt, BlackOutColor dw, SingleOut<Boolean> visible, String name) {
        String text = name == null ? "Rounded" : name;
        this.mode = sg.e(text + " Color Mode", dm, ".");
        this.roundedColor = sg.c(
                text + " Color", dt, ".", () -> (this.mode.get() == RoundedColorMode.Static || this.mode.get() == RoundedColorMode.Wave) && visible.get()
        );
        this.waveColor = sg.c(text + " Wave Color", dw, ".", () -> this.mode.get() == RoundedColorMode.Wave && visible.get());
        this.shadowColor = sg.c(text + " Shadow Color", dt, ".", () -> this.mode.get() == RoundedColorMode.Static && visible.get());
        this.saturation = sg.d(text + " Saturation", 1.0, 0.1, 1.0, 0.1, ".", () -> this.mode.get() == RoundedColorMode.Rainbow && visible.get());
        this.frequency = sg.d(
                text + " Frequency",
                1.0,
                0.1,
                10.0,
                0.1,
                ".",
                () -> (this.mode.get() == RoundedColorMode.Wave || this.mode.get() == RoundedColorMode.Rainbow) && visible.get()
        );
        this.speed = sg.d(
                text + " Speed",
                1.0,
                0.1,
                10.0,
                0.1,
                ".",
                () -> (this.mode.get() == RoundedColorMode.Wave || this.mode.get() == RoundedColorMode.Rainbow) && visible.get()
        );
    }

    public static RoundedColorMultiSetting of(SettingGroup sg, String name) {
        return of(sg, () -> true, name);
    }

    public static RoundedColorMultiSetting of(SettingGroup sg, SingleOut<Boolean> visible, String name) {
        return of(sg, RoundedColorMode.Static, visible, name);
    }

    public static RoundedColorMultiSetting of(SettingGroup sg, RoundedColorMode dm, SingleOut<Boolean> visible, String name) {
        return of(sg, dm, new BlackOutColor(255, 255, 255, 255), new BlackOutColor(125, 125, 125, 255), visible, name);
    }

    public static RoundedColorMultiSetting of(SettingGroup sg, RoundedColorMode dm, BlackOutColor rc, BlackOutColor sc, SingleOut<Boolean> visible, String name) {
        return new RoundedColorMultiSetting(sg, dm, rc, sc, visible, name);
    }

    public BlackOutColor getRoundedColor() {
        return this.roundedColor.get();
    }

    public BlackOutColor getWaveColor() {
        return this.waveColor.get();
    }

    public Boolean isRainbow() {
        return this.mode.get() == RoundedColorMode.Rainbow;
    }

    public Boolean isStatic() {
        return this.mode.get() == RoundedColorMode.Static;
    }

    public Boolean isWave() {
        return this.mode.get() == RoundedColorMode.Wave;
    }

    public double saturation() {
        return this.saturation.get();
    }

    public void render(MatrixStack stack, float x, float y, float w, float h, float r, float sr) {
        switch (this.mode.get()) {
            case Static:
                RenderUtils.rounded(stack, x, y, w, h, r, sr, this.roundedColor.get().getRGB(), this.shadowColor.get().getRGB());
                break;
            case Wave:
                RenderUtils.fadeRounded(
                        stack,
                        x,
                        y,
                        w,
                        h,
                        r,
                        sr,
                        this.roundedColor.get().getRGB(),
                        this.waveColor.get().getRGB(),
                        this.frequency.get().floatValue(),
                        this.speed.get().floatValue()
                );
                break;
            case Rainbow:
                RenderUtils.rainbowRounded(
                        stack, x, y, w, h, r, sr, this.saturation.get().floatValue(), this.frequency.get().floatValue() / 5.0F, this.speed.get().floatValue() / 10.0F
                );
        }
    }
}
