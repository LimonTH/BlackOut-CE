package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;
import bodevelopment.client.blackout.module.setting.Setting;

import java.time.LocalTime;

public class Welcomer extends TextElement {
    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Simple, ".");

    public Welcomer() {
        super("Welcomer", "Says hello to you");
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
