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

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;

public class KillEffects extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> range = this.sgGeneral.intSetting("Detection Radius", 50, 0, 100, 1, "The maximum distance from the player to detect entity deaths.");
    public final Setting<Integer> tickDelay = this.sgGeneral.intSetting("Effect Cooldown", 10, 0, 20, 1, "The minimum number of ticks between spawning successive effects to prevent performance drops.");

    private int ticks = 0;

    public KillEffects() {
        super("Kill Effects", "Spawns aesthetic lightning bolts and thunder sounds at the location of a player's death.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            this.ticks++;
            if (this.ticks >= this.tickDelay.get()) {
                for (Player player : BlackOut.mc.level.players()) {
                    if (player != BlackOut.mc.player
                            && player.position().distanceTo(BlackOut.mc.player.position()) <= this.range.get()
                            && (player.getHealth() <= 0.0F || player.isDeadOrDying())) {
                        this.ticks = 0;
                        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, BlackOut.mc.level);
                        lightning.setPos(player.getX(), player.getY(), player.getZ());
                        BlackOut.mc.level.addEntity(lightning);
                        BlackOut.mc
                                .level
                                .playLocalSound(
                                        player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F, true
                                );
                    }
                }
            }
        }
    }
}
