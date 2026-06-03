package bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Experimental;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.gui.clickgui.screens.SeedMapScreen;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.world.EndCitySimulationUtils;
import bodevelopment.client.blackout.util.world.LootTableSimulationUtils;
import bodevelopment.client.blackout.util.world.SeedSourceUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

@Experimental
public class SeedSearcher extends Module {
    private static final long REGION_X_MULT = 341873128712L;
    private static final long REGION_Z_MULT = 132897987541L;

    private static final int NETHER_SPACING = 27;
    private static final int NETHER_SEPARATION = 4;
    private static final int NETHER_SALT = 30084232;

    private static final Set<ResourceKey<Biome>> OVERWORLD_CAVE_BIOMES = Set.of(
            Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK
    );

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgStructures = this.addGroup("Structures");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<String> seed = this.sgGeneral.stringSetting("Seed", "", "World seed for structure generation.")
            .onChanged(v -> this.recalculate());
    private final Setting<Integer> searchRadius = this.sgGeneral.intSetting("Radius", 10000, 1000, 100000, 1000, "Search radius in blocks from the player.")
            .onChanged(v -> this.recalculate());
    private final Setting<Integer> renderDistance = this.sgGeneral.intSetting("Render Distance", 2000, 100, 20000, 100, "Max distance for 3D beam rendering.");
    private final Setting<Integer> recalcDistance = this.sgGeneral.intSetting("Recalc Distance", 512, 64, 2048, 64, "Distance the player must move before recalculating.");
    private final Setting<KeyBind> mapKey = this.sgGeneral.keySetting("Map Key", "Keybind to open the 2D seed map screen.");

    private final Setting<Boolean> enchantedApples = this.sgGeneral.booleanSetting("Enchanted Apples", false,
            "Search for chests containing Enchanted Golden Apples in all structures.")
            .onChanged(v -> this.recalculate());

    private final Setting<List<StructureType>> appleStructures = this.sgStructures.listSetting(
            "Structures",
            "Select which structures to scan for Enchanted Golden Apples.",
            this.enchantedApples::get,
            Arrays.stream(StructureType.values())
                    .filter(t -> LootTableSimulationUtils.canHaveEnchantedApple(t))
                    .collect(java.util.stream.Collectors.toList()),
            t -> t.displayName,
            StructureType.DUNGEON, StructureType.MINESHAFT, StructureType.ANCIENT_CITY,
            StructureType.BASTION_REMNANT, StructureType.DESERT_PYRAMID,
            StructureType.RUINED_PORTAL, StructureType.WOODLAND_MANSION,
            StructureType.TRIAL_CHAMBERS
    ).onChanged(v -> this.recalculate());

    private final Setting<List<StructureType>> structures = this.sgStructures.listSetting(
            "Structures",
            "Select which structures to search for.",
            () -> !this.enchantedApples.get(),
            Arrays.asList(StructureType.values()),
            t -> t.displayName,
            StructureType.VILLAGE, StructureType.STRONGHOLD
    ).onChanged(v -> this.recalculate());

