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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Search extends Module {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRender = this.addGroup("Visuals");

    private final Map<BlockPos, AABB> positions = new ConcurrentHashMap<>();
    private final Map<ChunkPos, Set<BlockPos>> chunkedPositions = new ConcurrentHashMap<>();
    private final Set<ChunkPos> prevChunks = new HashSet<>();
    private final Queue<ChunkPos> toScan = new ConcurrentLinkedQueue<>();

    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BlackOut-Search-Scanner");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private volatile Set<Block> blockSet = Set.of();

    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Target Blocks", "The specific block types to locate.")
            .onChanged(ignored -> refresh());
    private final Setting<Boolean> dynamicBox = this.sgGeneral.booleanSetting("Voxel Bounds", true, "Adjusts highlight to match the exact block shape.")
            .onChanged(ignored -> refresh());
    private final Setting<Boolean> instantScan = this.sgGeneral.booleanSetting("Force Scan", false, "Scans all loaded chunks immediately.");
    private final Setting<Integer> scanSpeed = this.sgGeneral.intSetting("Iteration Rate", 1, 1, 10, 1, "Chunks per frame during scan.", () -> !this.instantScan.get());
    private final Setting<Boolean> onlyExposed = this.sgGeneral.booleanSetting("Culling", false, "Only highlights blocks exposed to air.")
            .onChanged(ignored -> refresh());

    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgRender);

    public Search() {
        super("Search", "Locates blocks using all CPU cores and advanced palette culling.", SubCategory.WORLD, true);
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Event
    public void onJoin(GameJoinEvent event) {
        this.reset();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.level != null) {
            this.checkChunks();
            this.find();
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (positions.isEmpty()) return;
        for (AABB box : positions.values()) {
            rendering.render(box);
        }
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
        this.chunkedPositions.clear();
        this.rebuildBlockSet();
    }

    private void refresh() {
        this.rebuildBlockSet();
        if (BlackOut.mc.level == null) return;
        positions.clear();
        chunkedPositions.clear();
        for (ChunkPos pos : prevChunks) {
            if (!toScan.contains(pos)) {
                toScan.add(pos);
            }
        }
    }

    private void rebuildBlockSet() {
        List<Block> list = this.blocks.get();
        if (list.isEmpty()) {
            this.blockSet = Set.of();
        } else {
            this.blockSet = new ObjectOpenHashSet<>(list);
        }
    }

    private void find() {
        if (toScan.isEmpty()) return;

        int limit = instantScan.get() ? toScan.size() : scanSpeed.get();
        for (int i = 0; i < limit; i++) {
            ChunkPos pos = toScan.poll();
            if (pos == null) break;

            scanExecutor.execute(() -> this.scan(pos));
        }
    }

    private void scan(ChunkPos pos) {
        try {
            var chunkView = BlackOut.mc.level.getChunkSource().getChunkForLighting(pos.x, pos.z);
            if (!(chunkView instanceof LevelChunk chunk) || chunk.isEmpty()) {
                return;
            }

            Set<Block> targets = this.blockSet;
            if (targets.isEmpty()) return;

            LevelChunkSection[] sections = chunk.getSections();
            List<BlockPos> batch = new ArrayList<>(32);

            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                if (section == null || section.hasOnlyAir()) continue;

                if (!section.getStates().maybeHas(state -> targets.contains(state.getBlock()))) {
                    continue;
                }

                int startX = pos.getMinBlockX();
                int startZ = pos.getMinBlockZ();
                int minY = chunk.getMinY() + (i * 16);

                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            var state = section.getBlockState(x, y, z);
                            if (targets.contains(state.getBlock())) {
                                batch.add(new BlockPos(startX + x, minY + y, startZ + z));
                            }
                        }
                    }
                }
            }

            if (!batch.isEmpty()) {
                BlackOut.mc.execute(() -> {
                    for (BlockPos bp : batch) {
                        this.onBlock(BlackOut.mc.level.getBlockState(bp).getBlock(), bp, false);
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    private void checkChunks() {
        ClientChunkCache.Storage map = BlackOut.mc.level.getChunkSource().storage;
        int length = map.chunks.length();
        Set<ChunkPos> currentChunks = new HashSet<>();

        for (int i = 0; i < length; i++) {
            LevelChunk chunk = map.chunks.get(i);
            if (chunk != null) {
                currentChunks.add(chunk.getPos());
            }
        }

        for (ChunkPos pos : currentChunks) {
            if (!prevChunks.contains(pos) && !toScan.contains(pos)) {
                toScan.add(pos);
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
        Set<BlockPos> chunkBlocks = this.chunkedPositions.remove(pos);
        if (chunkBlocks != null) {
            for (BlockPos bp : chunkBlocks) {
                this.positions.remove(bp);
            }
        }
    }

    private void onBlock(Block block, BlockPos pos, boolean updateNeighbors) {
        Set<Block> targets = this.blockSet;
        boolean valid = targets.contains(block);

        if (valid && this.onlyExposed.get()) {
            valid = false;
            for (Direction dir : DIRECTIONS) {
                if (!BlackOut.mc.level.getBlockState(pos.relative(dir)).canOcclude()) {
                    valid = true;
                    break;
                }
            }
        }

        if (valid) {
            if (!this.positions.containsKey(pos)) {
                this.positions.put(pos, this.getBox(pos));
                ChunkPos cp = new ChunkPos(pos);
                this.chunkedPositions.computeIfAbsent(cp, k -> new ObjectOpenHashSet<>()).add(pos);
            }
        } else {
            AABB removed = this.positions.remove(pos);
            if (removed != null) {
                ChunkPos cp = new ChunkPos(pos);
                Set<BlockPos> set = this.chunkedPositions.get(cp);
                if (set != null) {
                    set.remove(pos);
                    if (set.isEmpty()) {
                        this.chunkedPositions.remove(cp);
                    }
                }
            }
        }

        if (updateNeighbors) {
            for (Direction dir : DIRECTIONS) {
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
}