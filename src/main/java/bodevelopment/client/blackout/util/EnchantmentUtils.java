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

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class EnchantmentUtils {
    public static int getLevel(ResourceKey<Enchantment> key, ItemStack stack) {
        if (BlackOut.mc.level == null || stack.isEmpty()) return 0;

        var registry = BlackOut.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> entry = registry.get(key.location()).orElse(null);

        if (entry == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(entry, stack);
    }

    public static int getEquipmentLevel(ResourceKey<Enchantment> key, LivingEntity entity) {
        if (BlackOut.mc.level == null) return 0;

        var registry = BlackOut.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> entry = registry.get(key.location()).orElse(null);

        if (entry == null) return 0;
        return EnchantmentHelper.getEnchantmentLevel(entry, entity);
    }
}
