package bodevelopment.client.blackout.module.setting.multisettings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.TextColorMode;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.font.CustomFontRenderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class TextColorMultiSetting {
    public static final long initTime = System.currentTimeMillis();
    private final Setting<TextColorMode> mode;
    private final Setting<BlackOutColor> textColor;
    private final Setting<BlackOutColor> waveColor;
    private final Setting<Double> saturation;
    private final Setting<Double> frequency;
    private final Setting<Double> speed;

    private TextColorMultiSetting(SettingGroup sg, TextColorMode dm, BlackOutColor dt, BlackOutColor dw, SingleOut<Boolean> visible, String name) {
        String text = name == null ? "Text" : name;

        this.mode = sg.enumSetting(text + " Color Mode", dm,
                "Determines how the text is colored: Static, Wave, or Rainbow.");

        this.textColor = sg.colorSetting(text + " Color", dt,
                "The primary color of the text, or the first color for the Wave mode.",
                () -> (this.mode.get() == TextColorMode.Static || this.mode.get() == TextColorMode.Wave) && visible.get());

        this.waveColor = sg.colorSetting(text + " Wave Color", dw,
                "The secondary color that cycles with the primary color in Wave mode.",
                () -> this.mode.get() == TextColorMode.Wave && visible.get());

        this.saturation = sg.doubleSetting(text + " Saturation", 1.0, 0.1, 1.0, 0.1,
                "Adjusts how vibrant the colors are in Rainbow mode.",
                () -> this.mode.get() == TextColorMode.Rainbow && visible.get());

        this.frequency = sg.doubleSetting(
                text + " Frequency",
                1.0,
                0.1,
                10.0,
                0.1,
                "Controls the density of the gradient. Higher values mean more color cycles across the text.",
                () -> (this.mode.get() == TextColorMode.Wave || this.mode.get() == TextColorMode.Rainbow) && visible.get()
        );

        this.speed = sg.doubleSetting(
                text + " Speed",
                1.0,
                0.1,
                10.0,
                0.1,
                "Controls how fast the colors animate/cycle.",
                () -> (this.mode.get() == TextColorMode.Wave || this.mode.get() == TextColorMode.Rainbow) && visible.get()
        );
    }

    public static TextColorMultiSetting of(SettingGroup sg, String name) {
        return of(sg, () -> true, name);
    }

    public static TextColorMultiSetting of(SettingGroup sg, SingleOut<Boolean> visible, String name) {
        return of(sg, TextColorMode.Static, visible, name);
    }

    public static TextColorMultiSetting of(SettingGroup sg, TextColorMode dm, SingleOut<Boolean> visible, String name) {
        return of(sg, dm, new BlackOutColor(255, 255, 255, 255), new BlackOutColor(125, 125, 125, 255), visible, name);
    }

    public static TextColorMultiSetting of(SettingGroup sg, TextColorMode dm, BlackOutColor dt, BlackOutColor dw, SingleOut<Boolean> visible, String name) {
        return new TextColorMultiSetting(sg, dm, dt, dw, visible, name);
    }

    public BlackOutColor getTextColor() {
        return this.textColor.get();
    }

    public BlackOutColor getWaveColor() {
        return this.waveColor.get();
    }

    public Boolean isRainbow() {
        return this.mode.get() == TextColorMode.Rainbow;
    }

    public Boolean isStatic() {
        return this.mode.get() == TextColorMode.Static;
    }

    public Boolean isWave() {
        return this.mode.get() == TextColorMode.Wave;
    }

    public float saturation() {
        return this.saturation.get().floatValue();
    }

    public float frequency() {
        return this.frequency.get().floatValue();
    }

    public float speed() {
        return this.speed.get().floatValue();
    }

    public void render(MatrixStack stack, String text, float scale, float x, float y, boolean xCenter, boolean yCenter) {
        this.render(stack, text, scale, x, y, xCenter, yCenter, false);
    }

    public void render(MatrixStack stack, String text, float scale, float x, float y, boolean xCenter, boolean yCenter, boolean bold) {
        CustomFontRenderer renderer = bold ? BlackOut.BOLD_FONT : BlackOut.FONT;
        switch (this.mode.get()) {
            case Static:
                renderer.text(stack, text, scale, x, y, this.textColor.get().getColor(), xCenter, yCenter);
                break;
            case Wave:
                renderer.text(stack, text, scale, x, y, Color.WHITE, xCenter, yCenter, Shaders.fontwave, new ShaderSetup(setup -> {
                    setup.set("frequency", this.frequency.get().floatValue() * 2.0F);
                    setup.set("speed", this.speed.get().floatValue());
                    setup.color("clr1", this.textColor.get().getRGB());
                    setup.color("clr2", this.waveColor.get().getRGB());
                    setup.time(initTime);
                }));
                break;
            case Rainbow:
                renderer.text(stack, text, scale, x, y, Color.WHITE, xCenter, yCenter, Shaders.gradientfont, new ShaderSetup(setup -> {
                    setup.set("frequency", this.frequency.get().floatValue() / 3.0F);
                    setup.set("speed", this.speed.get().floatValue() / 10.0F);
                    setup.set("saturation", this.saturation.get().floatValue());
                    setup.time(initTime);
                }));
        }
    }
}
