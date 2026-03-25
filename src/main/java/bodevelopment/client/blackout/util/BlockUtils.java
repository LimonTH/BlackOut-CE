package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockUtils {

    public static boolean mineable(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(pos);

        if (state.isAir() || state.is(Blocks.BEDROCK)) return false;
        if (state.getDestroySpeed(BlackOut.mc.level, pos) < 0) return false;

        return !state.getCollisionShape(BlackOut.mc.level, pos).isEmpty();
    }

    public static boolean replaceable(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        return replaceable(BlackOut.mc.level.getBlockState(pos));
    }

    public static boolean replaceable(BlockState state) {
        return state.canBeReplaced();
    }

    public static boolean collidable(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(pos);
        return !state.getCollisionShape(BlackOut.mc.level, pos).isEmpty();
    }

    public static boolean hasCollision(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(pos);
        return !state.getCollisionShape(BlackOut.mc.level, pos,
                BlackOut.mc.player != null ? CollisionContext.of(BlackOut.mc.player) : CollisionContext.empty()
        ).isEmpty();
    }

    public static boolean isFaceSturdy(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(pos);
        return state.isFaceSturdy(BlackOut.mc.level, pos, Direction.UP);
    }

    public static boolean isSafe(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        float resistance = BlackOut.mc.level.getBlockState(pos).getBlock().getExplosionResistance();
        return resistance >= 600.0f;
    }

    public static boolean hasEntityCollision(Entity entity, AABB box) {
        if (BlackOut.mc.level == null) return false;
        return BlackOut.mc.level.getBlockCollisions(entity, box).iterator().hasNext();
    }

    public static boolean strictDirection(BlockPos pos, Direction dir, boolean ncp) {
        return switch (dir) {
            case DOWN -> BlackOut.mc.player.getEyePosition().y <= pos.getY() + (ncp ? 0.5 : 0.0);
            case UP -> BlackOut.mc.player.getEyePosition().y >= pos.getY() + (ncp ? 0.5 : 1.0);
            case NORTH -> BlackOut.mc.player.getZ() < pos.getZ();
            case SOUTH -> BlackOut.mc.player.getZ() >= pos.getZ() + 1;
            case WEST -> BlackOut.mc.player.getX() < pos.getX();
            case EAST -> BlackOut.mc.player.getX() >= pos.getX() + 1;
            default -> throw new IncompatibleClassChangeError();
        };
    }

    public static boolean inWater(AABB box) {
        return inFluid(box, FluidTags.WATER);
    }

    public static boolean inLava(AABB box) {
        return inFluid(box, FluidTags.LAVA);
    }

    public static boolean inFluid(AABB box, TagKey<Fluid> tag) {
        int minX = Mth.floor(box.minX + 0.001);
        int maxX = Mth.ceil(box.maxX - 0.001);
        int minY = Mth.floor(box.minY + 0.001);
        int maxY = Mth.ceil(box.maxY - 0.001);
        int minZ = Mth.floor(box.minZ + 0.001);
        int maxZ = Mth.ceil(box.maxZ - 0.001);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    FluidState fluidState = BlackOut.mc.level.getFluidState(new BlockPos(x, y, z));
                    if (fluidState.is(tag) && y + fluidState.getOwnHeight() > minY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static double fluidHeight(AABB box, TagKey<Fluid> tag) {
        int minX = Mth.floor(box.minX + 0.001);
        int maxX = Mth.ceil(box.maxX - 0.001);
        int minY = Mth.floor(box.minY + 0.001);
        int maxY = Mth.ceil(box.maxY - 0.001);
        int minZ = Mth.floor(box.minZ + 0.001);
        int maxZ = Mth.ceil(box.maxZ - 0.001);
        double maxHeight = 0.0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    FluidState fluidState = BlackOut.mc.level.getFluidState(new BlockPos(x, y, z));
                    if (fluidState.is(tag)) {
                        maxHeight = Math.max(maxHeight, y + fluidState.getOwnHeight() - box.minY);
                    }
                }
            }
        }
        return maxHeight;
    }

    public static double getBlockBreakingDelta(BlockPos pos, ItemStack stack) {
        return getBlockBreakingDelta(pos, stack, true, true, true);
    }

    public static double getBlockBreakingDelta(BlockPos pos, ItemStack stack, boolean effects, boolean water, boolean onGround) {
        if (BlackOut.mc.level == null) return 0;
        return getBlockBreakingDelta(stack, BlackOut.mc.level.getBlockState(pos), pos, effects, water, onGround);
    }

    public static double getBlockBreakingDelta(ItemStack stack, BlockState state, BlockPos pos, boolean effects, boolean water, boolean onGround) {
        float f = state.getDestroySpeed(BlackOut.mc.level, pos);
        if (f == -1.0F) {
            return 0.0;
        } else {
            int i = state.requiresCorrectToolForDrops() && !stack.isCorrectToolForDrops(state) ? 100 : 30;
            return getBlockBreakingSpeed(state, stack, effects, water, onGround) / f / i;
        }
    }

    public static double getBlockBreakingSpeed(BlockState state, ItemStack stack, boolean effects, boolean water, boolean onGround) {
        float f = stack.getDestroySpeed(state);

        if (f > 1.0F) {
            int efficiencyLevel = EnchantmentUtils.getLevel(Enchantments.EFFICIENCY, stack);

            if (efficiencyLevel > 0 && !stack.isEmpty()) {
                f += (float) (efficiencyLevel * efficiencyLevel + 1);
            }
        }

        if (effects && BlackOut.mc.player.hasEffect(MobEffects.DIG_SPEED)) {
            f *= 1.0F + (BlackOut.mc.player.getEffect(MobEffects.DIG_SPEED).getAmplifier() + 1) * 0.2F;
        }

        if (effects && BlackOut.mc.player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            float fatigueMul = switch (BlackOut.mc.player.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.027F;
                default -> 8.1E-4F;
            };
            f *= fatigueMul;
        }

        if (water && BlackOut.mc.player.isEyeInFluid(FluidTags.WATER)) {
            int aquaLevel = EnchantmentUtils.getEquipmentLevel(Enchantments.AQUA_AFFINITY, BlackOut.mc.player);

            if (aquaLevel <= 0) {
                f /= 5.0F;
            }
        }

        if (onGround && !Managers.PACKET.isOnGround()) {
            f /= 5.0F;
        }

        return f;
    }
}