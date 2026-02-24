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
    private final Setting<Mode> mode = this.sgGeneral.enumSetting("Time format", Mode.Normal, "What format to use to show the time");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Text");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Background", true, "Renders a background");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Blur", true, ".");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Rounded", true, "Renders a background", () -> this.bg.get() || this.blur.get());
    private float textWidth = 0.0F;

    public Clock() {
        super("Clock", "Shows you the current time");
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
