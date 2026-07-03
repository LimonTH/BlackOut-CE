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

package bodevelopment.client.blackout.enums;

import bodevelopment.client.blackout.interfaces.functional.SingleOut;

import java.util.concurrent.ThreadLocalRandom;

public enum RandomMode {
    Sin(() -> (Math.sin(System.currentTimeMillis() / 500.0) + 1.0) / 2.0),
    Random(ThreadLocalRandom.current()::nextDouble),
    Disabled(() -> 0.5);

    private final SingleOut<Double> random;

    RandomMode(SingleOut<Double> random) {
        this.random = random;
    }

    public double get() {
        return this.random.get();
    }
}
