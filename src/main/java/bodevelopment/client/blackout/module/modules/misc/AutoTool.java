package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class AutoTool extends Module {

    public AutoTool() {
        super("Auto Tool", "Automatically selects the most efficient tool from the hotbar based on the block currently being broken.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.interactionManager.isBreakingBlock()) {
                BlockPos pos = BlackOut.mc.interactionManager.currentBreakingPos;
                if (pos != null) {
                    FindResult best = this.bestSlot(pos);
                    if (best.wasFound()) {
                        if (!(this.miningDelta(pos, best.stack()) <= this.miningDelta(pos, Managers.PACKET.getStack()))) {
                            InvUtils.swap(best.slot());
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
        return BlockUtils.getBlockBreakingDelta(pos, stack);
    }
}
