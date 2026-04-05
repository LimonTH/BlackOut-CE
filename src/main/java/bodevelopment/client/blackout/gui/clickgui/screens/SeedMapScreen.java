package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.world.SeedFinder;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.FileUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.SeedBiomeSource;
import bodevelopment.client.blackout.util.render.ScissorStack;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class SeedMapScreen extends ClickGuiScreen {
    private static final int TILE_SIZE = 256;

    private final Map<Long, MapTile> tileCache = new ConcurrentHashMap<>();
    private final ExecutorService generatePool = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));

    private static final float MAP_WIDTH = 860.0F;
    private static final float MAP_HEIGHT = 500.0F;
    private static final float FOOTER_HEIGHT = 40.0F;
    private static final float MAP_X = 10.0F;
    private static final float MAP_Y = 5.0F;

    private static final Color GRID_COLOR = new Color(50, 55, 60, 120);
    private static final Color GRID_ORIGIN = new Color(120, 120, 130, 180);
    private static final Color PLAYER_COLOR = new Color(50, 255, 50, 255);
    private static final Color BUTTON_COLOR = new Color(60, 130, 255, 255);
    private static final Color BUTTON_HOVER = new Color(80, 150, 255, 255);
    private static final Color SLIME_COLOR = new Color(100, 200, 50, 60);
    private static final Color SLIME_BORDER = new Color(100, 200, 50, 120);
    private static final Color COORD_LABEL_COLOR = new Color(200, 200, 210, 180);

    private final String seedString;
    private SeedBiomeSource biomeSource;
    private ResourceKey<Level> lastDim = null;

    private final long seed;
    private float mapCenterX;
    private float mapCenterZ;
    private float blocksPerPixel = 8.0F;

    private boolean dragging = false;
    private double dragStartMx;
    private double dragStartMy;
    private float dragStartCenterX;
    private float dragStartCenterZ;
    private boolean hoveredStructure = false;

    private final TextField fieldX = new TextField();
    private final TextField fieldZ = new TextField();
    private final int fieldXId = SelectedComponent.nextId();
    private final int fieldZId = SelectedComponent.nextId();
    private boolean showSlimeChunks = false;

    public SeedMapScreen(String seedString, long seed, SeedBiomeSource biomeSource) {
        super("Seed Map", MAP_WIDTH + 20.0F, MAP_HEIGHT + FOOTER_HEIGHT + 10.0F, false);
        this.seedString = seedString;
        this.seed = seed;
        this.biomeSource = biomeSource;

        if (BlackOut.mc.player != null) {
            this.mapCenterX = (float) BlackOut.mc.player.getX();
            this.mapCenterZ = (float) BlackOut.mc.player.getZ();
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        tileCache.values().forEach(MapTile::close);
        tileCache.clear();
        generatePool.shutdownNow();
    }

    @Override
    public boolean closesOnRightClick() { return false; }

    @Override
    public void render() {
        if (BlackOut.mc.level != null) {
            ResourceKey<Level> currentDim = BlackOut.mc.level.dimension();
            if (currentDim != lastDim) {
                lastDim = currentDim;
                tileCache.values().forEach(MapTile::close);
                tileCache.clear();

                SeedFinder finder = Managers.MODULES.getModule(SeedFinder.class);
                if (finder != null) {
                    this.biomeSource = new SeedBiomeSource(this.seed, currentDim);
                }
            }
        }
        float mapRight = MAP_X + MAP_WIDTH;
        float mapBottom = MAP_Y + MAP_HEIGHT;

        var window = BlackOut.mc.getWindow();
        int sh = window.getScreenHeight();
        int sw = window.getScreenWidth();

        float screenCenterX = sw / 2.0F + this.x;
        float screenCenterY = sh / 2.0F + this.y;
        float mapScreenLeft = screenCenterX - this.width / 2.0F * this.unscaled + MAP_X * this.unscaled;
        float mapScreenBottom = screenCenterY - this.height / 2.0F * this.unscaled + MAP_Y * this.unscaled;

        int glX = Math.max(0, (int) mapScreenLeft);
        int glY = Math.max(0, (int) (sh - mapScreenBottom - MAP_HEIGHT * this.unscaled));
        int glW = (int) (MAP_WIDTH * this.unscaled);
        int glH = (int) (MAP_HEIGHT * this.unscaled);

        try (ScissorStack.Region ignored = ScissorStack.pushRaw(glX, glY, glW, glH)) {
            this.renderBiomeTexture();
            if (this.showSlimeChunks && this.lastDim != Level.NETHER && this.lastDim != Level.END) {
                this.renderSlimeChunks();
            }
            this.renderGrid(mapRight, mapBottom);
            this.hoveredStructure = false;
            this.renderStructures();
            this.renderPlayer();
        }

        // Footer
        float footerY = mapBottom + 5.0F;
        float footerH = FOOTER_HEIGHT - 5.0F;
        this.rounded(MAP_X, footerY, MAP_WIDTH, footerH, 5.0F, 5.0F, GuiColorUtils.bg1, ColorUtils.SHADOW100);

        // Info text
        String info = "Seed: " + this.seedString + " | Scale: " + String.format("%.1f", this.blocksPerPixel);
        this.text(info, 1.8F, MAP_X + 8.0F, footerY + footerH / 2.0F, false, true, Color.LIGHT_GRAY);

        // Coordinate input
        float fieldW = 60.0F;
        float fieldH = 18.0F;
        float fieldsX = MAP_X + 310.0F;
        float fieldsY = footerY + (footerH - fieldH) / 2.0F;
        Color fieldBg = new Color(40, 40, 45, 200);

        if (!SelectedComponent.is(this.fieldXId)) {
            this.fieldX.setContent(String.valueOf((int) this.mapCenterX));
        }
        if (!SelectedComponent.is(this.fieldZId)) {
            this.fieldZ.setContent(String.valueOf((int) this.mapCenterZ));
        }

        float xFieldX = fieldsX + 18.0F;
        this.text("X:", 1.8F, fieldsX, fieldsY + fieldH / 2.0F, false, true, Color.LIGHT_GRAY);
        this.fieldX.setActive(SelectedComponent.is(this.fieldXId));
        this.fieldX.render(this.stack, 1.5F, this.mx, this.my, xFieldX, fieldsY, fieldW, fieldH, 3.0F, 0.0F, Color.WHITE, fieldBg);

        float zLabelX = xFieldX + fieldW + 8.0F;
        float zFieldX = zLabelX + 18.0F;
        this.text("Z:", 1.8F, zLabelX, fieldsY + fieldH / 2.0F, false, true, Color.LIGHT_GRAY);
        this.fieldZ.setActive(SelectedComponent.is(this.fieldZId));
        this.fieldZ.render(this.stack, 1.5F, this.mx, this.my, zFieldX, fieldsY, fieldW, fieldH, 3.0F, 0.0F, Color.WHITE, fieldBg);

        // Go button
        float goBtnW = 30.0F;
        float goBtnX = zFieldX + fieldW + 8.0F;
        boolean goHovered = this.mx >= goBtnX && this.mx <= goBtnX + goBtnW && this.my >= fieldsY && this.my <= fieldsY + fieldH;
        this.rounded(goBtnX, fieldsY, goBtnW, fieldH, 3.0F, 0.0F, goHovered ? BUTTON_HOVER : BUTTON_COLOR, ColorUtils.SHADOW100);
        this.text("Go", 1.8F, goBtnX + goBtnW / 2.0F, fieldsY + fieldH / 2.0F, true, true, Color.WHITE);

        // Slime Chunks toggle
        float slimeBtnW = 95.0F;
        float slimeBtnX = goBtnX + goBtnW + 12.0F;
        boolean slimeHovered = this.mx >= slimeBtnX && this.mx <= slimeBtnX + slimeBtnW && this.my >= fieldsY && this.my <= fieldsY + fieldH;
        Color slimeBtnColor = this.showSlimeChunks ? new Color(80, 180, 50, 255) : (slimeHovered ? new Color(70, 80, 70, 255) : new Color(50, 55, 55, 255));
        this.rounded(slimeBtnX, fieldsY, slimeBtnW, fieldH, 3.0F, 0.0F, slimeBtnColor, ColorUtils.SHADOW100);
        this.text("Slime Chunks", 1.6F, slimeBtnX + slimeBtnW / 2.0F, fieldsY + fieldH / 2.0F, true, true, Color.WHITE);

        // Chunkbase button
        float btnW = 130.0F, btnH = 22.0F;
        float btnX = mapRight - btnW - 5.0F, btnY = footerY + (footerH - btnH) / 2.0F;
        boolean hovered = this.mx >= btnX && this.mx <= btnX + btnW && this.my >= btnY && this.my <= btnY + btnH;
        this.rounded(btnX, btnY, btnW, btnH, 4.0F, 0.0F, hovered ? BUTTON_HOVER : BUTTON_COLOR, ColorUtils.SHADOW100);
        this.text("Open Chunkbase", 2.0F, btnX + btnW / 2.0F, btnY + btnH / 2.0F, true, true, Color.WHITE);

        this.renderBiomeTooltip();
    }

    private void renderBiomeTexture() {
        if (this.biomeSource == null) return;

        float blocksPerTile = TILE_SIZE * this.blocksPerPixel;
        float worldLeft = this.mapCenterX - (MAP_WIDTH / 2.0F) * this.blocksPerPixel;
        float worldTop = this.mapCenterZ - (MAP_HEIGHT / 2.0F) * this.blocksPerPixel;

        int startTileX = (int) Math.floor(worldLeft / blocksPerTile);
        int startTileZ = (int) Math.floor(worldTop / blocksPerTile);
        int endTileX = (int) Math.ceil((worldLeft + MAP_WIDTH * this.blocksPerPixel) / blocksPerTile);
        int endTileZ = (int) Math.ceil((worldTop + MAP_HEIGHT * this.blocksPerPixel) / blocksPerTile);

        for (int tx = startTileX; tx <= endTileX; tx++) {
            for (int tz = startTileZ; tz <= endTileZ; tz++) {
                long key = ((long) tx << 32) | (tz & 0xFFFFFFFFL);
                int finalTx = tx;
                int finalTz = tz;
                MapTile tile = tileCache.computeIfAbsent(key, k -> new MapTile(finalTx, finalTz));

                if (!tile.ready) {
                    enqueueTileGeneration(tile, blocksPerTile);
                    continue;
                }

                float x = MAP_X + (tx * blocksPerTile - worldLeft) / this.blocksPerPixel;
                float y = MAP_Y + (tz * blocksPerTile - worldTop) / this.blocksPerPixel;
                drawTileQuad(tile, x, y, TILE_SIZE);
            }
        }
    }

    private void enqueueTileGeneration(MapTile tile, float blocksPerTile) {
        if (tile.generating) return;
        tile.generating = true;

        float bpp = this.blocksPerPixel;
        generatePool.execute(() -> {
            try {
                float startX = tile.tx * blocksPerTile;
                float startZ = tile.tz * blocksPerTile;

                int quartStep = Math.max(1, Math.round(4.0f / bpp));
                int step = Math.min(4, Math.max(2, quartStep));

                for (int ty = 0; ty < TILE_SIZE; ty += step) {
                    for (int tx = 0; tx < TILE_SIZE; tx += step) {
                        int bx = (int) (startX + (tx + 0.5F) * bpp);
                        int bz = (int) (startZ + (ty + 0.5F) * bpp);
                        int color = biomeSource.getBiomeColor(bx, bz);
                        int height = biomeSource.getHeight(bx, bz);

                        // Shading based on height (64 is baseline)
                        float factor = 0.95f + (height - 64) * 0.008f;
                        factor = Math.max(0.75f, Math.min(1.25f, factor));

                        int r = (int) (((color >> 16) & 0xFF) * factor);
                        int g = (int) (((color >> 8) & 0xFF) * factor);
                        int b = (int) ((color & 0xFF) * factor);
                        int a = (color >> 24) & 0xFF;

                        color = (Math.min(255, a) << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);

                        int maxY = Math.min(ty + step, TILE_SIZE);
                        int maxX = Math.min(tx + step, TILE_SIZE);
                        for (int fy = ty; fy < maxY; fy++) {
                            for (int fx = tx; fx < maxX; fx++) {
                                tile.image.setPixel(fx, fy, color);
                            }
                        }
                    }
                }

                RenderSystem.recordRenderCall(() -> {
                    tile.texture.upload();
                    RenderSystem.bindTexture(tile.texture.getId());
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                    tile.ready = true;
                });
            } catch (Exception e) {
                tile.generating = false;
            }
        });
    }

    private void drawTileQuad(MapTile tile, float x, float y, float size) {
        Matrix4f matrix = this.stack.last().pose();
        RenderSystem.setShaderTexture(0, tile.texture.getId());
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.enableBlend();

        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(matrix, x, y + size, 0).setUv(0, 1);
        buf.addVertex(matrix, x + size, y + size, 0).setUv(1, 1);
        buf.addVertex(matrix, x + size, y, 0).setUv(1, 0);
        buf.addVertex(matrix, x, y, 0).setUv(0, 0);
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    private void renderBiomeTooltip() {
        if (this.biomeSource == null || this.hoveredStructure) return;
        if (this.mx < MAP_X || this.mx > MAP_X + MAP_WIDTH || this.my < MAP_Y || this.my > MAP_Y + MAP_HEIGHT) return;

        float worldLeft = this.mapCenterX - (MAP_WIDTH / 2.0F) * this.blocksPerPixel;
        float worldTop = this.mapCenterZ - (MAP_HEIGHT / 2.0F) * this.blocksPerPixel;
        int blockX = (int) (worldLeft + (this.mx - MAP_X) * this.blocksPerPixel);
        int blockZ = (int) (worldTop + (this.my - MAP_Y) * this.blocksPerPixel);

        var biomeKey = this.biomeSource.getBiome(blockX, blockZ);
        String biomeName = biomeKey.location().getPath().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : biomeName.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        String label = sb + " (" + blockX + ", " + blockZ + ")";

        float scale = 1.8F;
        float tw = BlackOut.FONT.getWidth(label) * scale + 10;
        float tx = (float) this.mx + 12;
        float ty = (float) this.my - 20;
        this.rounded(tx, ty, tw, 16.0F, 3.0F, 0.0F, new Color(20, 20, 20, 220), ColorUtils.SHADOW100);
        this.text(label, scale, tx + tw / 2.0F, ty + 8, true, true, Color.WHITE);
    }

    private void renderGrid(float mapRight, float mapBottom) {
        float worldLeft = this.mapCenterX - (MAP_WIDTH / 2.0F) * this.blocksPerPixel;
        float worldTop = this.mapCenterZ - (MAP_HEIGHT / 2.0F) * this.blocksPerPixel;

        float gridSpacing = this.getGridSpacing();

        float startWorldX = (float) (Math.floor(worldLeft / gridSpacing) * gridSpacing);
        float startWorldZ = (float) (Math.floor(worldTop / gridSpacing) * gridSpacing);

        for (float wx = startWorldX; wx <= worldLeft + MAP_WIDTH * this.blocksPerPixel; wx += gridSpacing) {
            float sx = MAP_X + (wx - worldLeft) / this.blocksPerPixel;
            if (sx < MAP_X || sx > mapRight) continue;
            Color c = Math.abs(wx) < 0.5F ? GRID_ORIGIN : GRID_COLOR;
            this.line(sx, MAP_Y, sx, mapBottom, c);

            String label = String.valueOf((int) wx);
            this.text(label, 1.3F, sx + 2, MAP_Y + 2, false, false, COORD_LABEL_COLOR);
        }
        for (float wz = startWorldZ; wz <= worldTop + MAP_HEIGHT * this.blocksPerPixel; wz += gridSpacing) {
            float sy = MAP_Y + (wz - worldTop) / this.blocksPerPixel;
            if (sy < MAP_Y || sy > mapBottom) continue;
            Color c = Math.abs(wz) < 0.5F ? GRID_ORIGIN : GRID_COLOR;
            this.line(MAP_X, sy, mapRight, sy, c);

            String label = String.valueOf((int) wz);
            this.text(label, 1.3F, MAP_X + 2, sy + 2, false, false, COORD_LABEL_COLOR);
        }
    }

    private final List<SeedFinder.FoundStructure> mapFound = new java.util.ArrayList<>();
    private float lastMapX, lastMapZ, lastBpp;
    private ResourceKey<Level> lastMapDim;

    private void renderStructures() {
        SeedFinder seedFinder = Managers.MODULES.getModule(SeedFinder.class);
        if (seedFinder == null || this.biomeSource == null) return;

        float worldLeft = this.mapCenterX - (MAP_WIDTH / 2.0F) * this.blocksPerPixel;
        float worldTop = this.mapCenterZ - (MAP_HEIGHT / 2.0F) * this.blocksPerPixel;
        float worldRight = worldLeft + MAP_WIDTH * this.blocksPerPixel;
        float worldBottom = worldTop + MAP_HEIGHT * this.blocksPerPixel;

        float markerSize = Math.max(3.5F, 8.0F / (this.blocksPerPixel / 4.0F));

        if (Math.abs(mapCenterX - lastMapX) > 100 * blocksPerPixel || Math.abs(mapCenterZ - lastMapZ) > 100 * blocksPerPixel || blocksPerPixel != lastBpp || lastDim != lastMapDim) {
            mapFound.clear();
            int radius = (int) (Math.max(MAP_WIDTH, MAP_HEIGHT) * blocksPerPixel);
            SeedFinder.findInArea(mapFound, seed, biomeSource, lastDim, (int) mapCenterX, (int) mapCenterZ, radius, false, t -> seedFinder.isDimensionEnabled(t, lastDim));

            lastMapX = mapCenterX;
            lastMapZ = mapCenterZ;
            lastBpp = blocksPerPixel;
            lastMapDim = lastDim;
        }

        boolean hideMinor = this.blocksPerPixel >= 16.0F;

        for (SeedFinder.FoundStructure s : mapFound) {
            if (hideMinor && !s.type().isMajor()) continue;
            if (s.blockX() < worldLeft || s.blockX() > worldRight || s.blockZ() < worldTop || s.blockZ() > worldBottom) continue;

            float sx = MAP_X + (s.blockX() - worldLeft) / this.blocksPerPixel;
            float sy = MAP_Y + (s.blockZ() - worldTop) / this.blocksPerPixel;
            drawMarker(sx, sy, markerSize, s.type().displayName + s.extraInfo(), s.type().mapColor, s.blockX(), s.blockZ());
        }
    }

    private void drawMarker(float sx, float sy, float size, String name, Color color, int bx, int bz) {
        float half = size / 2.0F;
        this.quad(sx - half - 0.5F, sy - half - 0.5F, size + 1.0F, size + 1.0F, new Color(0, 0, 0, 180));
        this.quad(sx - half, sy - half, size, size, color);

        if (this.blocksPerPixel < 16.0F) {
            this.text(name, 1.5F, sx, sy - half - 4.0F, true, true, Color.WHITE);
        }

        if (this.mx >= sx - half - 2 && this.mx <= sx + half + 2 && this.my >= sy - half - 2 && this.my <= sy + half + 2) {
            this.hoveredStructure = true;
            String tooltip = name + " [" + bx + ", " + bz + "]";
            float scale = 1.8F;
            float tw = BlackOut.FONT.getWidth(tooltip) * scale + 10;
            float tx = (float) this.mx + 10;
            float ty = (float) this.my - 10;
            this.rounded(tx, ty, tw, 16.0F, 3.0F, 0.0F, new Color(25, 25, 25, 240), ColorUtils.SHADOW100);
            this.text(tooltip, scale, tx + tw / 2.0F, ty + 8.0F, true, true, Color.WHITE);
        }
    }

    private void renderSlimeChunks() {
        float worldLeft = this.mapCenterX - (MAP_WIDTH / 2.0F) * this.blocksPerPixel;
        float worldTop = this.mapCenterZ - (MAP_HEIGHT / 2.0F) * this.blocksPerPixel;
        float worldRight = worldLeft + MAP_WIDTH * this.blocksPerPixel;
        float worldBottom = worldTop + MAP_HEIGHT * this.blocksPerPixel;

        int minChunkX = Math.floorDiv((int) worldLeft, 16);
        int maxChunkX = Math.ceilDiv((int) worldRight, 16);
        int minChunkZ = Math.floorDiv((int) worldTop, 16);
        int maxChunkZ = Math.ceilDiv((int) worldBottom, 16);

        long totalChunks = (long) (maxChunkX - minChunkX) * (maxChunkZ - minChunkZ);
        if (totalChunks > 10000) return;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!isSlimeChunk(this.seed, cx, cz)) continue;

                float x1 = MAP_X + (cx * 16 - worldLeft) / this.blocksPerPixel;
                float z1 = MAP_Y + (cz * 16 - worldTop) / this.blocksPerPixel;
                float chunkPx = 16.0F / this.blocksPerPixel;

                this.quad(x1, z1, chunkPx, chunkPx, SLIME_COLOR);
                this.line(x1, z1, x1 + chunkPx, z1, SLIME_BORDER);
                this.line(x1, z1 + chunkPx, x1 + chunkPx, z1 + chunkPx, SLIME_BORDER);
                this.line(x1, z1, x1, z1 + chunkPx, SLIME_BORDER);
                this.line(x1 + chunkPx, z1, x1 + chunkPx, z1 + chunkPx, SLIME_BORDER);
            }
        }
    }

    private static boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
        return new Random(
                seed
                + ((long) chunkX * chunkX * 0x4c1906)
                + (chunkX * 0x5ac0dbL)
                + (long) chunkZ * chunkZ * 0x4307a7L
                + (chunkZ * 0x5f24fL)
                ^ 0x3ad8025fL
        ).nextInt(10) == 0;
    }

    private void renderPlayer() {
        if (BlackOut.mc.player == null) return;

        float worldLeft = this.mapCenterX - (MAP_WIDTH / 2.0F) * this.blocksPerPixel;
        float worldTop = this.mapCenterZ - (MAP_HEIGHT / 2.0F) * this.blocksPerPixel;

        float px = MAP_X + ((float) BlackOut.mc.player.getX() - worldLeft) / this.blocksPerPixel;
        float py = MAP_Y + ((float) BlackOut.mc.player.getZ() - worldTop) / this.blocksPerPixel;

        if (px >= MAP_X && px <= MAP_X + MAP_WIDTH && py >= MAP_Y && py <= MAP_Y + MAP_HEIGHT) {
            float size = 6.0F;
            this.quad(px - size / 2 - 1, py - size / 2 - 1, size + 2, size + 2, new Color(0, 0, 0, 200));
            this.quad(px - size / 2, py - size / 2, size, size, PLAYER_COLOR);
            this.text("You", 1.8F, px + 1, py - size / 2 - 6.0F, true, true, new Color(0, 0, 0, 160));
            this.text("You", 1.8F, px, py - size / 2 - 7.0F, true, true, PLAYER_COLOR);
        }
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (button == 0 && state) {
            float mapBottom = MAP_Y + MAP_HEIGHT;
            float footerY = mapBottom + 5.0F;
            float footerH = FOOTER_HEIGHT - 5.0F;

            float fieldW = 60.0F;
            float fieldH = 18.0F;
            float fieldsX = MAP_X + 310.0F;
            float fieldsY = footerY + (footerH - fieldH) / 2.0F;
            float xFieldX = fieldsX + 18.0F;
            float zLabelX = xFieldX + fieldW + 8.0F;
            float zFieldX = zLabelX + 18.0F;

            // Text field clicks
            if (this.mx >= xFieldX && this.mx <= xFieldX + fieldW && this.my >= fieldsY && this.my <= fieldsY + fieldH) {
                SelectedComponent.setId(this.fieldXId);
                this.fieldX.click(button, state);
                return;
            }
            if (this.mx >= zFieldX && this.mx <= zFieldX + fieldW && this.my >= fieldsY && this.my <= fieldsY + fieldH) {
                SelectedComponent.setId(this.fieldZId);
                this.fieldZ.click(button, state);
                return;
            }

            SelectedComponent.reset();

            // Go button
            float goBtnW = 30.0F;
            float goBtnX = zFieldX + fieldW + 8.0F;
            if (this.mx >= goBtnX && this.mx <= goBtnX + goBtnW && this.my >= fieldsY && this.my <= fieldsY + fieldH) {
                this.goToCoordinates();
                return;
            }

            // Slime Chunks toggle
            float slimeBtnW = 95.0F;
            float slimeBtnX = goBtnX + goBtnW + 12.0F;
            if (this.mx >= slimeBtnX && this.mx <= slimeBtnX + slimeBtnW && this.my >= fieldsY && this.my <= fieldsY + fieldH) {
                this.showSlimeChunks = !this.showSlimeChunks;
                return;
            }

            // Chunkbase button
            float btnW = 130.0F;
            float btnH = 22.0F;
            float btnX = MAP_X + MAP_WIDTH - btnW - 5.0F;
            float btnY = footerY + (footerH - btnH) / 2.0F;
            if (this.mx >= btnX && this.mx <= btnX + btnW && this.my >= btnY && this.my <= btnY + btnH) {
                String dim = this.lastDim == Level.NETHER ? "nether" : this.lastDim == Level.END ? "end" : "overworld";
                String url = "https://www.chunkbase.com/apps/seed-map#seed=" + this.seedString + "&platform=java_1_21_4&dimension=" + dim + "&x=" + (int) this.mapCenterX + "&z=" + (int) this.mapCenterZ;
                FileUtils.openLink(url);
                return;
            }

            if (this.mx >= MAP_X && this.mx <= MAP_X + MAP_WIDTH && this.my >= MAP_Y && this.my <= MAP_Y + MAP_HEIGHT) {
                this.dragging = true;
                this.dragStartMx = this.mx;
                this.dragStartMy = this.my;
                this.dragStartCenterX = this.mapCenterX;
                this.dragStartCenterZ = this.mapCenterZ;
            }
        } else if (button == 0) {
            this.dragging = false;
        }
    }

    private void goToCoordinates() {
        try {
            String xText = this.fieldX.getContent().trim();
            String zText = this.fieldZ.getContent().trim();
            if (!xText.isEmpty()) this.mapCenterX = Float.parseFloat(xText);
            if (!zText.isEmpty()) this.mapCenterZ = Float.parseFloat(zText);
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public void onKey(int key, boolean state) {
        if (SelectedComponent.is(this.fieldXId)) {
            if (key == GLFW.GLFW_KEY_ENTER && state) {
                this.goToCoordinates();
                SelectedComponent.reset();
            } else if (key == GLFW.GLFW_KEY_TAB && state) {
                SelectedComponent.setId(this.fieldZId);
            } else {
                this.fieldX.type(key, state);
            }
            return;
        }
        if (SelectedComponent.is(this.fieldZId)) {
            if (key == GLFW.GLFW_KEY_ENTER && state) {
                this.goToCoordinates();
                SelectedComponent.reset();
            } else if (key == GLFW.GLFW_KEY_TAB && state) {
                SelectedComponent.setId(this.fieldXId);
            } else {
                this.fieldZ.type(key, state);
            }
            return;
        }

        if (!state) return;

        if (key == GLFW.GLFW_KEY_R) {
            if (BlackOut.mc.player != null) {
                this.mapCenterX = (float) BlackOut.mc.player.getX();
                this.mapCenterZ = (float) BlackOut.mc.player.getZ();
            } else {
                this.mapCenterX = 0;
                this.mapCenterZ = 0;
            }
            this.blocksPerPixel = 8.0F;
        }
    }

    @Override
    public boolean handleScroll(double horizontal, double vertical) {
        if (!this.insideBounds() || this.mx < MAP_X || this.mx > MAP_X + MAP_WIDTH || this.my < MAP_Y || this.my > MAP_Y + MAP_HEIGHT)
            return super.handleScroll(horizontal, vertical);

        float oldBpp = this.blocksPerPixel;
        if (vertical > 0) this.blocksPerPixel = Math.max(0.5F, this.blocksPerPixel * 0.8F);
        else if (vertical < 0) this.blocksPerPixel = Math.min(64.0F, this.blocksPerPixel * 1.25F);

        if (oldBpp != this.blocksPerPixel) {
            float relX = ((float) this.mx - MAP_X) / MAP_WIDTH - 0.5F;
            float relZ = ((float) this.my - MAP_Y) / MAP_HEIGHT - 0.5F;
            this.mapCenterX += relX * MAP_WIDTH * (oldBpp - this.blocksPerPixel);
            this.mapCenterZ += relZ * MAP_HEIGHT * (oldBpp - this.blocksPerPixel);

            tileCache.values().forEach(MapTile::close);
            tileCache.clear();
            return true;
        }
        return false;
    }

    public void updateDrag() {
        if (this.dragging) {
            double dx = this.mx - this.dragStartMx;
            double dy = this.my - this.dragStartMy;
            this.mapCenterX = (float) (this.dragStartCenterX - dx * this.blocksPerPixel);
            this.mapCenterZ = (float) (this.dragStartCenterZ - dy * this.blocksPerPixel);
        }
    }

    @Override
    public void onRender(float frameTime, double mouseX, double mouseY) {
        super.onRender(frameTime, mouseX, mouseY);
        this.updateDrag();
    }

    private float getGridSpacing() {
        float viewWidth = MAP_WIDTH * this.blocksPerPixel;
        if (viewWidth > 50000) return 10000;
        if (viewWidth > 10000) return 2000;
        if (viewWidth > 5000) return 1000;
        if (viewWidth > 2000) return 500;
        if (viewWidth > 500) return 100;
        if (viewWidth > 100) return 16;
        return 16;
    }

    public record StructureEntry(String name, int blockX, int blockZ, Color color) {}

    private static class MapTile {
        public final int tx, tz;
        public final DynamicTexture texture;
        public final NativeImage image;
        public volatile boolean ready = false;
        public volatile boolean generating = false;

        public MapTile(int tx, int tz) {
            this.tx = tx;
            this.tz = tz;
            this.image = new NativeImage(256, 256, false);
            this.texture = new DynamicTexture(this.image);
        }

        public void close() {
            RenderSystem.recordRenderCall(() -> {
                texture.close();
                image.close();
            });
        }
    }
}
