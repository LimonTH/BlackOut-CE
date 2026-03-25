package bodevelopment.client.blackout.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class ItemUtils {
    public static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static boolean isGapple(Item item) {
        return item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static boolean isGapple(ItemStack stack) {
        return isGapple(stack.getItem());
    }

    public static boolean isShulker(Item item) {
        return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static boolean isShulker(ItemStack stack) {
        return isShulker(stack.getItem());
    }

    public static boolean isBed(Item item) {
        return item instanceof BedItem;
    }

    public static boolean isBed(ItemStack stack) {
        return isBed(stack.getItem());
    }

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

        value += EnchantmentUtils.getLevel(Enchantments.PROTECTION, stack) + 1;
        value += EnchantmentUtils.getLevel(Enchantments.FIRE_PROTECTION, stack) * 0.05;
        value += EnchantmentUtils.getLevel(Enchantments.FEATHER_FALLING, stack) * 0.1;
        value += EnchantmentUtils.getLevel(Enchantments.BLAST_PROTECTION, stack) * 0.05;
        value += EnchantmentUtils.getLevel(Enchantments.PROJECTILE_PROTECTION, stack) * 0.15;

        return value;
    }

    public static double getPickaxeValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        float speed = stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
        int efficiency = EnchantmentUtils.getLevel(Enchantments.EFFICIENCY, stack);

        if (speed > 1.0f && efficiency > 0) {
            speed += (float) (efficiency * efficiency + 1);
        }
        return speed;
    }

    public static double getAxeValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        float speed = stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
        int efficiency = EnchantmentUtils.getLevel(Enchantments.EFFICIENCY, stack);

        if (speed > 1.0f && efficiency > 0) {
            speed += (float) (efficiency * efficiency + 1);
        }
        return speed;
    }

    public static double getWeaponValue(ItemStack stack) {
        double damage = DamageUtils.itemDamage(stack);

        damage += EnchantmentUtils.getLevel(Enchantments.FIRE_ASPECT, stack) * 0.1;
        damage += EnchantmentUtils.getLevel(Enchantments.KNOCKBACK, stack) * 0.05;

        return damage;
    }

    public static double getBowValue(ItemStack stack) {
        int power = EnchantmentUtils.getLevel(Enchantments.POWER, stack);
        double damage = (power > 0) ? (2.5 + power * 0.5) : 2.0;

        int punch = EnchantmentUtils.getLevel(Enchantments.PUNCH, stack);
        return damage + punch * 0.3;
    }

    public static double getElytraValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        return (EnchantmentUtils.getLevel(Enchantments.MENDING, stack) > 0 ? 100 : 0)
                + EnchantmentUtils.getLevel(Enchantments.UNBREAKING, stack);
    }
}