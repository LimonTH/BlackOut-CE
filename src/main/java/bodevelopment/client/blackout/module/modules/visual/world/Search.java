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
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Search extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRender = this.addGroup("Visuals");

    private final Map<BlockPos, AABB> positions = new ConcurrentHashMap<>();
    private final Set<ChunkPos> prevChunks = new HashSet<>();
    private final Queue<ChunkPos> toScan = new ConcurrentLinkedQueue<>();

    private final ForkJoinPool pool = new ForkJoinPool();

    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Target Blocks", "The specific block types to locate.").onChanged(ignored -> refresh());;
    private final Setting<Boolean> dynamicBox = this.sgGeneral.booleanSetting("Voxel Bounds", true, "Adjusts highlight to match the exact block shape.").onChanged(ignored -> refresh());
    private final Setting<Boolean> instantScan = this.sgGeneral.booleanSetting("Force Scan", false, "Scans all loaded chunks immediately.");
    private final Setting<Integer> scanSpeed = this.sgGeneral.intSetting("Iteration Rate", 1, 1, 10, 1, "Chunks per frame during scan.", () -> !this.instantScan.get());
    private final Setting<Boolean> onlyExposed = this.sgGeneral.booleanSetting("Culling", false, "Only highlights blocks exposed to air.").onChanged(ignored -> refresh());;

    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgRender);

    public Search() {
        super("Search", "Locates blocks using all CPU cores and advanced palette culling.", SubCategory.WORLD, true);
    }

    @Override
    public void onEnable() { this.reset(); }

    @Event
    public void onJoin(GameJoinEvent event) { this.reset(); }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.level != null) {
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
        if (BlackOut.mc.level != null) {
            this.onBlock(event.state.getBlock(), event.pos, true);
        }
    }

    private void reset() {
        this.prevChunks.clear();
        this.toScan.clear();
        this.positions.clear();
    }

    private void refresh() {
        if (BlackOut.mc.level == null) return;
        positions.clear();
        for (ChunkPos pos : prevChunks) {
            if (!toScan.contains(pos)) {
                toScan.add(pos);
            }
        }
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
            var chunkView = BlackOut.mc.level.getChunkSource().getChunkForLighting(pos.x, pos.z);
            if (!(chunkView instanceof LevelChunk chunk) || chunk.isEmpty()) {
                return;
            }
            LevelChunkSection[] sections = chunk.getSections();

            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                if (section == null || section.hasOnlyAir()) continue;

                if (!section.getStates().maybeHas(state -> this.blocks.get().contains(state.getBlock()))) {
                    continue;
                }

                int startX = pos.getMinBlockX();
                int startZ = pos.getMinBlockZ();
                int minY = chunk.getMinY() + (i * 16);

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
        ClientChunkCache.Storage map = BlackOut.mc.level.getChunkSource().storage;
        Set<ChunkPos> currentChunks = new HashSet<>();

        for (int i = 0; i < map.chunks.length(); i++) {
            LevelChunk chunk = map.chunks.get(i);
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
                p.getX() >= pos.getMinBlockX() && p.getX() <= pos.getMaxBlockX() &&
                        p.getZ() >= pos.getMinBlockZ() && p.getZ() <= pos.getMaxBlockZ()
        );
    }

    private void onBlock(Block block, BlockPos pos, boolean updateNeighbors) {
        boolean valid = this.blocks.get().contains(block);

        if (valid && this.onlyExposed.get()) {
            valid = false;
            for (Direction dir : Direction.values()) {
                if (!BlackOut.mc.level.getBlockState(pos.relative(dir)).canOcclude()) {
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
                BlockPos offsetPos = pos.relative(dir);
                this.onBlock(BlackOut.mc.level.getBlockState(offsetPos).getBlock(), offsetPos, false);
            }
        }
    }

    private AABB getBox(BlockPos pos) {
        if (this.dynamicBox.get()) {
            VoxelShape shape = BlackOut.mc.level.getBlockState(pos).getShape(BlackOut.mc.level, pos);
            if (!shape.isEmpty()) return shape.bounds().move(pos);
        }
        return BoxUtils.get(pos);
    }

    private record FoundBlock(Block block, BlockPos pos) {}
}