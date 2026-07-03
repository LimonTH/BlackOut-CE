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

import bodevelopment.client.blackout.annotations.NoAlloc;
import bodevelopment.client.blackout.annotations.PublicAPI;


import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.*;

@PublicAPI
@NoAlloc
public class ColorUtils {
    public static Color SHADOW100 = new Color(0, 0, 0, 100);
    public static int SHADOW100I = SHADOW100.getRGB();
    public static Color SHADOW80 = new Color(0, 0, 0, 80);
    public static int SHADOW80I = SHADOW80.getRGB();

    public static int intColor(int red, int green, int blue, int alpha) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static Color dark(Color color, double multiplier) {
        return new Color(
                (int) Math.floor(color.getRed() / multiplier),
                (int) Math.floor(color.getGreen() / multiplier),
                (int) Math.floor(color.getBlue() / multiplier),
                color.getAlpha()
        );
    }

    public static Color lerpColor(double delta, Color min, Color max) {
        return new Color(
                lerp(delta, min.getRed(), max.getRed()),
                lerp(delta, min.getGreen(), max.getGreen()),
                lerp(delta, min.getBlue(), max.getBlue()),
                lerp(delta, min.getAlpha(), max.getAlpha())
        );
    }

    public static Color getWave(Color color, Color color2, double speed, double length, int i) {
        double f = Math.sin(System.currentTimeMillis() / 1000.0 * speed - i / length) + 1.0;
        return new Color(
                colorVal(color.getRed(), color2.getRed(), f),
                colorVal(color.getGreen(), color2.getGreen(), f),
                colorVal(color.getBlue(), color2.getBlue(), f),
                color.getAlpha()
        );
    }

    public static int colorVal(int original, int wave, double f) {
        return Mth.clamp((int) Math.floor(wave + (original - wave) * f), 0, 255);
    }

    public static int getRainbow(float seconds, float saturation, float brightness) {
        float hue = (float) (System.currentTimeMillis() % (int) (seconds * 1000.0F)) / (seconds * 1000.0F);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static int getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = (float) ((System.currentTimeMillis() + index) % (int) (seconds * 1000.0F)) / (seconds * 1000.0F);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static float sinWave(float seconds, double offset) {
        return ((float) Math.sin((System.currentTimeMillis() + offset * 1000.0) % (int) (seconds * 1000.0F) / (seconds * 1000.0F) * 2.0 * Math.PI) + 1.0F) / 2.0F;
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 16777215;
    }

    public static int alphaMulti(int color, double alpha) {
        return withAlpha(color, (int) (ARGB.alpha(color) * alpha));
    }

    private static int lerp(double delta, int min, int max) {
        return (int) (min + (max - min) * delta);
    }
}
