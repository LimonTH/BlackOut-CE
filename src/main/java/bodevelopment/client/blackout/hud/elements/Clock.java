package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Clock extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Mode> mode = this.sgGeneral.enumSetting("Chronological Format", Mode.Normal, "The nomenclature used for displaying the system time (24-hour vs. 12-hour AM/PM).");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Time Color");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the time string.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a blur shader to the background for enhanced contrast.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Bezel Rounding", true, "Smooths the corners of the background and blur layers.", () -> this.bg.get() || this.blur.get());

    private float textWidth = 0.0F;

    public Clock() {
        super("Clock", "Displays the current local system time with customizable formatting and post-processing effects.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            String time = switch (this.mode.get()) {
                case Normal -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                case American -> new SimpleDateFormat("hh:mm a").format(new Date());
            };
            this.textWidth = BlackOut.FONT.getWidth(time);
            this.setSize(this.textWidth, BlackOut.FONT.getHeight());
            this.stack.push();
            if (this.blur.get()) {
                RenderUtils.drawLoadedBlur(
                        "hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, this.textWidth, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 10)
                );
                Renderer.onHUDBlur();
            }

            if (this.bg.get()) {
                this.background.render(this.stack, 0.0F, 0.0F, this.textWidth, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 3.0F);
            }

            this.textColor.render(this.stack, time, 1.0F, 0.0F, 0.0F, false, false);
            this.stack.pop();
        }
    }

    public enum Mode {
        Normal,
        American
    }
}
