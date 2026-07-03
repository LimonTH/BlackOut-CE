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

package bodevelopment.client.blackout.randomstuff;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.*;

public class BlackOutColor {
    public static final BlackOutColor WHITE = new BlackOutColor(255, 255, 255, 255);
    public int red;
    public int green;
    public int blue;
    public int alpha;

    public BlackOutColor(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public static BlackOutColor from(int color) {
        return new BlackOutColor(ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color));
    }

    public BlackOutColor copy() {
        return new BlackOutColor(this.red, this.green, this.blue, this.alpha);
    }

    public Color getColor() {
        return new Color(this.red, this.green, this.blue, this.alpha);
    }

    public BlackOutColor alphaMulti(double m) {
        return new BlackOutColor(this.red, this.green, this.blue, (int) Math.round(this.alpha * m));
    }

    public int alphaMultiRGB(double m) {
        return (byte) (this.alpha * m) << 24 | this.red << 16 | this.green << 8 | this.blue;
    }

    public int getRGB() {
        return this.alpha << 24 | this.red << 16 | this.green << 8 | this.blue;
    }

    public void set(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public void set(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public BlackOutColor lerp(double delta, BlackOutColor to) {
        return new BlackOutColor(
                (int) Mth.lerp(delta, this.red, to.red),
                (int) Mth.lerp(delta, this.green, to.green),
                (int) Mth.lerp(delta, this.blue, to.blue),
                (int) Mth.lerp(delta, this.alpha, to.alpha)
        );
    }

    public BlackOutColor withAlpha(int alpha) {
        return new BlackOutColor(this.red, this.green, this.blue, alpha);
    }
}
