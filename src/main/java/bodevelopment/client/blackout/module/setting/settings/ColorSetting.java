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

package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.screens.ColorScreen;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.*;

public class ColorSetting extends Setting<BlackOutColor> {
    public int theme = 0;
    public float saturation = 0.0F;
    public float brightness = 0.0F;
    public float hue = 0.0F;
    public int alpha = 255;
    public BlackOutColor actual = this.value.copy();

    public ColorSetting(String name, BlackOutColor val, String description, SingleOut<Boolean> visible) {
        super(name, val, description, visible);
        float[] hsb = Color.RGBtoHSB(val.red, val.green, val.blue, new float[3]);
        this.hue = hsb[0];
    }

    @Override
    public float render() {
        float textScale = 2.0F;
        float h = this.getHeight();
        float middleY = this.y + (h / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, middleY, GuiColorUtils.getSettingText(this.y), false, true);

        float rectWidth = 25.0F;
        float rectHeight = 8.0F;
        float rectX = this.x + this.width - rectWidth - 5.0F;

        float rectRenderY = middleY - (rectHeight / 2.0F);

        int color = this.get().alphaMulti(1.0F).getRGB();
        // Ensure minimum visibility: at least 30 alpha for the preview rect
        if ((color >> 24 & 0xFF) < 30) {
            color = color & 0x00FFFFFF | (30 << 24);
        }

        Render2DUtils.rounded(
                this.stack,
                rectX,
                rectRenderY,
                rectWidth,
                rectHeight,
                3.0F,
                4.0F,
                color,
                color
        );

        return h;
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        float clickOffset = -5.5F;

        if (key == 0 && pressed && this.mx > this.x && this.mx < this.x + this.width
                && this.my > this.y + clickOffset && this.my < this.y + this.getHeight() + clickOffset) {

            Managers.CLICK_GUI.openScreen(new ColorScreen(this, this.name));
            Managers.CONFIG.saveAll();
            return true;
        } else {
            return false;
        }
    }

    public BlackOutColor get() {
        return switch (this.theme) {
            case 1 -> this.modifyTheme(ThemeSettings.getInstance().getMain());
            case 2 -> this.modifyTheme(ThemeSettings.getInstance().getSecond());
            default -> this.actual;
        };
    }

    public BlackOutColor getUnmodified() {
        return switch (this.theme) {
            case 1 -> BlackOutColor.from(ThemeSettings.getInstance().getMain());
            case 2 -> BlackOutColor.from(ThemeSettings.getInstance().getSecond());
            default -> this.actual;
        };
    }

    private BlackOutColor modifyTheme(int theme) {
        float[] HSB = Color.RGBtoHSB(ARGB.red(theme), ARGB.green(theme), ARGB.blue(theme), new float[3]);
        HSB[1] = Mth.clamp(HSB[1] + this.saturation, 0.0F, 1.0F);
        HSB[2] = Mth.clamp(HSB[2] + this.brightness, 0.0F, 1.0F);
        return BlackOutColor.from(ColorUtils.withAlpha(Color.HSBtoRGB(HSB[0], HSB[1], HSB[2]), this.alpha));
    }

    @Override
    public float getHeight() {
        return 26.0F;
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.name, this.theme + "§" + this.alpha + "§" + this.saturation + "§" + this.brightness + "§" + this.actual.getRGB() + "§" + this.hue);
    }

    @Override
    public void set(JsonElement element) {
        String[] strings = element.getAsString().split("§");
        if (strings.length == 6) {
            // New format with hue
            this.theme = Integer.parseInt(strings[0]);
            this.alpha = Integer.parseInt(strings[1]);
            this.saturation = Mth.clamp(Float.parseFloat(strings[2]), -1.0F, 1.0F);
            this.brightness = Mth.clamp(Float.parseFloat(strings[3]), -1.0F, 1.0F);
            this.actual = BlackOutColor.from(Integer.parseInt(strings[4]));
            this.hue = Float.parseFloat(strings[5]);
        } else if (strings.length == 5) {
            // Legacy format, extract hue from RGB
            this.theme = Integer.parseInt(strings[0]);
            this.alpha = Integer.parseInt(strings[1]);
            this.saturation = Mth.clamp(Float.parseFloat(strings[2]), -1.0F, 1.0F);
            this.brightness = Mth.clamp(Float.parseFloat(strings[3]), -1.0F, 1.0F);
            this.actual = BlackOutColor.from(Integer.parseInt(strings[4]));
            float[] hsb = Color.RGBtoHSB(this.actual.red, this.actual.green, this.actual.blue, new float[3]);
            this.hue = hsb[0];
        } else {
            this.theme = 0;
            this.alpha = 255;
            this.brightness = 0.0F;
            this.saturation = 0.0F;
            this.hue = 0.0F;
            this.reset();
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.theme = 0;
        this.saturation = 0.0F;
        this.brightness = 0.0F;
        this.alpha = 255;
        this.actual = this.defaultValue.copy();
        float[] hsb = Color.RGBtoHSB(this.actual.red, this.actual.green, this.actual.blue, new float[3]);
        this.hue = hsb[0];
    }

    public void setValue(BlackOutColor color) {
        super.setValue(color);
        float[] hsb = Color.RGBtoHSB(color.red, color.green, color.blue, new float[3]);
        this.hue = hsb[0];
    }
}
