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

package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Items;

public class BowSpam extends Module {
    private static BowSpam INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> charge = this.sgGeneral.intSetting("Charge Duration", 3, 3, 20, 1, "The number of ticks to charge the bow before releasing the arrow.");
    public final Setting<Boolean> fast = this.sgGeneral.booleanSetting("Instant Draw", false, "Immediately begins drawing the next arrow after release to maximize projectile throughput.");

    public BowSpam() {
        super("Bow Spam", "Automatically releases bow tension at optimized intervals to maximize fire rate.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static BowSpam getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return String.valueOf(InvUtils.count(true, true, stack -> stack.getItem() instanceof ArrowItem));
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (BlackOut.mc.player.getMainHandItem().is(Items.BOW)
                    && BlackOut.mc.player.getTicksUsingItem() >= this.charge.get()
                    && BlackOut.mc.options.keyUse.isDown()) {
                BlackOut.mc.gameMode.releaseUsingItem(BlackOut.mc.player);
                if (this.fast.get()) {
                    BlackOut.mc.gameMode.useItem(BlackOut.mc.player, InteractionHand.MAIN_HAND);
                }
            }
        }
    }
}
