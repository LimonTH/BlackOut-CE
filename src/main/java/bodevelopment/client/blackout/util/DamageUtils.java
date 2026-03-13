package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DamageUtils {
    public static ClipContext raycastContext;

    public static double crystalDamage(LivingEntity entity, AABB box, Vec3 pos) {
        return crystalDamage(entity, box, pos, null);
    }

    public static double crystalDamage(LivingEntity entity, AABB entityBox, Vec3 crystalPos, BlockPos ignorePos) {
        if (entity == null || Managers.ENTITY.isDead(entity.getId())) return 0.0;

        return explosionDamage(entity, entityBox, crystalPos, ignorePos, 6.0);
    }

    public static double anchorDamage(LivingEntity entity, AABB box, Vec3 pos) {
        return explosionDamage(entity, box, pos, null, 5.0);
    }

    public static double anchorDamage(LivingEntity entity, AABB box, Vec3 pos, BlockPos ignorePos) {
        return explosionDamage(entity, box, pos, ignorePos, 5.0);
    }

    public static double creeperDamage(LivingEntity entity, AABB box, Vec3 pos) {
        return explosionDamage(entity, box, pos, null, 3.0);
    }

    public static double creeperDamage(LivingEntity entity, AABB box, Vec3 pos, BlockPos ignorePos) {
        return explosionDamage(entity, box, pos, ignorePos, 3.0);
    }

    public static double chargedCreeperDamage(LivingEntity entity, AABB box, Vec3 pos) {
        return explosionDamage(entity, box, pos, null, 6.0);
    }

    public static double chargedCreeperDamage(LivingEntity entity, AABB box, Vec3 pos, BlockPos ignorePos) {
        return explosionDamage(entity, box, pos, ignorePos, 6.0);
    }

    private static double explosionDamage(LivingEntity entity, AABB box, Vec3 pos, BlockPos ignorePos, double strength) {
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
        var registry = BlackOut.mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        for (ItemStack stack : equipment) {
            if (stack.isEmpty()) continue;

            int protLevel = EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.PROTECTION), stack);
            total += protLevel;

            if (explosion) {
                int blastLevel = EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(Enchantments.BLAST_PROTECTION), stack);
                total += blastLevel * 2;
            }
        }
        return total;
    }

    public static double difficultyDamage(double damage) {
        if (BlackOut.mc.level.getDifficulty() == Difficulty.EASY) {
            return Math.min(damage / 2.0 + 1.0, damage);
        } else {
            return BlackOut.mc.level.getDifficulty() == Difficulty.NORMAL ? damage : damage * 1.5;
        }
    }

    public static double applyArmor(LivingEntity entity, double damage) {
        double armor = entity.getArmorValue();

        double toughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        double f = 2.0 + toughness / 4.0;

        return damage * (1.0 - Mth.clamp(armor - damage / f, armor * 0.2, 20.0) / 25.0);
    }

    public static double applyResistance(LivingEntity entity, double damage) {
        int amplifier = entity.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? entity.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() : 0;
        int j = 25 - (amplifier + 1) * 5;
        return Math.max(damage * j / 25.0, 0.0);
    }

    public static double applyProtection(LivingEntity entity, double damage, boolean explosions) {
        int i = getProtectionAmount(entity.getArmorSlots(), explosions);
        if (i > 0) {
            damage *= 1.0F - Mth.clamp(i, 0.0F, 20.0F) / 25.0F;
        }

        return damage;
    }

    public static double getExposure(Vec3 source, AABB box, BlockPos ignorePos) {
        ((IRaycastContext) raycastContext).blackout_Client$set(ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, BlackOut.mc.player);
        ((IRaycastContext) raycastContext).blackout_Client$setStart(source);
        Vec3 vec3d = new Vec3(0.0, 0.0, 0.0);
        double lx = box.getXsize();
        double ly = box.getYsize();
        double lz = box.getZsize();
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

    public static BlockHitResult raycast(ClipContext context, boolean damage) {
        return raycast(context, damage, null);
    }

    public static BlockHitResult raycast(ClipContext context, boolean damage, BlockPos ignorePos) {
        return BlockGetter.traverseBlocks(
                context.getFrom(),
                context.getTo(),
                context,
                (contextx, pos) -> {
                    BlockState blockState;
                    if (pos.equals(ignorePos)) {
                        blockState = Blocks.AIR.defaultBlockState();
                    } else if (damage) {
                        if (BlackOut.mc.level.getBlockState(pos).getBlock().getExplosionResistance() < 200.0F) {
                            blockState = Blocks.AIR.defaultBlockState();
                        } else {
                            blockState = Managers.BLOCK.damageState(pos);
                        }
                    } else {
                        blockState = BlackOut.mc.level.getBlockState(pos);
                    }

                    Vec3 vec3d = contextx.getFrom();
                    Vec3 vec3d2 = contextx.getTo();
                    VoxelShape voxelShape = contextx.getBlockShape(blockState, BlackOut.mc.level, pos);
                    return BlackOut.mc.level.clipWithInteractionOverride(vec3d, vec3d2, pos, voxelShape, blockState);
                },
                contextx -> {
                    Vec3 vec3d = contextx.getFrom().subtract(contextx.getTo());
                    return BlockHitResult.miss(
                            contextx.getTo(),
                            Direction.getApproximateNearest(vec3d.x, vec3d.y, vec3d.z),
                            BlockPos.containing(contextx.getTo())
                    );
                }
        );
    }

    public static double itemDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 1.0;
        }

        double damage = 1.0;

        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY
        );

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE) && entry.slot().test(net.minecraft.world.entity.EquipmentSlot.MAINHAND)) {
                damage += entry.modifier().amount();
            }
        }

        if (BlackOut.mc.level != null) {
            var registry = BlackOut.mc.level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT);

            int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(
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
