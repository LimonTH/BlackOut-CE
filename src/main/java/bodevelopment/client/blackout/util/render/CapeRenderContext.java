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

package bodevelopment.client.blackout.util.render;

import net.minecraft.resources.ResourceLocation;

public class CapeRenderContext {
    private static final ThreadLocal<ResourceLocation> CURRENT_CAPE = new ThreadLocal<>();
    private static final ThreadLocal<float[]> CURRENT_DIMENSIONS = new ThreadLocal<>();

    public static void set(ResourceLocation cape) {
        CURRENT_CAPE.set(cape);
    }

    public static void set(ResourceLocation cape, float texWidth, float texHeight) {
        CURRENT_CAPE.set(cape);
        CURRENT_DIMENSIONS.set(new float[] { texWidth, texHeight });
    }

    public static void clear() {
        CURRENT_CAPE.remove();
        CURRENT_DIMENSIONS.remove();
    }

    public static ResourceLocation get() {
        return CURRENT_CAPE.get();
    }

    public static float[] getDimensions() {
        return CURRENT_DIMENSIONS.get();
    }
}
