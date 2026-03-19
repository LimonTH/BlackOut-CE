package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUtils {

    public static boolean mineable(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        BlockState state = BlackOut.mc.level.getBlockState(pos);

        if (state.isAir() || state.is(Blocks.BEDROCK)) return false;
        if (state.getDestroySpeed(BlackOut.mc.level, pos) < 0) return false;

        return !state.getCollisionShape(BlackOut.mc.level, pos).isEmpty();
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
            int efficiencyLevel = OLEPOSSUtils.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);

            if (efficiencyLevel > 0 && !stack.isEmpty()) {
                f += (float) (efficiencyLevel * efficiencyLevel + 1);
            }
        }

        if (effects && BlackOut.mc.player.hasEffect(MobEffects.HASTE)) {
            f *= 1.0F + (BlackOut.mc.player.getEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2F;
        }

        if (effects && BlackOut.mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float fatigueMul = switch (BlackOut.mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.027F;
                default -> 8.1E-4F;
            };
            f *= fatigueMul;
        }

        if (water && BlackOut.mc.player.isEyeInFluid(FluidTags.WATER)) {
            int aquaLevel = OLEPOSSUtils.getEquipmentEnchantmentLevel(Enchantments.AQUA_AFFINITY, BlackOut.mc.player);

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