    private final Setting<BlackOutColor> beamColor = this.sgRender.colorSetting("Beam Color", new BlackOutColor(255, 255, 50, 120), "Color for the structure beam.");
    private final Setting<BlackOutColor> textColor = this.sgRender.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), "Color for the structure label.");
    private final Setting<Double> textScale = this.sgRender.doubleSetting("Text Scale", 3.0, 0.5, 10.0, 0.5, "Scale of the structure label.");
    private final Setting<Integer> beamHeight = this.sgRender.intSetting("Beam Height", 256, 32, 512, 16, "Height of the structure beam.");
    private final Setting<Double> beamWidth = this.sgRender.doubleSetting("Beam Width", 0.5, 0.1, 2.0, 0.1, "Width of the structure beam.");

    // Debug: показывает все проверяемые блоки при поиске яблок
    private final Setting<Boolean> debugShowChecked = this.sgRender.booleanSetting("Debug Show Checked", false,
            "Renders ALL block positions checked for Enchanted Golden Apples (green=has apple, red=no apple).");

    private final List<FoundStructure> found = new CopyOnWriteArrayList<>();

    public List<FoundStructure> getFound() {
        return this.found;
    }

    private CompletableFuture<Void> calcFuture = null;

    private double lastCalcX = Double.MAX_VALUE;
    private double lastCalcZ = Double.MAX_VALUE;
    private String lastSeed = "";
    private boolean mapKeyWasDown = false;
    private SeedSourceUtils biomeSource;
    private long currentSeed;
    private ResourceKey<Level> lastDimension;

    public SeedSearcher() {
        super("Seed Searcher", "Locates structures in a Minecraft world based on seed analysis.", SubCategory.WORLD, true);
    }

    @Override
    public void onEnable() {
        this.biomeSource = null;
        this.recalculate();
    }

    @Override
    public void onDisable() {
        this.found.clear();
        this.biomeSource = null;
    }

    @Event
    public void onJoin(GameJoinEvent event) {
        this.lastCalcX = Double.MAX_VALUE;
        this.lastCalcZ = Double.MAX_VALUE;
        this.biomeSource = null;
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.level == null) return;

        var clickGui = Managers.CLICK_GUI.CLICK_GUI;
        boolean isClickGuiOpen = BlackOut.mc.screen == clickGui;
        boolean hasBlockingSubScreen = isClickGuiOpen && clickGui.openedScreen != null && !(clickGui.openedScreen instanceof SeedMapScreen);
        boolean isTyping = SelectedComponent.isSelected();
        boolean canInteract = BlackOut.mc.screen == null || (isClickGuiOpen && !hasBlockingSubScreen && !isTyping);

        if (!canInteract) {
            this.mapKeyWasDown = true;
            handleRecalculation();
            return;
        }

        handleRecalculation();

        KeyBind key = this.mapKey.get();
        boolean keyDown = key != null && key.isPressed();

        if (keyDown && !this.mapKeyWasDown) {
            this.openSeedMap();
        }
        this.mapKeyWasDown = keyDown;
    }

    private void handleRecalculation() {
        ResourceKey<Level> currentDim = BlackOut.mc.level.dimension();
        boolean dimChanged = currentDim != this.lastDimension;
        boolean seedChanged = !this.seed.get().equals(this.lastSeed);

        double dx = BlackOut.mc.player.getX() - this.lastCalcX;
        double dz = BlackOut.mc.player.getZ() - this.lastCalcZ;
        boolean moved = dx * dx + dz * dz > (double) this.recalcDistance.get() * this.recalcDistance.get();

        if (seedChanged || dimChanged || moved) {
            if (dimChanged) {
                this.biomeSource = null;
                this.lastDimension = currentDim;
            }
            this.recalculate();
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player == null || this.found.isEmpty()) return;

        double renderDistSq = (double) this.renderDistance.get() * this.renderDistance.get();
        Vec3 playerPos = BlackOut.mc.player.position();
        double halfW = this.beamWidth.get() / 2.0;
        int height = this.beamHeight.get();

        try (var ignored = Render3DUtils.begin()) {
            for (FoundStructure s : this.found) {
                double x = s.blockX() + 0.5;
                double z = s.blockZ() + 0.5;

                double dx = playerPos.x - x;
                double dz = playerPos.z - z;
                double distSq = dx * dx + dz * dz;
                if (distSq > renderDistSq) continue;

                AABB beam = new AABB(x - halfW, -64, z - halfW, x + halfW, height, z + halfW);
                Render3DUtils.box(beam, this.beamColor.get(), this.beamColor.get(), RenderShape.Full);

                if (s.extraInfo().contains("(Apple)") && !s.appleChestPositions().isEmpty()) {
                    BlackOutColor chestColor = new BlackOutColor(255, 215, 0, 200);
                    BlackOutColor chestGlow = new BlackOutColor(255, 255, 100, 80);
                    double chestSize = 0.3;
                    int[] pos = s.appleChestPositions().get(0);
                    AABB chestBox = new AABB(
                            pos[0] + 0.5 - chestSize, pos[1] + 0.5 - chestSize, pos[2] + 0.5 - chestSize,
                            pos[0] + 0.5 + chestSize, pos[1] + 0.5 + chestSize, pos[2] + 0.5 + chestSize
                    );
                    Render3DUtils.box(chestBox, chestColor, chestGlow, RenderShape.Full);
                }

                // Debug overlay: показывает ВСЕ проверяемые блоки
                if (this.debugShowChecked.get() && LootTableSimulationUtils.canHaveEnchantedApple(s.type())) {
                    java.util.List<int[]> debugPositions = LootTableSimulationUtils.debugGetAllCheckedPositions(
                            this.currentSeed, this.biomeSource, s.type(), s.blockX(), s.blockZ());
                    for (int[] dp : debugPositions) {
                        double dbgX = dp[0] + 0.5;
                        double dbgY = dp[1] + 0.5;
                        double dbgZ = dp[2] + 0.5;
                        boolean hasApple = dp[3] == 1;
                        double dbgDistSq = (playerPos.x - dbgX) * (playerPos.x - dbgX) + (playerPos.z - dbgZ) * (playerPos.z - dbgZ);
                        if (dbgDistSq > renderDistSq) continue;

                        double dbgSize = 0.4;
                        BlackOutColor dbgColor = hasApple
                                ? new BlackOutColor(50, 255, 50, 180)
                                : new BlackOutColor(255, 50, 50, 120);
                        BlackOutColor dbgGlow = hasApple
                                ? new BlackOutColor(100, 255, 100, 60)
                                : new BlackOutColor(255, 100, 100, 40);
                        AABB dbgBox = new AABB(
                                dbgX - dbgSize, dbgY - dbgSize, dbgZ - dbgSize,
                                dbgX + dbgSize, dbgY + dbgSize, dbgZ + dbgSize
                        );
                        Render3DUtils.box(dbgBox, dbgColor, dbgGlow, RenderShape.Full);
                    }
                }

                double dist = Math.sqrt(distSq);
                Render3DUtils.text(s.type().displayName + s.extraInfo() + " (" + (int) dist + "m)",
                        new Vec3(x, playerPos.y + 10, z), -1, this.textScale.get().floatValue());
            }
        }
    }

    @Override
    public String getInfo() {
        return String.valueOf(this.found.size());
    }

    private void recalculate() {
        if (BlackOut.mc.player == null || BlackOut.mc.level == null) return;

        if (this.calcFuture != null && !this.calcFuture.isDone()) return;

        long worldSeed = this.parseSeed();
        this.lastSeed = this.seed.get();
        this.lastCalcX = BlackOut.mc.player.getX();
        this.lastCalcZ = BlackOut.mc.player.getZ();

        SeedSourceUtils source = this.getOrCreateBiomeSource(worldSeed);
        ResourceKey<Level> dim = BlackOut.mc.level.dimension();

        int startX = (int) this.lastCalcX;
        int startZ = (int) this.lastCalcZ;
        int radius = this.searchRadius.get();

        boolean appleOnly = this.enchantedApples.get();
        this.calcFuture = CompletableFuture.runAsync(() -> {
            List<FoundStructure> newFound = new ArrayList<>();

            findInArea(newFound, worldSeed, source, dim, startX, startZ, radius, true,
                    t -> !appleOnly && this.isDimensionEnabled(t, dim), appleOnly,
                    t -> this.isAppleStructureEnabled(t));

            newFound.sort(Comparator.comparingDouble(s -> {
                double ddx = s.blockX() - startX;
                double ddz = s.blockZ() - startZ;
                return ddx * ddx + ddz * ddz;
            }));

            this.found.clear();
            this.found.addAll(newFound);
        });
    }

    public static void findInArea(List<FoundStructure> results, long worldSeed, SeedSourceUtils source, ResourceKey<Level> dim,
                                  int centerX, int centerZ, int radius, boolean limitRadius,
                                  Predicate<StructureType> enabledCheck, boolean searchApples,
                                  Predicate<StructureType> appleTypeCheck) {

        int playerChunkX = centerX >> 4;
        int playerChunkZ = centerZ >> 4;
        int radiusChunks = radius >> 4;
        double radiusSq = (double) radius * radius;

        if (dim == Level.NETHER) {
            findNetherStructures(results, worldSeed, source, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, enabledCheck, centerX, centerZ, searchApples);
        } else if (dim == Level.END) {
            if (enabledCheck.test(StructureType.END_CITY)) {
                findEndCities(results, worldSeed, source, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ);
            }
            if (enabledCheck.test(StructureType.END_GATEWAY)) {
                findEndGateways(results, centerX, centerZ, radiusSq, limitRadius);
            }
        } else {
            if (enabledCheck.test(StructureType.SPAWN)) findSpawnPos(results, source);
            for (StructureType type : StructureType.values()) {
                if (type.isCaveBiome()) continue;

                boolean enabled = enabledCheck.test(type);
                boolean appleScan = searchApples && LootTableSimulationUtils.canHaveEnchantedApple(type)
                        && appleTypeCheck.test(type);
                if (!enabled && !appleScan) continue;

                int before = results.size();
                if (type.isFeature()) {
                    if (type == StructureType.DUNGEON) {
                        findFeatures(results, worldSeed, source, type, centerX, centerZ, radius, radiusSq, limitRadius, searchApples);
                    } else {
                        findFeatures(results, worldSeed, source, type, centerX, centerZ, radius, radiusSq, limitRadius);
                    }
                } else if (type == StructureType.STRONGHOLD) {
                    findStrongholds(results, worldSeed, source, centerX, centerZ, radiusSq, limitRadius, searchApples);
                } else if (type.spacing > 0) {
                    findRandomSpread(results, worldSeed, source, type, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ, searchApples);
                } else {
                    continue;
                }

                if (appleScan && !enabled) {
                    results.removeIf(s -> s.type() == type && !s.extraInfo().contains("(Apple)"));
                }
            }
            findCaveBiomes(results, source, centerX, centerZ, radius, radiusSq, limitRadius, enabledCheck);
        }
    }

    private static void findEndCities(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                      int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq, boolean limitRadius,
                                      int centerX, int centerZ) {
        int spacing = StructureType.END_CITY.spacing;
        int separation = StructureType.END_CITY.separation;
        int range = spacing - separation;

        int minRegX = Math.floorDiv(playerChunkX - radiusChunks, spacing);
        int maxRegX = Math.floorDiv(playerChunkX + radiusChunks, spacing);
        int minRegZ = Math.floorDiv(playerChunkZ - radiusChunks, spacing);
        int maxRegZ = Math.floorDiv(playerChunkZ + radiusChunks, spacing);

        Random genRng = new Random(worldSeed);
        long la = genRng.nextLong();
        long lb = genRng.nextLong();

        for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {

                long regionSeed = rx * REGION_X_MULT + rz * REGION_Z_MULT + worldSeed + StructureType.END_CITY.salt;
                Random random = new Random(regionSeed);
                int offsetX = (random.nextInt(range) + random.nextInt(range)) / 2;
                int offsetZ = (random.nextInt(range) + random.nextInt(range)) / 2;

                int chunkX = rx * spacing + offsetX;
                int chunkZ = rz * spacing + offsetZ;

                int locateX = chunkX << 4;
                int locateZ = chunkZ << 4;

                if (limitRadius) {
                    double dx = (double) locateX - centerX;
                    double dz = (double) locateZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                ResourceKey<Biome> biome = source.getBiome(locateX + 8, locateZ + 8);
                if (biome != Biomes.END_HIGHLANDS && biome != Biomes.END_MIDLANDS) continue;

                long rotSeed = (long) chunkX * la ^ (long) chunkZ * lb ^ worldSeed;
                int rot = new Random(rotSeed).nextInt(4);
                int di = (rot == 1 || rot == 2) ? -5 : 5;
                int dj = (rot == 2 || rot == 3) ? -5 : 5;
                int k = locateX + 7;
                int l = locateZ + 7;

                if (!source.hasTerrainAtOrAbove60(k, l)
                        || !source.hasTerrainAtOrAbove60(k, l + dj)
                        || !source.hasTerrainAtOrAbove60(k + di, l)
                        || !source.hasTerrainAtOrAbove60(k + di, l + dj)) {
                    continue;
                }

                boolean hasShip = EndCitySimulationUtils.hasShip(worldSeed, chunkX, chunkZ);

                results.add(new FoundStructure(StructureType.END_CITY, locateX, locateZ, hasShip ? " Ship" : ""));
            }
        }
    }

    private static final int GATEWAY_COUNT = 20;
    private static final double GATEWAY_RADIUS = 96.0;

    private static void findEndGateways(List<FoundStructure> results,
                                        int centerX, int centerZ, double radiusSq, boolean limitRadius) {
        for (int i = 0; i < GATEWAY_COUNT; i++) {
            double angle = i * Math.PI / 10.0;
            int bx = (int) Math.floor(GATEWAY_RADIUS * Math.cos(angle));
            int bz = (int) Math.floor(GATEWAY_RADIUS * Math.sin(angle));

            if (limitRadius) {
                double dx = (double) bx - centerX;
                double dz = (double) bz - centerZ;
                if (dx * dx + dz * dz > radiusSq) continue;
            }

            results.add(new FoundStructure(StructureType.END_GATEWAY, bx, bz, ""));
        }
    }

    private static final Set<ResourceKey<Biome>> OCEAN_RIVER_BIOMES = Set.of(
            Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
            Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN,
            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN,
            Biomes.RIVER, Biomes.FROZEN_RIVER,
            Biomes.SWAMP, Biomes.MANGROVE_SWAMP,
            Biomes.STONY_SHORE, Biomes.BEACH, Biomes.SNOWY_BEACH
    );

    private static final Set<StructureType> SURFACE_STRUCTURES = Set.of(
            StructureType.DESERT_PYRAMID, StructureType.JUNGLE_TEMPLE,
            StructureType.SWAMP_HUT, StructureType.IGLOO
    );

    private static boolean hasValidTerrain(SeedSourceUtils source, StructureType type, int blockX, int blockZ) {
        if (!SURFACE_STRUCTURES.contains(type)) return true;

        int[][] samplePoints;
        switch (type) {
            case DESERT_PYRAMID, JUNGLE_TEMPLE -> {
                samplePoints = new int[][]{{0, 0}, {10, 10}, {10, -10}, {-10, 10}, {-10, -10}};
            }
            case SWAMP_HUT -> {
                samplePoints = new int[][]{{0, 0}, {3, 3}, {3, -3}, {-3, 3}, {-3, -3}};
            }
            case IGLOO -> {
                samplePoints = new int[][]{{0, 0}, {3, 3}, {3, -3}, {-3, 3}, {-3, -3}};
            }
            default -> { return true; }
        }

        for (int[] pt : samplePoints) {
            ResourceKey<Biome> biome = source.getBiomeAt(blockX + 8 + pt[0], 64, blockZ + 8 + pt[1]);
            if (OCEAN_RIVER_BIOMES.contains(biome)) return false;
        }

        return source.hasAreaAboveSeaLevel(blockX + 8, blockZ + 8,
                type == StructureType.DESERT_PYRAMID || type == StructureType.JUNGLE_TEMPLE ? 10 : 3);
    }

    private static void findRandomSpread(List<FoundStructure> results, long worldSeed, SeedSourceUtils source, StructureType type,
                                         int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq, boolean limitRadius,
                                         int centerX, int centerZ) {
        findRandomSpread(results, worldSeed, source, type, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ, false);
    }

    private static void findRandomSpread(List<FoundStructure> results, long worldSeed, SeedSourceUtils source, StructureType type,
                                         int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq, boolean limitRadius,
                                         int centerX, int centerZ, boolean searchApples) {
        boolean appleCapable = searchApples && LootTableSimulationUtils.canHaveEnchantedApple(type);

        int spacing = type.spacing;
        int separation = type.separation;
        int range = spacing - separation;

        int minRegX = Math.floorDiv(playerChunkX - radiusChunks, spacing);
        int maxRegX = Math.floorDiv(playerChunkX + radiusChunks, spacing);
        int minRegZ = Math.floorDiv(playerChunkZ - radiusChunks, spacing);
        int maxRegZ = Math.floorDiv(playerChunkZ + radiusChunks, spacing);

        for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {

                long regionSeed = rx * REGION_X_MULT + rz * REGION_Z_MULT + worldSeed + type.salt;
                Random regionRandom = new Random(regionSeed);

                int offsetX, offsetZ;

                if (type.triangular) {
                    offsetX = (regionRandom.nextInt(range) + regionRandom.nextInt(range)) / 2;
                    offsetZ = (regionRandom.nextInt(range) + regionRandom.nextInt(range)) / 2;
                } else {
                    offsetX = regionRandom.nextInt(range);
                    offsetZ = regionRandom.nextInt(range);
                }

                int chunkX = rx * spacing + offsetX;
                int chunkZ = rz * spacing + offsetZ;

                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                if (type.frequency < 1.0F) {
                    Random freqRandom = new Random(worldSeed);
                    long a = freqRandom.nextLong();
                    long b = freqRandom.nextLong();
                    freqRandom.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
                    if (freqRandom.nextDouble() >= type.frequency) continue;
                }

                ResourceKey<Biome> biome = null;
                if (type.validBiomes != null) {
                    biome = source.getBiomeAt(blockX + 8, 64, blockZ + 8);
                    if (!type.validBiomes.contains(biome)) continue;
                }

                if (!hasValidTerrain(source, type, blockX, blockZ)) continue;

                String extra = "";
                if (type == StructureType.IGLOO) {
                    extra = getIglooExtra(worldSeed, chunkX, chunkZ);
                } else if (type == StructureType.OCEAN_RUIN) {
                    boolean isWarm = biome == Biomes.WARM_OCEAN
                            || biome == Biomes.LUKEWARM_OCEAN
                            || biome == Biomes.DEEP_LUKEWARM_OCEAN;
                    extra = getOceanRuinExtra(worldSeed, chunkX, chunkZ, isWarm);
                } else if (type == StructureType.VILLAGE && biome != null) {
                    extra = isZombieVillage(worldSeed, chunkX, chunkZ, biome)
                            ? "zombie_" + biome.location().getPath()
                            : biome.location().getPath();
                }

                List<int[]> applePositions = Collections.emptyList();
                if (appleCapable && LootTableSimulationUtils.hasEnchantedApple(worldSeed, source, type, blockX, blockZ)) {
                    applePositions = LootTableSimulationUtils.getAppleChestPositions(worldSeed, source, type, blockX, blockZ);
                    extra = extra.isEmpty() ? "(Apple)" : extra + " (Apple)";
                }

                results.add(new FoundStructure(type, blockX, blockZ, extra, applePositions));
            }
        }
    }

    private static void findNetherStructures(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                             int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq,
                                             boolean limitRadius, Predicate<StructureType> enabledCheck,
                                             int centerX, int centerZ, boolean searchApples) {
        boolean showFortress = enabledCheck.test(StructureType.NETHER_FORTRESS);
        boolean showBastion = enabledCheck.test(StructureType.BASTION_REMNANT) || (searchApples && LootTableSimulationUtils.canHaveEnchantedApple(StructureType.BASTION_REMNANT));
        boolean showFossil = enabledCheck.test(StructureType.NETHER_FOSSIL);

        int range = NETHER_SPACING - NETHER_SEPARATION;
        int minRegX = Math.floorDiv(playerChunkX - radiusChunks, NETHER_SPACING);
        int maxRegX = Math.floorDiv(playerChunkX + radiusChunks, NETHER_SPACING);
        int minRegZ = Math.floorDiv(playerChunkZ - radiusChunks, NETHER_SPACING);
        int maxRegZ = Math.floorDiv(playerChunkZ + radiusChunks, NETHER_SPACING);

        for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {
                long regionSeed = rx * REGION_X_MULT + rz * REGION_Z_MULT + worldSeed + NETHER_SALT;
                Random random = new Random(regionSeed);

                int offsetX = random.nextInt(range);
                int offsetZ = random.nextInt(range);

                int chunkX = rx * NETHER_SPACING + offsetX;
                int chunkZ = rz * NETHER_SPACING + offsetZ;

                StructureType type = getNetherStructureType(worldSeed, chunkX, chunkZ);

                String extra = "";
                if (type == StructureType.BASTION_REMNANT) {
                    extra = getBastionType(worldSeed, chunkX, chunkZ);
                }

                boolean isBastion = type == StructureType.BASTION_REMNANT;
                boolean isAppleBastion = isBastion && searchApples && LootTableSimulationUtils.canHaveEnchantedApple(StructureType.BASTION_REMNANT);
                if (isBastion && !showBastion && !isAppleBastion) continue;
                if (!isBastion && !showFortress) continue;

                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;

                if (limitRadius) {
                    double dx = blockX - centerX;
                    double dz = blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                if (type.validBiomes != null && !type.validBiomes.contains(source.getBiome(blockX + 8, blockZ + 8)))
                    continue;

                if (isAppleBastion && !LootTableSimulationUtils.hasEnchantedApple(worldSeed, source, StructureType.BASTION_REMNANT, blockX, blockZ)) {
                    continue;
                }
                String bastionExtra = isAppleBastion && !extra.isEmpty() ? extra + " (Apple)" : isAppleBastion ? "(Apple)" : extra;

                results.add(new FoundStructure(type, blockX, blockZ, bastionExtra));
            }
        }

        if (showFossil) {
            findRandomSpread(results, worldSeed, source, StructureType.NETHER_FOSSIL,
                    playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ);
        }
    }

    public static String getBastionType(long worldSeed, int chunkX, int chunkZ) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        return switch (rand.nextInt(4)) {
            case 0 -> "housing";
            case 1 -> "hoglin";
            case 2 -> "treasure";
            default -> "bridges";
        };
    }

    private static String getOceanRuinExtra(long worldSeed, int chunkX, int chunkZ, boolean isWarm) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        boolean large = rand.nextFloat() < 0.3F;
        return large ? "Big" : "Small";
    }

    public static StructureType getNetherStructureType(long worldSeed, int chunkX, int chunkZ) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);

        int roll = rand.nextInt(5);
        return roll < 2 ? StructureType.NETHER_FORTRESS : StructureType.BASTION_REMNANT;
    }

    private static String getIglooExtra(long worldSeed, int chunkX, int chunkZ) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        rand.nextInt(4);
        return rand.nextDouble() < 0.5 ? "Laboratory" : "";
    }

    private static boolean isZombieVillage(long worldSeed, int chunkX, int chunkZ, ResourceKey<Biome> biome) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        rand.nextInt(4);

        int totalWeight = getVillageTotalWeight(biome);
        int normalWeight = getVillageNormalWeight(biome);
        return rand.nextInt(totalWeight) >= normalWeight;
    }

    private static int getVillageTotalWeight(ResourceKey<Biome> biome) {
        if (biome == Biomes.DESERT) return 250;
        if (biome == Biomes.SAVANNA) return 459;
        if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.SNOWY_TAIGA) return 306;
        if (biome == Biomes.TAIGA) return 100;
        return 204;
    }

    private static int getVillageNormalWeight(ResourceKey<Biome> biome) {
        if (biome == Biomes.DESERT) return 245;
        if (biome == Biomes.SAVANNA) return 450;
        if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.SNOWY_TAIGA) return 300;
        if (biome == Biomes.TAIGA) return 98;
        return 200;
    }

    private static void findSpawnPos(List<FoundStructure> results, SeedSourceUtils source) {
        int foundX = 0;
        int foundZ = 0;
        outer:
        for (int r = 0; r <= 512; r++) {
            for (int dz = -r; dz <= r; dz++) {
                boolean edgeZ = Math.abs(dz) == r;
                for (int dx = -r; dx <= r; dx++) {
                    if (!edgeZ && Math.abs(dx) != r) continue;
                    int blockX = dx * 4;
                    int blockZ = dz * 4;
                    ResourceKey<Biome> biome = source.getBiome(blockX, blockZ);
                    if (isValidSpawnBiome(biome)) {
                        foundX = blockX;
                        foundZ = blockZ;
                        break outer;
                    }
                }
            }
        }
        results.add(new FoundStructure(StructureType.SPAWN, foundX, foundZ, ""));
    }

    private static boolean isValidSpawnBiome(ResourceKey<Biome> biome) {
        return biome != Biomes.OCEAN && biome != Biomes.DEEP_OCEAN
                && biome != Biomes.COLD_OCEAN && biome != Biomes.DEEP_COLD_OCEAN
                && biome != Biomes.WARM_OCEAN && biome != Biomes.LUKEWARM_OCEAN
                && biome != Biomes.DEEP_LUKEWARM_OCEAN
                && biome != Biomes.FROZEN_OCEAN && biome != Biomes.DEEP_FROZEN_OCEAN
                && biome != Biomes.RIVER && biome != Biomes.FROZEN_RIVER
                && biome != Biomes.THE_VOID
                && biome != Biomes.NETHER_WASTES && biome != Biomes.SOUL_SAND_VALLEY
                && biome != Biomes.CRIMSON_FOREST && biome != Biomes.WARPED_FOREST
                && biome != Biomes.BASALT_DELTAS
                && biome != Biomes.THE_END && biome != Biomes.END_HIGHLANDS
                && biome != Biomes.END_MIDLANDS && biome != Biomes.END_BARRENS
                && biome != Biomes.SMALL_END_ISLANDS;
    }

    private static void findCaveBiomes(List<FoundStructure> results, SeedSourceUtils source, int centerX, int centerZ,
                                       int radius, double radiusSq, boolean limitRadius,
                                       Predicate<StructureType> enabledCheck) {
        int scanStep = 128;
        int sectionSize = 512;

        StructureType[] caveTypes = {StructureType.LUSH_CAVES, StructureType.DRIPSTONE_CAVES};

        for (StructureType type : caveTypes) {
            if (!enabledCheck.test(type)) continue;
            ResourceKey<Biome> targetBiome = type.validBiomes.iterator().next();
            int scanY = 16;

            int minSecX = Math.floorDiv(centerX - radius, sectionSize);
            int maxSecX = Math.floorDiv(centerX + radius, sectionSize);
            int minSecZ = Math.floorDiv(centerZ - radius, sectionSize);
            int maxSecZ = Math.floorDiv(centerZ + radius, sectionSize);

            List<int[]> rawCandidates = new ArrayList<>();

            for (int sx = minSecX; sx <= maxSecX; sx++) {
                for (int sz = minSecZ; sz <= maxSecZ; sz++) {

                    long sumX = 0;
                    long sumZ = 0;
                    int count = 0;

                    int sectionStartX = sx * sectionSize;
                    int sectionStartZ = sz * sectionSize;

                    for (int bx = sectionStartX; bx < sectionStartX + sectionSize; bx += scanStep) {
                        for (int bz = sectionStartZ; bz < sectionStartZ + sectionSize; bz += scanStep) {
                            if (source.getBiomeAt(bx, scanY, bz).equals(targetBiome)) {
                                sumX += bx;
                                sumZ += bz;
                                count++;
                            }
                        }
                    }

                    if (count >= 2) {
                        int finalX = (int) (sumX / count);
                        int finalZ = (int) (sumZ / count);

                        if (limitRadius) {
                            double dx = finalX - centerX;
                            double dz = finalZ - centerZ;
                            if (dx * dx + dz * dz > radiusSq) continue;
                        }

                        boolean merged = false;
                        for (int[] existing : rawCandidates) {
                            if (Math.abs(existing[0] - finalX) < 400 && Math.abs(existing[1] - finalZ) < 400) {
                                existing[0] = (existing[0] + finalX) / 2;
                                existing[1] = (existing[1] + finalZ) / 2;
                                merged = true;
                                break;
                            }
                        }
                        if (!merged) {
                            rawCandidates.add(new int[]{finalX, finalZ});
                        }
                    }
                }
            }

            for (int[] pos : rawCandidates) {
                results.add(new FoundStructure(type, pos[0], pos[1], ""));
            }
        }
    }

    private static void findFeatures(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                     StructureType type, int centerX, int centerZ, int radius,
                                     double radiusSq, boolean limitRadius) {
        findFeatures(results, worldSeed, source, type, centerX, centerZ, radius, radiusSq, limitRadius, false);
    }

    private static void findFeatures(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                     StructureType type, int centerX, int centerZ, int radius,
                                     double radiusSq, boolean limitRadius, boolean searchApples) {
        boolean appleCapable = searchApples && LootTableSimulationUtils.canHaveEnchantedApple(type);

        Random seedRng = new Random(worldSeed);
        long a = seedRng.nextLong();
        long b = seedRng.nextLong();

        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2000000) return;

        boolean isDense = (type == StructureType.RAVINE);
        int step = isDense ? 4 : 1;

        for (int cx = minChunkX; cx <= maxChunkX; cx += step) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz += step) {
                Random chunkRand = new Random(worldSeed);
                long ca = chunkRand.nextLong();
                long cb = chunkRand.nextLong();
                chunkRand.setSeed((long) cx * ca ^ (long) cz * cb ^ worldSeed);

                int blockX = 0, blockZ = 0;
                boolean found = false;

                switch (type) {
                    case DUNGEON -> {
                        if (chunkRand.nextInt(100) != 0) continue;
                        blockX = (cx << 4) + chunkRand.nextInt(16);
                        blockZ = (cz << 4) + chunkRand.nextInt(16);

                        if (source != null) {
                            var biome = source.getBiome(blockX + 8, blockZ + 8);
                            boolean valid = false;
                            for (var caveBiome : OVERWORLD_CAVE_BIOMES) {
                                if (caveBiome == biome) { valid = true; break; }
                            }
                            if (!valid) continue;
                        }
                        found = true;
                    }

                    case GEODE -> {
                        double noiseX = cx / 200.0;
                        double noiseZ = cz / 200.0;
                        double noise = getSimplexNoiseApprox(noiseX, noiseZ);
                        if (noise >= -0.7) {
                            blockX = (cx << 4) + chunkRand.nextInt(16);
                            blockZ = (cz << 4) + chunkRand.nextInt(16);

                            if (source != null) {
                                var biome = source.getBiome(blockX + 8, blockZ + 8);
                                if (biome != Biomes.LUSH_CAVES) continue;
                            }
                            found = true;
                        }
                        if (!found) continue;
                    }

                    case LAVA_POOL_SURFACE -> {
                        if (chunkRand.nextInt(33) != 0) continue;
                        blockX = (cx << 4) + chunkRand.nextInt(16);
                        blockZ = (cz << 4) + chunkRand.nextInt(16);
                        found = true;
                    }

                    case LAVA_POOL_CAVE -> {
                        if (chunkRand.nextInt(9) != 0) continue;
                        blockX = (cx << 4) + chunkRand.nextInt(16);
                        blockZ = (cz << 4) + chunkRand.nextInt(16);
                        found = true;
                    }

                    case RAVINE -> {
                        double noiseX = cx * 0.2;
                        double noiseZ = cz * 0.2;
                        double noise = getSimplexNoiseApprox(noiseX, noiseZ);
                        if (noise > 0.0) {
                            blockX = (cx << 4) + 8;
                            blockZ = (cz << 4) + 8;
                            found = true;
                        }
                        if (!found) continue;
                    }

                    default -> { continue; }
                }

                if (!found) continue;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                String featureExtra = "";
                if (appleCapable && LootTableSimulationUtils.hasEnchantedApple(worldSeed, source, type, blockX, blockZ)) {
                    featureExtra = "(Apple)";
                }
                results.add(new FoundStructure(type, blockX, blockZ, featureExtra));
            }
        }
    }

    private static double getSimplexNoiseApprox(double x, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;

        double sx = fx * fx * (3.0 - 2.0 * fx);
        double sz = fz * fz * (3.0 - 2.0 * fz);

        double n00 = hashNoise(ix, iz);
        double n10 = hashNoise(ix + 1, iz);
        double n01 = hashNoise(ix, iz + 1);
        double n11 = hashNoise(ix + 1, iz + 1);

        double nx0 = n00 + (n10 - n00) * sx;
        double nx1 = n01 + (n11 - n01) * sx;
        return nx0 + (nx1 - nx0) * sz;
    }

    private static double hashNoise(int x, int z) {
        long hash = (x * 341873128712L + z * 132897987541L) & 0x7FFFFFFFFFFFFFFFL;
        hash = hash * 0x9E3779B97F4A7C15L;
        hash ^= hash >> 33;
        hash *= 0xC6A4A7935BD1E995L;
        hash ^= hash >> 29;
        return (hash & 0x7FFFFFFFFFFFFFFFL) / (double) Long.MAX_VALUE * 2.0 - 1.0;
    }

    private static void findStrongholds(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                        int centerX, int centerZ, double radiusSq, boolean limitRadius) {
        findStrongholds(results, worldSeed, source, centerX, centerZ, radiusSq, limitRadius, false);
    }

    private static void findStrongholds(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                        int centerX, int centerZ, double radiusSq, boolean limitRadius, boolean searchApples) {
        boolean appleCapable = searchApples && LootTableSimulationUtils.canHaveEnchantedApple(StructureType.STRONGHOLD);

        int distance = 32;
        int count = 128;
        int spread = 3;

        RandomSource random = RandomSource.create();
        random.setSeed(worldSeed);

        double angle = random.nextDouble() * Math.PI * 2.0;

        int placedInRing = 0;
        int ring = 0;

        for (int i = 0; i < count; i++) {
            double dist = 4.0 * distance + 6.0 * distance * ring
                    + (random.nextDouble() - 0.5) * 2.5 * distance;
            int chunkX = (int) Math.round(Math.cos(angle) * dist);
            int chunkZ = (int) Math.round(Math.sin(angle) * dist);

            random.fork();

            if (source != null) {
                int[] snapped = source.findNearestStrongholdChunk(chunkX, chunkZ);
                chunkX = snapped[0];
                chunkZ = snapped[1];
            }

            int blockX = chunkX << 4;
            int blockZ = chunkZ << 4;

            String shExtra = "";
            if (appleCapable && LootTableSimulationUtils.hasEnchantedApple(worldSeed, source, StructureType.STRONGHOLD, blockX, blockZ)) {
                shExtra = "(Apple)";
            }

            if (limitRadius) {
                double dx = blockX - centerX;
                double dz = blockZ - centerZ;
                if (dx * dx + dz * dz <= radiusSq) {
                    results.add(new FoundStructure(StructureType.STRONGHOLD, blockX, blockZ, shExtra));
                }
            } else {
                results.add(new FoundStructure(StructureType.STRONGHOLD, blockX, blockZ, shExtra));
            }

            angle += Math.PI * 2.0 / spread;
            placedInRing++;

            if (placedInRing == spread) {
                ring++;
                placedInRing = 0;

                spread += 2 * spread / (ring + 1);
                spread = Math.min(spread, count - i - 1);
                if (spread <= 0) break;

                angle += random.nextDouble() * Math.PI * 2.0;
            }
        }
    }

    private SeedSourceUtils getOrCreateBiomeSource(long seed) {
        if (this.biomeSource == null || this.currentSeed != seed) {
            var dimension = BlackOut.mc.level != null ? BlackOut.mc.level.dimension() : Level.OVERWORLD;
            this.biomeSource = new SeedSourceUtils(seed, dimension);
            this.currentSeed = seed;
        }
        return this.biomeSource;
    }

    private void openSeedMap() {
        var clickGui = Managers.CLICK_GUI.CLICK_GUI;

        if (clickGui.openedScreen instanceof SeedMapScreen) {
            clickGui.setScreen(null);
            clickGui.setOpen(false);
            BlackOut.mc.setScreen(null);
            return;
        }

        long worldSeed = this.parseSeed();
        SeedMapScreen screen = new SeedMapScreen(this.seed.get(), worldSeed, this.getOrCreateBiomeSource(worldSeed));

        if (!clickGui.isOpen()) {
            clickGui.setOpen(true);
            clickGui.initGui();
            BlackOut.mc.setScreen(clickGui);
        }
        clickGui.setScreen(screen);
    }

    public boolean isDimensionEnabled(StructureType type, ResourceKey<Level> dim) {
        if (dim == null) return false;

        if (this.enchantedApples.get() && LootTableSimulationUtils.canHaveEnchantedApple(type)) return true;

        boolean isFeature = type.isFeature();
        boolean isNetherStruct = !isFeature && (type == StructureType.NETHER_FORTRESS
                || type == StructureType.BASTION_REMNANT
                || type == StructureType.NETHER_FOSSIL);
        boolean isEndStruct = !isFeature && (type == StructureType.END_CITY
                || type == StructureType.END_GATEWAY);

        if (dim == Level.NETHER) return isNetherStruct && isStructureEnabled(type);
        if (dim == Level.END) return isEndStruct && isStructureEnabled(type);
        if (isFeature) return dim == Level.OVERWORLD && isStructureEnabled(type);
        return !isNetherStruct && !isEndStruct && isStructureEnabled(type);
    }

    private long parseSeed() {
        String s = this.seed.get().trim();
        if (s.isEmpty()) return 0L;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return s.hashCode();
        }
    }

    public boolean isStructureEnabled(StructureType type) {
        return this.structures.get().contains(type);
    }

    public boolean isAppleStructureEnabled(StructureType type) {
        return this.appleStructures.get().contains(type);
    }

    public boolean isEnchantedApplesEnabled() {
        return this.enchantedApples.get();
    }
}
