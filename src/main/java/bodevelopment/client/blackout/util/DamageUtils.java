package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RaycastContext;
import org.apache.commons.lang3.mutable.MutableInt;

public class DamageUtils {
    public static RaycastContext raycastContext;

    public static double crystalDamage(LivingEntity entity, Box box, Vec3d pos) {
        return crystalDamage(entity, box, pos, null);
    }

    public static double crystalDamage(LivingEntity entity, Box entityBox, Vec3d crystalPos, BlockPos ignorePos) {
        if (entity == null || Managers.ENTITY.isDead(entity.getId())) return 0.0;

        return explosionDamage(entity, entityBox, crystalPos, ignorePos, 6.0);
    }

    public static double anchorDamage(LivingEntity entity, Box box, Vec3d pos) {
        return explosionDamage(entity, box, pos, null, 5.0);
    }

    public static double anchorDamage(LivingEntity entity, Box box, Vec3d pos, BlockPos ignorePos) {
        return explosionDamage(entity, box, pos, ignorePos, 5.0);
    }

    public static double creeperDamage(LivingEntity entity, Box box, Vec3d pos) {
        return explosionDamage(entity, box, pos, null, 3.0);
    }

    public static double creeperDamage(LivingEntity entity, Box box, Vec3d pos, BlockPos ignorePos) {
        return explosionDamage(entity, box, pos, ignorePos, 3.0);
    }

    public static double chargedCreeperDamage(LivingEntity entity, Box box, Vec3d pos) {
        return explosionDamage(entity, box, pos, null, 6.0);
    }

    public static double chargedCreeperDamage(LivingEntity entity, Box box, Vec3d pos, BlockPos ignorePos) {
        return explosionDamage(entity, box, pos, ignorePos, 6.0);
    }

    private static double explosionDamage(LivingEntity entity, Box box, Vec3d pos, BlockPos ignorePos, double strength) {
        double q = strength * 2.0;
        double dist = BoxUtils.feet(box).distanceTo(pos) / q;
        if (dist > 1.0) {
            return 0.0;
        } else {
            double aa = getExposure(pos, box, ignorePos);
            double ab = (1.0 - dist) * aa;
            double damage = (int) ((ab * ab + ab) * 3.5 * q + 1.0);
            damage = difficultyDamage(damage);
            damage = applyArmor(entity, damage);
            damage = applyResistance(entity, damage);
            return applyProtection(entity, damage, true);
        }
    }

    public static int getProtectionAmount(Iterable<ItemStack> equipment, boolean explosion) {
        int total = 0;
        var registry = BlackOut.mc.world.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

        for (ItemStack stack : equipment) {
            if (stack.isEmpty()) continue;

            // (Protection)
            int protLevel = EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.PROTECTION), stack);
            total += protLevel;

