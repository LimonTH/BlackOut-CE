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

import net.minecraft.util.Mth;

import java.util.concurrent.ThreadLocalRandom;

public class StringStorage {
    private static final String[] adjectives = new String[]{
            "Fat",
            "Goofy",
            "Funny",
            "Sad",
            "Mad",
            "Large",
            "Former",
            "Massive",
            "Huge",
            "Angry",
            "Legal",
            "Nice",
            "Cute",
            "Happy",
            "Poor",
            "Hot",
            "Strong",
            "Known",
            "Scared",
            "Old",
            "Fast",
            "Epic",
            "Best",
            "Wide",
            "Smart"
    };
    private static final String[] substantives = new String[]{
            "Dog",
            "Pig",
            "Bear",
            "Player",
            "Salmon",
            "Fish",
            "Sheep",
            "Cow",
            "Bat",
            "Goose",
            "Ostrich",
            "Emu",
            "Kiwi",
            "Hog",
            "Sloth",
            "Noob",
            "Person",
            "Kid",
            "Rat",
            "Mouse",
            "Cat",
            "Bird"
    };

    public static String randomAdj() {
        return getRandom(adjectives);
    }

    public static String randomSub() {
        return getRandom(substantives);
    }

    private static <T> T getRandom(T[] array) {
        return array[(int) Math.round(Mth.lerp(ThreadLocalRandom.current().nextDouble(), 0.0, array.length - 1))];
    }
}
