package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AutoTool extends Module {
    public AutoTool() {
        super("Auto Tool", "Automatically selects the most efficient tool from the hotbar based on the block currently being broken.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (BlackOut.mc.gameMode.isDestroying()) {
                BlockPos pos = BlackOut.mc.gameMode.destroyBlockPos;
                if (pos != null) {
                    FindResult best = this.bestSlot(pos);
                    if (best.wasFound()) {
                        if (!(this.miningDelta(pos, best.stack()) <= this.miningDelta(pos, Managers.PACKET.getStack()))) {
                            SwitchMode.Normal.swap(best.slot());
                        }
                    }
                }
            }
        }
    }

    private FindResult bestSlot(BlockPos pos) {
        return InvUtils.findBest(true, false, stack -> this.miningDelta(pos, stack));
    }

    private double miningDelta(BlockPos pos, ItemStack stack) {
        double delta = BlockUtils.getBlockBreakingDelta(pos, stack);

        // In 1.21.4, Mojang removed bamboo from the sword_efficient tag and added it
        // to mineable/axe instead. This makes axes mathematically faster for bamboo,
        // but semantically a sword should still be preferred for cutting bamboo/cobwebs.
        if (stack.getItem() instanceof SwordItem && BlackOut.mc.level != null) {
            BlockState state = BlackOut.mc.level.getBlockState(pos);

            // Apply a 1.5x bonus for blocks in the SWORD_EFFICIENT tag (leaves, cobwebs, etc.)
            // and for bamboo/bamboo_sapling which were removed from the tag in 1.21.4.
            if (state.is(BlockTags.SWORD_EFFICIENT) || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)) {
                delta *= 1.5;
            }
        }

        return delta;
    }
}
