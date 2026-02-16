package bodevelopment.client.blackout.util;

import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.client.MinecraftClient;

public class ItemUtils {

    public static double getArmorValue(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armor) {
            double value = armor.getProtection();
            value += armor.getToughness() / 4.0;

            value += armor.getMaterial().value().knockbackResistance() / 3.0;

            value += getEnchantLevel(stack, Enchantments.PROTECTION) + 1;
            value += getEnchantLevel(stack, Enchantments.FIRE_PROTECTION) * 0.05;
            value += getEnchantLevel(stack, Enchantments.FEATHER_FALLING) * 0.1;
            value += getEnchantLevel(stack, Enchantments.BLAST_PROTECTION) * 0.05;
            value += getEnchantLevel(stack, Enchantments.PROJECTILE_PROTECTION) * 0.15;

            return value;
        }
        return 0.0;
    }

    public static double getPickaxeValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        float speed = stack.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState());
        int efficiency = getEnchantLevel(stack, Enchantments.EFFICIENCY);

        if (speed > 1.0f && efficiency > 0) {
            speed += (float) (efficiency * efficiency + 1);
        }
        return speed;
    }

    public static double getAxeValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        float speed = stack.getMiningSpeedMultiplier(Blocks.OAK_LOG.getDefaultState());
        int efficiency = getEnchantLevel(stack, Enchantments.EFFICIENCY);

        if (speed > 1.0f && efficiency > 0) {
            speed += (float) (efficiency * efficiency + 1);
        }
        return speed;
    }

    public static double getWeaponValue(ItemStack stack) {
        double damage = DamageUtils.itemDamage(stack);

        damage += getEnchantLevel(stack, Enchantments.FIRE_ASPECT) * 0.1;
        damage += getEnchantLevel(stack, Enchantments.KNOCKBACK) * 0.05;

        return damage;
    }

    private static int getEnchantLevel(ItemStack stack, net.minecraft.registry.RegistryKey<Enchantment> key) {
        var world = MinecraftClient.getInstance().world;
        if (world == null || stack.isEmpty()) return 0;

        var registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> entry = registry.getEntry(key).orElse(null);

        if (entry == null) return 0;
        return EnchantmentHelper.getLevel(entry, stack);
    }

    public static double getBowValue(ItemStack stack) {
        int power = getEnchantLevel(stack, Enchantments.POWER);
        double damage = (power > 0) ? (2.5 + power * 0.5) : 2.0;

        int punch = getEnchantLevel(stack, Enchantments.PUNCH);
        return damage + punch * 0.3;
    }

    public static double getElytraValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        return (getEnchantLevel(stack, Enchantments.MENDING) > 0 ? 100 : 0)
                + getEnchantLevel(stack, Enchantments.UNBREAKING);
    }
}