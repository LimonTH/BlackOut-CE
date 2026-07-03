/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

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
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.ScreenUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Search extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRender = this.addGroup("Visuals");

    private final Setting<Boolean> instantScan = this.sgGeneral.booleanSetting("Force Scan", false, "Scans all loaded chunks immediately.");
    private final Setting<Integer> scanSpeed = this.sgGeneral.intSetting("Iteration Rate", 1, 1, 10, 1, "Chunks per frame during scan.", () -> !this.instantScan.get());
    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Target Blocks", "The specific block types to locate.").onChanged(ignored -> refresh());
    private final Setting<Boolean> dynamicBox = this.sgGeneral.booleanSetting("Voxel Bounds", true, "Adjusts highlight to match the exact block shape.").onChanged(ignored -> refresh());
    private final Setting<Boolean> tracers = this.sgGeneral.booleanSetting("Tracers", false, "Draws 2D tracer lines from the center of the screen to each found block.");
    private final Setting<Integer> tracerWidth = this.sgGeneral.intSetting("Tracer Width", 2, 1, 10, 1, "Thickness of the tracer lines.", () -> this.tracers.get());
    private final Setting<BlackOutColor> tracerColor = this.sgGeneral.colorSetting("Tracer Color", new BlackOutColor(255, 255, 255, 100), "Default tracer color when no per-block color is set.", () -> this.tracers.get());
    private final Setting<Boolean> onlyExposed = this.sgGeneral.booleanSetting("Culling", false, "Only highlights blocks exposed to air.").onChanged(ignored -> refresh());

    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgRender);

    private final PoseStack stack = new PoseStack();
    private volatile Set<Block> blockSet = Set.of();
    private static final Direction[] DIRECTIONS = Direction.values();
    private final Map<BlockPos, AABB> positions = new ConcurrentHashMap<>();
    private final Map<BlockPos, Block> blockTypes = new ConcurrentHashMap<>();
    private final Map<ChunkPos, Set<BlockPos>> chunkedPositions = new ConcurrentHashMap<>();
    private final Set<ChunkPos> prevChunks = new HashSet<>();
    private final Queue<ChunkPos> toScan = new ConcurrentLinkedQueue<>();
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "BlackOut-Search-Scanner");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    public Search() {
        super("Search", "Locates blocks using all CPU cores and advanced palette culling.", SubCategory.WORLD, true);
        ((ListSetting<Block>) this.blocks).withItemColors(
                () -> this.rendering.lineColor.get().getColor(),
                () -> this.rendering.sideColor.get().getColor(),
                this.rendering.shape::get
        ).snapshotDefaults();
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
        for (Map.Entry<BlockPos, AABB> entry : positions.entrySet()) {
            BlockPos pos = entry.getKey();
            AABB box = entry.getValue();
            Block block = blockTypes.get(pos);

            if (block != null) {
                ListSetting<Block> listBlocks = (ListSetting<Block>) this.blocks;
                Color lineCol = listBlocks.getItemData(block, "lineColor");
                Color sideCol = listBlocks.getItemData(block, "sideColor");
                BlackOutColor lineBC = lineCol != null ? new BlackOutColor(lineCol.getRed(), lineCol.getGreen(), lineCol.getBlue(), lineCol.getAlpha()) : null;
                BlackOutColor sideBC = sideCol != null ? new BlackOutColor(sideCol.getRed(), sideCol.getGreen(), sideCol.getBlue(), sideCol.getAlpha()) : null;
                if (lineBC != null || sideBC != null) {
                    rendering.render(box, lineBC, sideBC);
                    continue;
                }
            }

            rendering.render(box);
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (!this.tracers.get() || BlackOut.mc.level == null || BlackOut.mc.player == null || positions.isEmpty())
            return;

        PoseStack poseStack = event.context.pose();
        ScreenUtils.beginPixelSpace(poseStack);

        for (BlockPos pos : positions.keySet()) {
            Block block = blockTypes.get(pos);
            Color color = null;

            if (block != null) {
                Color customColor = ((ListSetting<Block>) this.blocks).getItemData(block, "color");
                if (customColor != null) {
                    color = customColor;
                }
            }

            if (color == null) {
                color = this.tracerColor.get().getColor();
            }

            this.renderTracer(poseStack, pos, color);
        }

        ScreenUtils.endPixelSpace(poseStack);
    }

    private void renderTracer(PoseStack poseStack, BlockPos pos, Color color) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        Vec2 screenPos = Render2DUtils.getCoords(x, y, z, false);
        if (screenPos == null) return;

        int width = this.tracerWidth.get();
        if (width <= 1) {
            Render2DUtils.line(poseStack,
                    ScreenUtils.screenWidth() / 2.0F,
                    ScreenUtils.screenHeight() / 2.0F,
                    screenPos.x,
                    screenPos.y,
                    color.getRGB()
            );
        } else {
            Render2DUtils.line(poseStack,
                    ScreenUtils.screenWidth() / 2.0F,
                    ScreenUtils.screenHeight() / 2.0F,
                    screenPos.x,
                    screenPos.y,
                    color.getRGB(),
                    width
            );
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
        this.blockTypes.clear();
        this.chunkedPositions.clear();
        this.rebuildBlockSet();
    }

    private void refresh() {
        this.rebuildBlockSet();
        if (BlackOut.mc.level == null) return;
        positions.clear();
        blockTypes.clear();
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
                this.blockTypes.remove(bp);
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
                this.blockTypes.put(pos, block);
                ChunkPos cp = new ChunkPos(pos);
                this.chunkedPositions.computeIfAbsent(cp, k -> new ObjectOpenHashSet<>()).add(pos);
            }
        } else {
            AABB removed = this.positions.remove(pos);
            if (removed != null) {
                this.blockTypes.remove(pos);
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