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

package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;

public class MewingGuide extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    public MewingGuide() {
        super("Mewing Guide", "Displays a graphical reference for facial posture techniques to optimize your player character's aesthetic presence.");
        TextureRenderer t = BOTextures.getMewingIconRenderer();
        this.setSize(t.getWidth() / 4.0F, t.getHeight() / 4.0F);
    }

    @Override
    public void render() {
        TextureRenderer t = BOTextures.getMewingIconRenderer();
        float width = t.getWidth() / 4.0F;
        float height = t.getHeight() / 4.0F;
        this.setSize(width, height);
        t.quad(this.stack, 0.0F, 0.0F, width, height);
    }
}
