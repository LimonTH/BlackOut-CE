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

package bodevelopment.client.blackout.gui.clickgui;

import net.minecraft.ChatFormatting;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConsoleLog {
    private static final List<Entry> entries = new CopyOnWriteArrayList<>();
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final int MAX_ENTRIES = 200;

    public static void info(String message) {
        add(message, Color.WHITE.getRGB());
    }

    public static void success(String message) {
        add(message, Color.GREEN.getRGB());
    }

    public static void warn(String message) {
        add(message, Color.YELLOW.getRGB());
    }

    public static void error(String message) {
        add(message, Color.RED.getRGB());
    }

    public static void module(String moduleName, String event) {
        add(ChatFormatting.LIGHT_PURPLE + "[" + moduleName + "] " + ChatFormatting.GRAY + event, Color.WHITE.getRGB());
    }

    public static void combat(String message) {
        add(ChatFormatting.RED + "[Combat] " + ChatFormatting.GRAY + message, Color.WHITE.getRGB());
    }

    public static void toggle(String moduleName, boolean enabled) {
        ChatFormatting clr = enabled ? ChatFormatting.GREEN : ChatFormatting.RED;
        String state = enabled ? "enabled" : "disabled";
        add(clr + "[Toggle] " + ChatFormatting.WHITE + moduleName + " " + clr + state, Color.WHITE.getRGB());
    }

    public static void add(String message, int color) {
        LocalDateTime now = LocalDateTime.now();
        String time = String.format("[%02d:%02d:%02d] ", now.getHour(), now.getMinute(), now.getSecond());
        Entry entry = new Entry(time + message, time, color, System.currentTimeMillis());
        entries.addFirst(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
        for (Listener l : listeners) {
            l.onEntry(entry);
        }
    }

    public static List<Entry> getEntries() {
        return entries;
    }

    public static void addListener(Listener listener) {
        listeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void onEntry(Entry entry);
    }

    public record Entry(String text, String time, int color, long timestamp) {
    }
}
