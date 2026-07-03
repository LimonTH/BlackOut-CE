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

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import bodevelopment.client.blackout.util.render.RenderLayer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public class GearHUD extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    private final Setting<Double> textScale = this.sgScale.doubleSetting("Label Scale", 1.0, 0.0, 5.0, 0.05, "The size multiplier for the numerical item counters.");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the gear list.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Blur Effect", true, "Applies a blur effect to the background for increased UI depth.");
    private final Setting<List<Item>> items = this.sgGeneral.itemListSetting("Tracked Items", "The specific inventory items to monitor and display in the HUD.", Items.END_CRYSTAL, Items.TOTEM_OF_UNDYING);
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Counter");

    public GearHUD() {
        super("Gear HUD", "Monitors and displays the total count of specific items within the player's inventory, such as crystals or totems.");
        this.setSize(32.0F, 64.0F);
    }

    @Override
    public void render() {
        float textWidth = 10.0F;

        for (Item item : this.items.get()) {
            textWidth = Math.max(textWidth, BlackOut.FONT.getWidth(String.valueOf(this.getAmount(item))) * this.textScale.get().floatValue());
        }

        textWidth += 2.0F;
        float backgroundWidth = textWidth + 16.0F;
        float length = this.items.get().size() * 16 + this.items.get().size() * 6 - 6;
        this.setSize(backgroundWidth, length);
        this.stack.pushPose();

        if (this.blur.get()) {
            Render2DUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, backgroundWidth, length, 3.0F, 10));
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(this.stack, 0.0F, 0.0F, backgroundWidth, length, 3.0F, 3.0F);
        }

        BlackOut.mc.renderBuffers().bufferSource().endBatch();
        RenderSystem.disableDepthTest();

        for (Item item : this.items.get()) {
            int amount = this.getAmount(item);
            this.textColor.render(this.stack, String.valueOf(amount), this.textScale.get().floatValue(), textWidth / 2.0F, 8.0F, true, true);
            Render2DUtils.renderItem(this.stack, item.getDefaultInstance(), textWidth, 0.0F, 16.0F, RenderLayer.HUD, true);
            this.stack.translate(0.0F, 22.0F, 0.0F);
        }
        BlackOut.mc.renderBuffers().bufferSource().endBatch();
        this.stack.popPose();
    }

    private int getAmount(Item item) {
        return InvUtils.count(true, true, stack -> stack.getItem() == item);
    }
}
