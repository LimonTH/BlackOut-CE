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

public class StringUtils {
    public static double similarity(String string1, String string2) {
        String shorter;
        String longer;
        if (string1.length() > string2.length()) {
            shorter = string2.toLowerCase();
            longer = string1.toLowerCase();
        } else {
            shorter = string1.toLowerCase();
            longer = string2.toLowerCase();
        }

        int i = 0;
        int matched = 0;

        for (int c = 0; c < longer.length(); c++) {
            char charLonger = longer.charAt(c);

            for (int ci = i; ci < shorter.length(); ci++) {
                if (charLonger == shorter.charAt(ci)) {
                    matched++;
                    i = ci;
                    break;
                }
            }
        }

        return (float) matched / longer.length();
    }
}
