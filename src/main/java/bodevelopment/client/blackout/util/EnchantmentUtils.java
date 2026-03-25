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
