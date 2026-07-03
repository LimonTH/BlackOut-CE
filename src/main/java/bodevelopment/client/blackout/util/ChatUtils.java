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

package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.interfaces.mixin.IChatComponent;
import net.minecraft.network.chat.Component;

@PublicAPI
public class ChatUtils {
    public static void addMessage(Object object) {
        addMessage(object.toString());
    }

    public static void addMessage(String text, Object... objects) {
        addMessage(String.format(text, objects));
    }

    public static void addMessage(String text) {
        addMessage(Component.nullToEmpty(text));
    }

    public static void addMessage(String text, int id) {
        addMessage(Component.nullToEmpty(text), id);
    }

    public static void addMessage(Component text) {
        ((IChatComponent) BlackOut.mc.gui.getChat()).blackout_Client$addMessageToChat(text, -1);
    }

    public static void addMessage(Component text, int id) {
        ((IChatComponent) BlackOut.mc.gui.getChat()).blackout_Client$addMessageToChat(text, id);
    }

    public static void sendMessage(String text) {
        if (text.startsWith("/")) {
            BlackOut.mc.getConnection().sendCommand(text.substring(1));
        } else {
            BlackOut.mc.getConnection().sendChat(text);
        }
    }
}
