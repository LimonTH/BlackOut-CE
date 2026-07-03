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

import bodevelopment.client.blackout.annotations.Internal;

@Internal
public class RenderLayer {
    /**
     * WORLD
     */
    public static final float WORLD = 0.0F;

    /**
     * Particles
     */
    public static final float PARTICLES = 100.0F;

    /**
     * ESP
     */
    public static final float ESP = 200.0F;

    /**
     * Nametags
     */
    public static final float NAMETAGS = 300.0F;

    /**
     * HUD
     */
    public static final float HUD = 500.0F;

    /**
     * GUI
     */
    public static final float GUI = 700.0F;

    /**
     * GUI Elements (buttons, sliders)
     */
    public static final float GUI_ELEMENT = 800.0F;

    /**
     * GUI Popups (menus, dialogs)
     */
    public static final float GUI_POPUP = 900.0F;

    /**
     * Offset for sorting
     */
    public static final float OFFSET_LARGE = 10.0F;
    /**
     * Offset for sorting
     */
    public static final float OFFSET_SMALL = 1.0F;
    /**
     * Offset for sorting
     */
    public static final float OFFSET_MILI = 0.1F;
    /**
     * Offset for sorting
     */
    public static final float OFFSET_MICRO = 0.01F;
    /**
     * Offset for sorting
     */
    public static final float OFFSET_NANO = 0.001F;

    public static boolean isStandardLayer(float z) {
        return z == WORLD || z == ESP || z == PARTICLES || z == HUD || z == GUI ||
                z == GUI_ELEMENT || z == GUI_POPUP || z == OFFSET_SMALL ||
                z == OFFSET_LARGE || z == OFFSET_MICRO || z == OFFSET_NANO || z == OFFSET_MILI || z == NAMETAGS;
    }
}
