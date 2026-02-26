package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.util.BoxUtils;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.*;

public class Search extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRender = this.addGroup("Visuals");

    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Target Blocks", "The specific block types to locate.");
    private final Setting<Boolean> dynamicBox = this.sgGeneral.booleanSetting("Voxel Bounds", true, "Adjusts highlight to match the exact block shape.");
    private final Setting<Boolean> instantScan = this.sgGeneral.booleanSetting("Force Scan", false, "Scans all loaded chunks immediately.");
    private final Setting<Integer> scanSpeed = this.sgGeneral.intSetting("Iteration Rate", 1, 1, 10, 1, "Chunks per frame during scan.", () -> !this.instantScan.get());
    private final Setting<Boolean> onlyExposed = this.sgGeneral.booleanSetting("Culling", false, "Only highlights blocks exposed to air.");

    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgRender);

    private final Map<BlockPos, Box> positions = new ConcurrentHashMap<>();
    private final Set<ChunkPos> prevChunks = new HashSet<>();
    private final Queue<ChunkPos> toScan = new ConcurrentLinkedQueue();

    private final ForkJoinPool pool = new ForkJoinPool();

    public Search() {
        super("Search", "Locates blocks using all CPU cores and advanced palette culling.", SubCategory.WORLD, true);
    }

    @Override
    public void onEnable() { this.reset(); }

    @Event
    public void onJoin(GameJoinEvent event) { this.reset(); }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.world != null) {
            this.checkChunks();
            this.find();
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        positions.values().forEach(rendering::render);
    }

    @Event
    public void onState(BlockStateEvent event) {
        if (BlackOut.mc.world != null) {
            this.onBlock(event.state.getBlock(), event.pos, true);
        }
    }

    private void reset() {
        this.prevChunks.clear();
        this.toScan.clear();
        this.positions.clear();
    }

    private void find() {
        if (toScan.isEmpty()) return;

        int limit = instantScan.get() ? toScan.size() : scanSpeed.get();
        for (int i = 0; i < limit; i++) {
            if (toScan.isEmpty()) break;
            ChunkPos pos = toScan.remove();

            pool.execute(() -> this.scan(pos));
        }
    }

    private void scan(ChunkPos pos) {
        try {
            var chunkView = BlackOut.mc.world.getChunkManager().getChunk(pos.x, pos.z);
            if (!(chunkView instanceof WorldChunk chunk) || chunk.isEmpty()) {
                return;
            }
            ChunkSection[] sections = chunk.getSectionArray();

            for (int i = 0; i < sections.length; i++) {
                ChunkSection section = sections[i];
                if (section == null || section.isEmpty()) continue;

                if (!section.getBlockStateContainer().hasAny(state -> this.blocks.get().contains(state.getBlock()))) {
                    continue;
                }

                int startX = pos.getStartX();
                int startZ = pos.getStartZ();
                int minY = chunk.getBottomY() + (i * 16);

                List<FoundBlock> batch = new ArrayList<>();

                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            var state = section.getBlockState(x, y, z);
                            if (this.blocks.get().contains(state.getBlock())) {
                                batch.add(new FoundBlock(state.getBlock(), new BlockPos(startX + x, minY + y, startZ + z)));
                            }
                        }
                    }
                }

                if (!batch.isEmpty()) {
                    BlackOut.mc.execute(() -> {
                        for (FoundBlock fb : batch) {
                            this.onBlock(fb.block(), fb.pos, false);
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void checkChunks() {
        ClientChunkManager.ClientChunkMap map = BlackOut.mc.world.getChunkManager().chunks;
        Set<ChunkPos> currentChunks = new HashSet<>();

        for (int i = 0; i < map.chunks.length(); i++) {
            WorldChunk chunk = map.chunks.get(i);
            if (chunk != null) currentChunks.add(chunk.getPos());
        }

        for (ChunkPos pos : currentChunks) {
            if (!prevChunks.contains(pos)) {
                if (!toScan.contains(pos)) toScan.add(pos);
            }
        }

        prevChunks.removeIf(pos -> {
            if (!currentChunks.contains(pos)) {
                this.unScan(pos);
                return true;
            }
            return false;
        });
        prevChunks.addAll(currentChunks);
    }

    private void unScan(ChunkPos pos) {
        this.toScan.remove(pos);
        this.positions.keySet().removeIf(p ->
                p.getX() >= pos.getStartX() && p.getX() <= pos.getEndX() &&
                        p.getZ() >= pos.getStartZ() && p.getZ() <= pos.getEndZ()
        );
    }

    private void onBlock(Block block, BlockPos pos, boolean updateNeighbors) {
        boolean valid = this.blocks.get().contains(block);

        if (valid && this.onlyExposed.get()) {
            valid = false;
            for (Direction dir : Direction.values()) {
                if (!BlackOut.mc.world.getBlockState(pos.offset(dir)).isOpaque()) {
                    valid = true;
                    break;
                }
            }
        }

        if (valid) {
            if (!this.positions.containsKey(pos)) {
                this.positions.put(pos, this.getBox(pos));
            }
        } else {
            this.positions.remove(pos);
        }

        if (updateNeighbors) {
            for (Direction dir : Direction.values()) {
                BlockPos offsetPos = pos.offset(dir);
                this.onBlock(BlackOut.mc.world.getBlockState(offsetPos).getBlock(), offsetPos, false);
            }
        }
    }

    private Box getBox(BlockPos pos) {
        if (this.dynamicBox.get()) {
            VoxelShape shape = BlackOut.mc.world.getBlockState(pos).getOutlineShape(BlackOut.mc.world, pos);
            if (!shape.isEmpty()) return shape.getBoundingBox().offset(pos);
        }
        return BoxUtils.get(pos);
    }

    private record FoundBlock(Block block, BlockPos pos) {}
}