            // (Blast Protection)
            if (explosion) {
                int blastLevel = EnchantmentHelper.getLevel(registry.getOrThrow(Enchantments.BLAST_PROTECTION), stack);
                total += blastLevel * 2;
            }
        }
        return total;
    }

    public static double difficultyDamage(double damage) {
        if (BlackOut.mc.world.getDifficulty() == Difficulty.EASY) {
            return Math.min(damage / 2.0 + 1.0, damage);
        } else {
            return BlackOut.mc.world.getDifficulty() == Difficulty.NORMAL ? damage : damage * 1.5;
        }
    }

    public static double applyArmor(LivingEntity entity, double damage) {
        double armor = entity.getArmor();

        double toughness = entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

        double f = 2.0 + toughness / 4.0;

        // damage * (1 - clamp(armor - damage / f, armor * 0.2, 20) / 25)
        return damage * (1.0 - MathHelper.clamp(armor - damage / f, armor * 0.2, 20.0) / 25.0);
    }

    public static double applyResistance(LivingEntity entity, double damage) {
        int amplifier = entity.hasStatusEffect(StatusEffects.RESISTANCE) ? entity.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() : 0;
        int j = 25 - (amplifier + 1) * 5;
        return Math.max(damage * j / 25.0, 0.0);
    }

    public static double applyProtection(LivingEntity entity, double damage, boolean explosions) {
        int i = getProtectionAmount(entity.getArmorItems(), explosions);
        if (i > 0) {
            damage *= 1.0F - MathHelper.clamp(i, 0.0F, 20.0F) / 25.0F;
        }

        return damage;
    }

    public static double getExposure(Vec3d source, Box box, BlockPos ignorePos) {
        ((IRaycastContext) raycastContext).blackout_Client$set(RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, BlackOut.mc.player);
        ((IRaycastContext) raycastContext).blackout_Client$setStart(source);
        Vec3d vec3d = new Vec3d(0.0, 0.0, 0.0);
        double lx = box.getLengthX();
        double ly = box.getLengthY();
        double lz = box.getLengthZ();
        double deltaX = 1.0 / (lx * 2.0 + 1.0);
        double deltaY = 1.0 / (ly * 2.0 + 1.0);
        double deltaZ = 1.0 / (lz * 2.0 + 1.0);
        double offsetX = (1.0 - Math.floor(1.0 / deltaX) * deltaX) / 2.0;
        double offsetZ = (1.0 - Math.floor(1.0 / deltaZ) * deltaZ) / 2.0;
        double stepX = deltaX * lx;
        double stepY = deltaY * ly;
        double stepZ = deltaZ * lz;
        if (!(stepX < 0.0) && !(stepY < 0.0) && !(stepZ < 0.0)) {
            float i = 0.0F;
            float j = 0.0F;
            double x = box.minX + offsetX;

            for (double maxX = box.maxX + offsetX; x <= maxX; x += stepX) {
                ((IVec3d) vec3d).blackout_Client$setX(x);

                for (double y = box.minY; y <= box.maxY; y += stepY) {
                    ((IVec3d) vec3d).blackout_Client$setY(y);
                    double z = box.minZ + offsetZ;

                    for (double maxZ = box.maxZ + offsetZ; z <= maxZ; z += stepZ) {
                        ((IVec3d) vec3d).blackout_Client$setZ(z);
                        ((IRaycastContext) raycastContext).blackout_Client$setEnd(vec3d);
                        if (raycast(raycastContext, true, ignorePos).getType() == HitResult.Type.MISS) {
                            i++;
                        }

                        j++;
                    }
                }
            }

            return i / j;
        } else {
            return 0.0;
        }
    }

    public static BlockHitResult raycast(RaycastContext context, boolean damage) {
        return raycast(context, damage, null);
    }

    public static BlockHitResult raycast(RaycastContext context, boolean damage, BlockPos ignorePos) {
        return BlockView.raycast(
                context.getStart(),
                context.getEnd(),
                context,
                (contextx, pos) -> {
                    BlockState blockState;
                    if (pos.equals(ignorePos)) {
                        blockState = Blocks.AIR.getDefaultState();
                    } else if (damage) {
                        if (BlackOut.mc.world.getBlockState(pos).getBlock().getBlastResistance() < 200.0F) {
                            blockState = Blocks.AIR.getDefaultState();
                        } else {
                            blockState = Managers.BLOCK.damageState(pos);
                        }
                    } else {
                        blockState = BlackOut.mc.world.getBlockState(pos);
                    }

                    Vec3d vec3d = contextx.getStart();
                    Vec3d vec3d2 = contextx.getEnd();
                    VoxelShape voxelShape = contextx.getBlockShape(blockState, BlackOut.mc.world, pos);
                    return BlackOut.mc.world.raycastBlock(vec3d, vec3d2, pos, voxelShape, blockState);
                },
                contextx -> {
                    Vec3d vec3d = contextx.getStart().subtract(contextx.getEnd());
                    return BlockHitResult.createMissed(
                            contextx.getEnd(),
                            Direction.getFacing(vec3d.x, vec3d.y, vec3d.z),
                            BlockPos.ofFloored(contextx.getEnd())
                    );
                }
        );
    }

    public static double itemDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1.0;
        }

        // (Base)
        double damage = 1.0;

        // (Attack Damage)
        net.minecraft.component.type.AttributeModifiersComponent modifiers = stack.getOrDefault(
                net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS,
                net.minecraft.component.type.AttributeModifiersComponent.DEFAULT
        );

        // (Other)
        for (net.minecraft.component.type.AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(EntityAttributes.GENERIC_ATTACK_DAMAGE) && entry.slot().matches(net.minecraft.entity.EquipmentSlot.MAINHAND)) {
                damage += entry.modifier().value();
            }
        }

        // (Sharpness)
        if (BlackOut.mc.world != null) {
            var registry = BlackOut.mc.world.getRegistryManager()
                    .getWrapperOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT);

            int sharpnessLevel = EnchantmentHelper.getLevel(
                    registry.getOrThrow(Enchantments.SHARPNESS),
                    stack
            );

            if (sharpnessLevel > 0) {
                // 0.5 * level + 0.5
                damage += (sharpnessLevel * 0.5f + 0.5f);
            }
        }

        return damage;
    }
}
