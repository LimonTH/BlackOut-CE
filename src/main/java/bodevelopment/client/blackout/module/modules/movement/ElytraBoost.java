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

package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;

import java.util.ArrayList;
import java.util.List;

public class ElytraBoost extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> antiConsume = this.sgGeneral.booleanSetting("Anti-Consume", true, "Prevents using actual fireworks from your inventory.");
    private final Setting<Integer> fireworkLevel = this.sgGeneral.intSetting("Boost Power", 1, 1, 3, 1, "The flight duration of the spawned firework.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "How to swap to fireworks if you aren't holding them.");
    private final Setting<Boolean> playSound = this.sgGeneral.booleanSetting("Play Sound", true, "Plays launch sound.");

    private final List<FireworkRocketEntity> spawnedFireworks = new ArrayList<>();
    private long lastBoostTime = 0L;

    public ElytraBoost() {
        super("Elytra Boost", "Simple boost for elytra flight.", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onDisable() {
        spawnedFireworks.clear();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (!PlayerUtils.isInGame()) return;

        spawnedFireworks.removeIf(Entity::isRemoved);

        if (BlackOut.mc.player.isFallFlying() && BlackOut.mc.options.keyUse.isDown()) {

            InteractionHand hand = InvUtils.getHand(stack -> stack.getItem() instanceof FireworkRocketItem);
            FindResult result = this.switchMode.get().find(stack -> stack.getItem() instanceof FireworkRocketItem);

            if (hand != null || result.wasFound()) {
                if (antiConsume.get()) {
                    if (System.currentTimeMillis() - lastBoostTime > 500) {
                        doFakeBoost();
                        lastBoostTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void doFakeBoost() {
        ItemStack stack = Items.FIREWORK_ROCKET.getDefaultInstance();
        stack.set(DataComponents.FIREWORKS, new Fireworks(fireworkLevel.get(), new ArrayList<>()));

        FireworkRocketEntity rocket = new FireworkRocketEntity(BlackOut.mc.level, stack, BlackOut.mc.player);
        spawnedFireworks.add(rocket);

        if (playSound.get()) {
            BlackOut.mc.level.playSound(BlackOut.mc.player, rocket,
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0F, 1.0F);
        }

        BlackOut.mc.level.addEntity(rocket);
    }
}