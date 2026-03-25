package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

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
