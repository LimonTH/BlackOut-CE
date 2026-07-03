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
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@PublicAPI
public class BOLogger {
    private static final Logger LOGGER = LogManager.getLogger(BlackOut.NAME);

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void info(Component text) {
        LOGGER.info(text.getString());
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void warn(Component text) {
        LOGGER.warn(text);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    public static void error(Throwable throwable) {
        LOGGER.error(throwable);
    }

    public static void debug(String message) {
        LOGGER.debug(message);
    }
}