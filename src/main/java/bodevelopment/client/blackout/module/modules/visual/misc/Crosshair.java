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

package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.util.ScreenUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;

public class Crosshair extends Module {
    private static Crosshair INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> remove = this.sgGeneral.booleanSetting("Hide Vanilla", false, "Suppresses the rendering of the default Minecraft crosshair.");
    private final Setting<Integer> dist = this.sgGeneral.intSetting("Gap Size", 5, 0, 25, 1, "The distance between the center of the screen and the start of the crosshair lines.", () -> !this.remove.get());
    private final Setting<Integer> width = this.sgGeneral.intSetting("Stroke Thickness", 1, 1, 5, 1, "The pixel width of each crosshair segment.", () -> !this.remove.get());
    private final Setting<Integer> length = this.sgGeneral.intSetting("Segment Length", 10, 0, 50, 1, "The pixel height/length of each crosshair segment.", () -> !this.remove.get());
    private final Setting<BlackOutColor> color = this.sgGeneral.colorSetting("Reticle Color", new BlackOutColor(255, 255, 255, 225), "The color and transparency of the custom crosshair.", () -> !this.remove.get());
    public final Setting<Boolean> t = this.sgGeneral.booleanSetting("T-Style Configuration", false, "Removes the top segment of the crosshair to create a 'T' shape.");

    private final PoseStack stack = new PoseStack();

    public Crosshair() {
        super("Crosshair", "Replaces or modifies the standard targeting reticle with a custom geometric crosshair.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static Crosshair getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.Hud.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (!this.remove.get()) {
                ScreenUtils.beginPixelSpace(this.stack);
                this.stack.translate(ScreenUtils.screenWidth() / 2.0F - 1.0F, ScreenUtils.screenHeight() / 2.0F - 1.0F, 0.0F);
                int d = this.dist.get();
                int w = this.width.get();
                int l = this.length.get();
                if (!this.t.get()) {
                    Render2DUtils.rounded(this.stack, -w / 2.0F, -d - l, w, l, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                }

                Render2DUtils.rounded(this.stack, d, -w / 2.0F, l, w, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                Render2DUtils.rounded(this.stack, -w / 2.0F, d, w, l, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                Render2DUtils.rounded(this.stack, -d - l, -w / 2.0F, l, w, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                ScreenUtils.endPixelSpace(this.stack);
            }
        }
    }
}
