/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ChatUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoChatGame extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> chance = this.sgGeneral.doubleSetting("Response Probability", 1.0, 0.0, 1.0, 0.01, "The statistical likelihood of the module participating in a detected chat game.");
    private final Setting<DelayMode> delayMode = this.sgGeneral.enumSetting("Timing Mode", DelayMode.Dumb, "The method used to calculate the response delay.");
    private final Setting<Double> minDelay = this.sgGeneral.doubleSetting("Minimum Wait", 1.0, 0.0, 10.0, 0.1, "The lower bound for randomized response timing.", () -> this.delayMode.get() == DelayMode.Dumb);
    private final Setting<Double> maxDelay = this.sgGeneral.doubleSetting("Maximum Wait", 2.0, 0.0, 10.0, 0.1, "The upper bound for randomized response timing.", () -> this.delayMode.get() == DelayMode.Dumb);
    private final Setting<Double> chatOpenTime = this.sgGeneral.doubleSetting("GUI Interaction Time", 1.0, 0.0, 10.0, 0.1, "Simulated time taken to open the chat interface.", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> shiftTime = this.sgGeneral.doubleSetting("Modifier Key Delay", 0.1, 0.0, 10.0, 0.1, "Additional time added when a character requires the Shift key.", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> altTime = this.sgGeneral.doubleSetting("AltGr Delay", 0.5, 0.0, 10.0, 0.1, "Additional time added when a character requires the Alt key.", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> letterTime = this.sgGeneral.doubleSetting("Alpha Key Stroke", 0.2, 0.0, 10.0, 0.1, "Simulated time taken to type a standard letter.", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> numberTime = this.sgGeneral.doubleSetting("Numeric Key Stroke", 0.3, 0.0, 10.0, 0.1, "Simulated time taken to type a number.", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> specialTime = this.sgGeneral.doubleSetting("Symbol Key Stroke", 0.3, 0.0, 10.0, 0.1, "Simulated time taken to type special characters.", () -> this.delayMode.get() == DelayMode.Smart);
    private final Setting<Double> enterTime = this.sgGeneral.doubleSetting("Submission Delay", 0.1, 0.0, 10.0, 0.1, "Simulated time taken to press the Enter key.", () -> this.delayMode.get() == DelayMode.Smart);

    private final List<Character> shiftChars = new ArrayList<>();
    private final List<Character> altChars = new ArrayList<>();
    private String message;
    private long sendTime = 0L;

    public AutoChatGame() {
        super("Auto Chat Game", "Automatically solves and answers in-game chat challenges with customizable human-like typing delays.", SubCategory.MISC, true);
        this.initChars();
    }

    @Event
    public void onMessage(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundSystemChatPacket packet) {
            String text = packet.content().getString();
            if (this.shouldSend(text)) {
                this.message = text.split("\"")[1];
                double delay = this.getDelay();
                this.sendTime = System.currentTimeMillis() + Math.round(delay * 1000.0);
                Managers.NOTIFICATIONS
                        .addNotification(String.format("Answering to a chat game in %.1fs", delay), this.getDisplayName(), 5.0, NotificationsSettings.Type.Info);
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame() && this.message != null) {
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
            case Dumb -> Mth.lerp(ThreadLocalRandom.current().nextDouble(), this.minDelay.get(), this.maxDelay.get());
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
