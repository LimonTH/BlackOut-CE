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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TimeUtils {
    public static int secondsSince(LocalDateTime dateTime) {
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        return (int) java.time.Duration.between(dateTime, currentTime).getSeconds();
    }

    public static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        seconds -= minutes * 60;
        int hours = minutes / 60;
        minutes -= hours * 60;
        int days = hours / 24;
        hours -= days * 24;
        int months = days / 30;
        days -= months * 30;
        int years = months / 12;
        months -= years * 12;

        if (years > 0) {
            return years + "y " + months + "mo";
        } else if (months > 0) {
            return months + "mo " + days + "d";
        } else if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + " s";
        }
    }

    public static String formatMillis(long ms) {
        int hours = (int) (ms / 3600000L % 60L);
        int minutes = (int) (ms / 60000L % 60L);
        int seconds = (int) (ms / 1000L % 60L);

        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0) {
            result.append(minutes).append("m ");
        }
        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            result.append(seconds).append("s");
        }

        return result.toString();
    }

    public static long measure(Runnable runnable) {
        long start = System.currentTimeMillis();
        runnable.run();
        return System.currentTimeMillis() - start;
    }
}
