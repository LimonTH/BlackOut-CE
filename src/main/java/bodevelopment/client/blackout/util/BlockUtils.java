package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public class BlockUtils {

    public static boolean mineable(BlockPos pos) {
        if (BlackOut.mc.world == null) return false;
        BlockState state = BlackOut.mc.world.getBlockState(pos);

        if (state.isAir() || state.isOf(Blocks.BEDROCK)) return false;
        if (state.getHardness(BlackOut.mc.world, pos) < 0) return false;

        return !state.getCollisionShape(BlackOut.mc.world, pos).isEmpty();
    }

    public static double getBlockBreakingDelta(BlockPos pos, ItemStack stack) {
        return getBlockBreakingDelta(pos, stack, true, true, true);
    }

    public static double getBlockBreakingDelta(BlockPos pos, ItemStack stack, boolean effects, boolean water, boolean onGround) {
        if (BlackOut.mc.world == null) return 0;
        return getBlockBreakingDelta(stack, BlackOut.mc.world.getBlockState(pos), pos, effects, water, onGround);
    }

    public static double getBlockBreakingDelta(ItemStack stack, BlockState state, BlockPos pos, boolean effects, boolean water, boolean onGround) {
        float f = state.getHardness(BlackOut.mc.world, pos);
        if (f == -1.0F) {
            return 0.0;
        } else {
            int i = state.isToolRequired() && !stack.isSuitableFor(state) ? 100 : 30;
            return getBlockBreakingSpeed(state, stack, effects, water, onGround) / f / i;
        }
    }

    public static double getBlockBreakingSpeed(BlockState state, ItemStack stack, boolean effects, boolean water, boolean onGround) {
        float f = stack.getMiningSpeedMultiplier(state);

        if (f > 1.0F) {
            int efficiencyLevel = OLEPOSSUtils.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);

            if (efficiencyLevel > 0 && !stack.isEmpty()) {
                f += (float) (efficiencyLevel * efficiencyLevel + 1);
            }
        }

        if (effects && BlackOut.mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            f *= 1.0F + (BlackOut.mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
        }

        if (effects && BlackOut.mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float fatigueMul = switch (BlackOut.mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.027F;
                default -> 8.1E-4F;
            };
            f *= fatigueMul;
        }

        if (water && BlackOut.mc.player.isSubmergedIn(FluidTags.WATER)) {
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