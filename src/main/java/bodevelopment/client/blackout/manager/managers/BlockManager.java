package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockManager extends Manager {
    private final TimerMap<BlockPos, SpoofedBlock> timers = new TimerMap<>(true);

    public void set(BlockPos pos, Block type, boolean damage, boolean placing) {
        this.timers.add(pos, new SpoofedBlock(type, damage, placing), 1.0);
    }

    public void reset(BlockPos pos) {
        if (this.timers.containsKey(pos)) {
            this.timers.removeKey(pos);
        }
    }

    public BlockState damageState(BlockPos pos) {
        if (Simulation.getInstance().blocks() && this.timers.containsKey(pos)) {
            SpoofedBlock block = this.timers.get(pos);
            if (block != null && block.damage()) {
                return block.type().getDefaultState();
            }
        }

        return BlackOut.mc.world.getBlockState(pos);
    }

    public BlockState blockState(BlockPos pos) {
        if (Simulation.getInstance().blocks() && this.timers.containsKey(pos)) {
            SpoofedBlock block = this.timers.get(pos);
            if (block != null && block.placing()) {
                return block.type().getDefaultState();
            }
        }

        return BlackOut.mc.world.getBlockState(pos);
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onBlock(BlockStateEvent event) {
        this.reset(event.pos);
    }

    private record SpoofedBlock(Block type, boolean damage, boolean placing) {
    }
}
