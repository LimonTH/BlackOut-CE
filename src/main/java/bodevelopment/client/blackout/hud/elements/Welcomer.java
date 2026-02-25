package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;
import bodevelopment.client.blackout.module.setting.Setting;

import java.time.LocalTime;

public class Welcomer extends TextElement {
    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Greeting Logic", Mode.Simple, "Determines whether to use a static welcome message or a time-of-day specific greeting.");

    public Welcomer() {
        super("Welcomer", "Displays a personalized greeting and welcome message on the HUD based on the current system time.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.stack.push();
            LocalTime currentTime = LocalTime.now();
            String timetxt;
            if (currentTime.isBefore(LocalTime.NOON)) {
                timetxt = "Good Morning,";
            } else if (currentTime.isBefore(LocalTime.of(18, 0))) {
                timetxt = "Good afternoon,";
            } else if (currentTime.isBefore(LocalTime.of(22, 0))) {
                timetxt = "Good evening,";
            } else {
                timetxt = "Good night,";
            }

            String txt;
            if (this.mode.get() == Mode.Time) {
                txt = timetxt;
            } else {
                txt = "Welcome to Blackout Client";
            }

            this.setSize(BlackOut.FONT.getWidth(txt), BlackOut.FONT.getHeight());
            this.drawElement(this.stack, txt, BlackOut.mc.player.getName().getString());
            this.stack.pop();
        }
    }

    public enum Mode {
        Simple,
        Time
    }
}
