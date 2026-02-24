package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ChatUtils;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoChatGame extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Double> chance = this.sgGeneral.doubleSetting("Chance", 1.0, 0.0, 1.0, 0.01, "");
    private final Setting<DelayMode> delayMode = this.sgGeneral.enumSetting("Delay Mode", DelayMode.Dumb, "");
    private final Setting<Double> minDelay = this.sgGeneral.doubleSetting("Min Delay", 1.0, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Dumb);
    private final Setting<Double> maxDelay = this.sgGeneral.doubleSetting("Max Delay", 2.0, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Dumb);
    private final Setting<Double> chatOpenTime = this.sgGeneral
            .doubleSetting("Chat Open Time", 1.0, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> shiftTime = this.sgGeneral
            .doubleSetting("Shift Time", 0.1, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> altTime = this.sgGeneral.doubleSetting("Alt Time", 0.5, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> letterTime = this.sgGeneral
            .doubleSetting("Letter Time", 0.2, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> numberTime = this.sgGeneral
            .doubleSetting("Number Time", 0.3, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> specialTime = this.sgGeneral
            .doubleSetting("Special Char Time", 0.3, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> enterTime = this.sgGeneral
            .doubleSetting("Enter Time", 0.1, 0.0, 10.0, 0.1, "", () -> this.delayMode.get() == DelayMode.Smart);
    private final List<Character> shiftChars = new ArrayList<>();
    private final List<Character> altChars = new ArrayList<>();
    private String message;
    private long sendTime = 0L;

    public AutoChatGame() {
        super("Auto Chat Game", ".", SubCategory.MISC, true);
        this.initChars();
    }

    @Event
    public void onMessage(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof GameMessageS2CPacket packet) {
            String text = packet.content().getString();
            if (this.shouldSend(text)) {
                this.message = text.split("\"")[1];
                double delay = this.getDelay();
                this.sendTime = System.currentTimeMillis() + Math.round(delay * 1000.0);
                Managers.NOTIFICATIONS
                        .addNotification(String.format("Answering to a chat game in %.1fs", delay), this.getDisplayName(), 5.0, Notifications.Type.Info);
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.message != null) {
            if (System.currentTimeMillis() > this.sendTime) {
                ChatUtils.sendMessage(this.message);
                this.message = null;
            }
        } else {
            this.message = null;
        }
    }

    private boolean shouldSend(String string) {
        return !(ThreadLocalRandom.current().nextDouble() > this.chance.get()) && string.contains("CHAT GAME") && string.contains("First to type word");
    }

    private double getDelay() {
        return switch (this.delayMode.get()) {
            case Dumb ->
                    MathHelper.lerp(ThreadLocalRandom.current().nextDouble(), this.minDelay.get(), this.maxDelay.get());
            case Smart -> this.getSmartDelay(this.message);
        };
    }

    private double getSmartDelay(String string) {
        double total = this.chatOpenTime.get();
        int state = 0;

        for (char c : string.toCharArray()) {
            int reqState;
            if (Character.isUpperCase(c) || this.shiftChars.contains(c)) {
                reqState = 1;
            } else if (this.altChars.contains(c)) {
                reqState = 2;
            } else {
                reqState = 0;
            }

            if (state != reqState) {
                total += switch (reqState) {
                    case 1 -> this.shiftTime.get();
                    case 2 -> this.altTime.get();
                    default -> 0.0;
                };
                state = reqState;
            }

            if (Character.isDigit(c)) {
                total += this.numberTime.get();
            } else if ((c <= '@' || c >= '[') && (c <= '`' || c >= '{')) {
                total += this.specialTime.get();
            } else {
                total += this.letterTime.get();
            }
        }

        return total + this.enterTime.get();
    }

    private void initChars() {
        this.shiftChars.add('>');
        this.shiftChars.add(';');
        this.shiftChars.add(':');
        this.shiftChars.add('_');
        this.shiftChars.add('*');
        this.shiftChars.add('^');
        this.shiftChars.add('`');
        this.shiftChars.add('?');
        this.shiftChars.add('!');
        this.shiftChars.add('"');
        this.shiftChars.add('#');
        this.shiftChars.add('¤');
        this.shiftChars.add('%');
        this.shiftChars.add('&');
        this.shiftChars.add('/');
        this.shiftChars.add('(');
        this.shiftChars.add(')');
        this.shiftChars.add('=');
        this.shiftChars.add('½');
        this.altChars.add('|');
        this.altChars.add('@');
        this.altChars.add('£');
        this.altChars.add('$');
        this.altChars.add('€');
        this.altChars.add('{');
        this.altChars.add('[');
        this.altChars.add(']');
        this.altChars.add('}');
        this.altChars.add('\\');
        this.altChars.add('~');
    }

    public enum DelayMode {
        Dumb,
        Smart
    }
}
