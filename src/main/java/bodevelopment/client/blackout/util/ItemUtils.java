package bodevelopment.client.blackout.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

public class ItemUtils {
    public static double getArmorValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0.0;
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return 0.0;
        EquipmentSlot itemSlot = equippable.slot();

        double armor = 0;
        double toughness = 0;
        double knockbackResist = 0;

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.slot().test(itemSlot)) {
                if (entry.attribute().equals(Attributes.ARMOR)) {
                    armor += entry.modifier().amount();
                } else if (entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                    toughness += entry.modifier().amount();
                } else if (entry.attribute().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                    knockbackResist += entry.modifier().amount();
                }
            }
        }

        double value = armor;
        value += toughness / 4.0;
        value += knockbackResist / 3.0;

        value += getEnchantLevel(stack, Enchantments.PROTECTION) + 1;
        value += getEnchantLevel(stack, Enchantments.FIRE_PROTECTION) * 0.05;
        value += getEnchantLevel(stack, Enchantments.FEATHER_FALLING) * 0.1;
        value += getEnchantLevel(stack, Enchantments.BLAST_PROTECTION) * 0.05;
        value += getEnchantLevel(stack, Enchantments.PROJECTILE_PROTECTION) * 0.15;

        return value;
    }

    public static double getPickaxeValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        float speed = stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
        int efficiency = getEnchantLevel(stack, Enchantments.EFFICIENCY);

        if (speed > 1.0f && efficiency > 0) {
            speed += (float) (efficiency * efficiency + 1);
        }
        return speed;
    }

    public static double getAxeValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        float speed = stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
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

    private static int getEnchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
        var world = Minecraft.getInstance().level;
        if (world == null || stack.isEmpty()) return 0;

        var registry = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> entry = registry.get(key.location()).orElse(null);

        if (entry == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(entry, stack);
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