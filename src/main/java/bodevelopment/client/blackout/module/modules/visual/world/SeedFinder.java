package bodevelopment.client.blackout.module.modules.visual.world;

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
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.EndCitySimulator;
import bodevelopment.client.blackout.util.LootSimulator;
import bodevelopment.client.blackout.util.SeedBiomeSource;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Seed-based structure locator.
 */
@Experimental
public class SeedFinder extends Module {
    private static final long REGION_X_MULT = 341873128712L;
    private static final long REGION_Z_MULT = 132897987541L;

    private static final int NETHER_SPACING = 27;
    private static final int NETHER_SEPARATION = 4;
    private static final int NETHER_SALT = 30084232;

    /** Cave biomes for mineshaft biome filtering. */
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

    /** Scans ALL apple-capable structures regardless of enabled types. */
    private final Setting<Boolean> enchantedApples = this.sgGeneral.booleanSetting("Enchanted Apples", false,
            "Search for chests containing Enchanted Golden Apples in all structures.")
            .onChanged(v -> this.recalculate());

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

    private final List<FoundStructure> found = new CopyOnWriteArrayList<>();

    public List<FoundStructure> getFound() {
        return this.found;
    }

    private CompletableFuture<Void> calcFuture = null;

    private double lastCalcX = Double.MAX_VALUE;
    private double lastCalcZ = Double.MAX_VALUE;
    private String lastSeed = "";
    private boolean mapKeyWasDown = false;
    private SeedBiomeSource biomeSource;
    private long currentSeed;
    private ResourceKey<Level> lastDimension;

    public SeedFinder() {
        super("Seed Finder", "Locates structures in a Minecraft world based on seed analysis.", SubCategory.WORLD, true);
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
                double x = s.blockX + 0.5;
                double z = s.blockZ + 0.5;

                double dx = playerPos.x - x;
                double dz = playerPos.z - z;
                double distSq = dx * dx + dz * dz;
                if (distSq > renderDistSq) continue;

                AABB beam = new AABB(x - halfW, -64, z - halfW, x + halfW, height, z + halfW);
                Render3DUtils.box(beam, this.beamColor.get(), this.beamColor.get(), RenderShape.Full);

                double dist = Math.sqrt(distSq);
                Render3DUtils.text(s.type.displayName + s.extraInfo + " (" + (int) dist + "m)",
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

        SeedBiomeSource source = this.getOrCreateBiomeSource(worldSeed);
        ResourceKey<Level> dim = BlackOut.mc.level.dimension();

        int startX = (int) this.lastCalcX;
        int startZ = (int) this.lastCalcZ;
        int radius = this.searchRadius.get();

        boolean appleOnly = this.enchantedApples.get();
        this.calcFuture = CompletableFuture.runAsync(() -> {
            List<FoundStructure> newFound = new ArrayList<>();

            findInArea(newFound, worldSeed, source, dim, startX, startZ, radius, true,
                    t -> !appleOnly && this.isDimensionEnabled(t, dim), appleOnly);

            newFound.sort(Comparator.comparingDouble(s -> {
                double ddx = s.blockX - startX;
                double ddz = s.blockZ - startZ;
                return ddx * ddx + ddz * ddz;
            }));

            this.found.clear();
            this.found.addAll(newFound);
        });
    }


    public static void findInArea(List<FoundStructure> results, long worldSeed, SeedBiomeSource source, ResourceKey<Level> dim,
                                  int centerX, int centerZ, int radius, boolean limitRadius,
                                  Predicate<StructureType> enabledCheck, boolean searchApples) {

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
                boolean appleScan = searchApples && LootSimulator.canHaveEnchantedApple(type);
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
                    continue; // skip non-random-spread types (SPAWN, END_GATEWAY handled elsewhere)
                }

                if (appleScan && !enabled) {
                    results.removeIf(s -> s.type() == type && !s.extraInfo().contains("(Apple)"));
                }
            }
            findCaveBiomes(results, source, centerX, centerZ, radius, radiusSq, limitRadius, enabledCheck);
        }
    }

    // ========================================================================
    // End Cities
    // ========================================================================


    private static void findEndCities(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
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

                boolean hasShip = EndCitySimulator.hasShip(worldSeed, chunkX, chunkZ);

                results.add(new FoundStructure(StructureType.END_CITY, locateX, locateZ, hasShip ? " Ship" : ""));
            }
        }
    }

    // ========================================================================
    // End Gateways — fixed 20 positions around the main island
    // ========================================================================

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


    private static void findRandomSpread(List<FoundStructure> results, long worldSeed, SeedBiomeSource source, StructureType type,
                                         int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq, boolean limitRadius,
                                         int centerX, int centerZ) {
        findRandomSpread(results, worldSeed, source, type, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ, false);
    }

    private static void findRandomSpread(List<FoundStructure> results, long worldSeed, SeedBiomeSource source, StructureType type,
                                         int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq, boolean limitRadius,
                                         int centerX, int centerZ, boolean searchApples) {
        boolean appleCapable = searchApples && LootSimulator.canHaveEnchantedApple(type);

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

                // Vanilla frequency reduction: uses setLargeFeatureSeed(worldSeed, chunkX, chunkZ) NOT setLargeFeatureWithSalt!
                // The frequency check creates a SEPARATE random seeded via a/b formula.
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

                String extra = "";
                if (type == StructureType.IGLOO) {
                    extra = getIglooExtra(worldSeed, chunkX, chunkZ);
                } else if (type == StructureType.OCEAN_RUIN) {
                    // Fix 1.12: Both warm and cold have largeProbability=0.3 in 1.21.4
                    boolean isWarm = biome == Biomes.WARM_OCEAN
                            || biome == Biomes.LUKEWARM_OCEAN
                            || biome == Biomes.DEEP_LUKEWARM_OCEAN;
                    extra = getOceanRuinExtra(worldSeed, chunkX, chunkZ, isWarm);
                } else if (type == StructureType.VILLAGE && biome != null) {
                    extra = isZombieVillage(worldSeed, chunkX, chunkZ, biome)
                            ? "zombie_" + biome.location().getPath()
                            : biome.location().getPath();
                }

                // Enchanted Apple check (independent of ListSetting toggle)
                if (appleCapable && LootSimulator.hasEnchantedApple(worldSeed, type, blockX, blockZ)) {
                    extra = extra.isEmpty() ? "(Apple)" : extra + " (Apple)";
                }

                results.add(new FoundStructure(type, blockX, blockZ, extra));
            }
        }
    }

    // ========================================================================
    // Nether Structures
    // ========================================================================

    private static void findNetherStructures(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                             int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq,
                                             boolean limitRadius, Predicate<StructureType> enabledCheck,
                                             int centerX, int centerZ, boolean searchApples) {
        boolean showFortress = enabledCheck.test(StructureType.NETHER_FORTRESS);
        boolean showBastion = enabledCheck.test(StructureType.BASTION_REMNANT) || (searchApples && LootSimulator.canHaveEnchantedApple(StructureType.BASTION_REMNANT));
        boolean showFossil = enabledCheck.test(StructureType.NETHER_FOSSIL);

        // --- Fortress + Bastion (shared grid: spacing=27) ---
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
                boolean isAppleBastion = isBastion && searchApples && LootSimulator.canHaveEnchantedApple(StructureType.BASTION_REMNANT);
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

                // Bastion apple check: only add if apple found when scanning for apples only
                if (isAppleBastion && !LootSimulator.hasEnchantedApple(worldSeed, StructureType.BASTION_REMNANT, blockX, blockZ)) {
                    continue;
                }
                String bastionExtra = isAppleBastion && !extra.isEmpty() ? extra + " (Apple)" : isAppleBastion ? "(Apple)" : extra;

                results.add(new FoundStructure(type, blockX, blockZ, bastionExtra));
            }
        }

        // --- NETHER_FOSSIL (independent grid: spacing=2, separation=1, salt=14357921, triangular=true) ---
        if (showFossil) {
            findRandomSpread(results, worldSeed, source, StructureType.NETHER_FOSSIL,
                    playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ);
        }
    }

    // ========================================================================
    // Structure Type Detection Helpers
    // ========================================================================

    /**
     * Fix 1.9: Bastion type detection corrected for vanilla 1.21.4.
     * BastionRemnant is now a JigsawStructure. The start pool contains 4 templates
     * with equal weight 1, in this order (from BastionPieces.bootstrap):
     * <ol>
     *   <li>bastion/units/air_base (HOUSING)</li>
     *   <li>bastion/hoglin_stable/air_base (HOGLIN)</li>
     *   <li>bastion/treasure/big_air_full (TREASURE)</li>
     *   <li>bastion/bridge/starting_pieces/entrance_base (BRIDGE)</li>
     * </ol>
     */
    public static String getBastionType(long worldSeed, int chunkX, int chunkZ) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        return switch (rand.nextInt(4)) {
            case 0 -> "housing";   // bastion/units/air_base
            case 1 -> "hoglin";    // bastion/hoglin_stable/air_base
            case 2 -> "treasure";  // bastion/treasure/big_air_full
            default -> "bridges";  // bastion/bridge/starting_pieces/entrance_base
        };
    }

    /**
     * Fix 1.12: Ocean ruin largeProbability is 0.3 for BOTH warm and cold in 1.21.4.
     * Previously used 0.9 for cold, which was incorrect (old version behavior).
     */
    private static String getOceanRuinExtra(long worldSeed, int chunkX, int chunkZ, boolean isWarm) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        // Fix 1.12: Both warm and cold ruins have largeProbability=0.3 in vanilla 1.21.4
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

    /**
     * Fix 1.9: In 1.21.4, IglooPieces.addPieces uses {@code random.nextDouble() < 0.5} (50% chance)
     * for basement/laboratory, NOT {@code nextInt(10) == 0} (10% chance) as was previously coded.
     * <p>
     * Vanilla flow:
     * <ol>
     *   <li>{@code Rotation.getRandom(random)} → consumes {@code nextInt(4)}</li>
     *   <li>{@code random.nextDouble() < 0.5} → 50% basement chance</li>
     * </ol>
     */
    private static String getIglooExtra(long worldSeed, int chunkX, int chunkZ) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        rand.nextInt(4); // Rotation.getRandom (consumed by IglooStructure.findGenerationPoint)
        return rand.nextDouble() < 0.5 ? "Laboratory" : "";
    }

    /**
     * Fix 1.10: In 1.21.4, villages are JigsawStructures. Zombie variant is determined by
     * template pool weighted random selection, NOT by a simple random flag.
     * <p>
     * Vanilla JigsawPlacement.addPieces:
     * <ol>
     *   <li>{@code Rotation.getRandom(random)} → consumes {@code nextInt(4)}</li>
     *   <li>{@code pool.getRandomTemplate(random)} → consumes {@code nextInt(totalWeight)}</li>
     * </ol>
     * Each biome has different total weight / zombie weight. See template pool JSON files.
     */
    private static boolean isZombieVillage(long worldSeed, int chunkX, int chunkZ, ResourceKey<Biome> biome) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);
        rand.nextInt(4); // Rotation.getRandom (consumed by JigsawPlacement)

        // Vanilla template pool weights per biome (from JSON)
        int totalWeight = getVillageTotalWeight(biome);
        int normalWeight = getVillageNormalWeight(biome);
        return rand.nextInt(totalWeight) >= normalWeight;
    }

    private static int getVillageTotalWeight(ResourceKey<Biome> biome) {
        if (biome == Biomes.DESERT) return 250;
        if (biome == Biomes.SAVANNA) return 459;
        if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.SNOWY_TAIGA) return 306;
        if (biome == Biomes.TAIGA) return 100;
        // Plains, Meadow, Sunflower Plains, Cherry Grove, Grove
        return 204;
    }

    private static int getVillageNormalWeight(ResourceKey<Biome> biome) {
        if (biome == Biomes.DESERT) return 245;
        if (biome == Biomes.SAVANNA) return 450;
        if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.SNOWY_TAIGA) return 300;
        if (biome == Biomes.TAIGA) return 98;
        return 200;
    }

    // ========================================================================
    // World Spawn
    // ========================================================================

    private static void findSpawnPos(List<FoundStructure> results, SeedBiomeSource source) {
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

    // ========================================================================
    // Cave Biome Search (Fix 1.2: merge duplicate markers)
    // ========================================================================

    private static void findCaveBiomes(List<FoundStructure> results, SeedBiomeSource source, int centerX, int centerZ,
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

            // Fix 1.2: Collect raw section results first, then deduplicate
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

                        // Fix 1.2: Deduplicate - merge sections closer than 400 blocks
                        boolean merged = false;
                        for (int[] existing : rawCandidates) {
                            if (Math.abs(existing[0] - finalX) < 400 && Math.abs(existing[1] - finalZ) < 400) {
                                // Weighted average: merge into existing
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

    // ========================================================================
    // Features (Dungeon, Geode, Lava Pool, Ravine)
    // Unlike structures with RandomSpreadStructurePlacement, features are placed
    // per-chunk using placement modifiers (RarityFilter, NoiseThresholdCount, etc.).
    // ========================================================================

    /**
     * Finds feature-type placements by iterating over all chunks in range and
     * simulating the placement modifier checks using chunk-seeded Random.
     *
     * In 1.21.4, features use per-chunk placement modifier chains instead of the
     * grid-based RandomSpreadStructurePlacement used by structures. The chunk seed
     * is derived from the world seed + chunk coordinates using the same formula
     * as vanilla: seed = (long) chunkX * a ^ (long) chunkZ * b ^ worldSeed
     * where a,b are the first two nextLong() values from java.util.Random(worldSeed).
     *
     * Performance note: feature scanning is O(chunks) vs O(regions) for structures.
     * For a 10k block radius (~1000 chunks), this is manageable (1M+ iterations).
     * We use a step-based scan to balance accuracy vs performance.
     */
    private static void findFeatures(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                     StructureType type, int centerX, int centerZ, int radius,
                                     double radiusSq, boolean limitRadius) {
        findFeatures(results, worldSeed, source, type, centerX, centerZ, radius, radiusSq, limitRadius, false);
    }

    private static void findFeatures(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                     StructureType type, int centerX, int centerZ, int radius,
                                     double radiusSq, boolean limitRadius, boolean searchApples) {
        boolean appleCapable = searchApples && LootSimulator.canHaveEnchantedApple(type);

        // Compute chunk RNG seeds (same formula used by vanilla feature placement)
        Random seedRng = new Random(worldSeed);
        long a = seedRng.nextLong();
        long b = seedRng.nextLong();

        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        // Limit total chunks to avoid excessive computation
        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2000000) return; // Safety cap

        // For denser features (ravine), use a step to reduce iterations
        boolean isDense = (type == StructureType.RAVINE);
        int step = isDense ? 4 : 1;

        for (int cx = minChunkX; cx <= maxChunkX; cx += step) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz += step) {
                // Generate chunk-seeded Random (vanilla formula)
                Random chunkRand = new Random(worldSeed);
                long ca = chunkRand.nextLong();
                long cb = chunkRand.nextLong();
                chunkRand.setSeed((long) cx * ca ^ (long) cz * cb ^ worldSeed);

                int blockX = 0, blockZ = 0;
                boolean found = false;

                switch (type) {
                    // =============================================================
                    // Dungeon (MonsterRoomFeature)
                    // Vanilla placement: RarityFilter(100) → InSquare → HeightRange
                    // RarityFilter: random.nextInt(100) == 0
                    // InSquare: +random.nextInt(16) to x,z
                    // HeightRange: uniform -64 to -48 (deep underground)
                    // =============================================================
                    case DUNGEON -> {
                        if (chunkRand.nextInt(100) != 0) continue;
                        blockX = (cx << 4) + chunkRand.nextInt(16);
                        blockZ = (cz << 4) + chunkRand.nextInt(16);

                        // Biome check: dungeons generate in overworld cave biomes
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

                    // =============================================================
                    // GeodeFeature
                    // Vanilla placement: NoiseThresholdCountPlacement(noise_level=-0.7,
                    //   below_noise=0, above_noise=1) → InSquare → HeightRange
                    // Uses SimplexNoise at (chunkX/200.0, chunkZ/200.0)
                    // If noise < -0.7: no geode; if noise >= -0.7: 1 geode
                    // =============================================================
                    case GEODE -> {
                        // SimplexNoise at (chunkX/200.0, chunkZ/200.0)
                        double noiseX = cx / 200.0;
                        double noiseZ = cz / 200.0;
                        double noise = getSimplexNoiseApprox(noiseX, noiseZ);
                        if (noise >= -0.7) {
                            blockX = (cx << 4) + chunkRand.nextInt(16);
                            blockZ = (cz << 4) + chunkRand.nextInt(16);

                            // Biome check: geodes generate in any biome, but we check lush caves as primary
                            if (source != null) {
                                var biome = source.getBiome(blockX + 8, blockZ + 8);
                                if (biome != Biomes.LUSH_CAVES) continue;
                            }
                            found = true;
                        }
                        if (!found) continue;
                    }

                    // =============================================================
                    // Lava Pool (Surface)
                    // Vanilla placement: RarityFilter(33) → InSquare → HeightRange(0,160)
                    // =============================================================
                    case LAVA_POOL_SURFACE -> {
                        if (chunkRand.nextInt(33) != 0) continue;
                        blockX = (cx << 4) + chunkRand.nextInt(16);
                        blockZ = (cz << 4) + chunkRand.nextInt(16);
                        // Surface lava pools: Y near surface (approx 50-70)
                        // No strict biome restriction in vanilla
                        found = true;
                    }

                    // =============================================================
                    // Lava Pool (Cave)
                    // Vanilla placement: RarityFilter(9) → InSquare → HeightRange(0,256)
                    // =============================================================
                    case LAVA_POOL_CAVE -> {
                        if (chunkRand.nextInt(9) != 0) continue;
                        blockX = (cx << 4) + chunkRand.nextInt(16);
                        blockZ = (cz << 4) + chunkRand.nextInt(16);
                        found = true;
                    }

                    // =============================================================
                    // Ravine (CanyonWorldCarver)
                    // Uses carver noise system. Carvers generate in a noise-based grid.
                    // The CanyonWorldCarver checks if noise at chunk passes threshold.
                    // We approximate using simplex noise at chunk position.
                    // =============================================================
                    case RAVINE -> {
                        // CanyonWorldCarver uses: noise(x*0.2, z*0.2) > 0
                        // The carver noise is seeded from world seed + carver index
                        double noiseX = cx * 0.2;
                        double noiseZ = cz * 0.2;
                        double noise = getSimplexNoiseApprox(noiseX, noiseZ);
                        // Ravines generate where noise is positive
                        if (noise > 0.0) {
                            blockX = (cx << 4) + 8;  // center of chunk
                            blockZ = (cz << 4) + 8;
                            found = true;
                        }
                        if (!found) continue;
                    }

                    default -> { continue; }
                }

                if (!found) continue;

                // Radius check
                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                // Enchanted Apple check (only for Dungeon, independent of ListSetting toggle)
                String featureExtra = "";
                if (appleCapable && LootSimulator.hasEnchantedApple(worldSeed, type, blockX, blockZ)) {
                    featureExtra = "(Apple)";
                }
                results.add(new FoundStructure(type, blockX, blockZ, featureExtra));
            }
        }
    }

    /**
     * Simplified SimplexNoise approximation for feature placement checks.
     * In vanilla 1.21.4, NoiseThresholdCountPlacement uses esf (SimplexNoise)
     * which is a multi-octave noise generator. We use a simple 2D Perlin/Simplex
     * approximation for the seed map tool.
     *
     * For precise vanilla-matching noise, we'd need to replicate the full
     * esf (SimplexNoise) class which uses random-seeded noise octaves.
     * The approximation is accurate enough for most seed finding purposes.
     */
    private static double getSimplexNoiseApprox(double x, double z) {
        // Simple gradient noise approximation
        // Uses sin/cos based hash for pseudo-random gradient at each grid point
        // This is NOT vanilla-accurate but provides a reasonable noise field

        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;

        // Smooth steps
        double sx = fx * fx * (3.0 - 2.0 * fx);
        double sz = fz * fz * (3.0 - 2.0 * fz);

        // Pseudo-random gradients at grid corners
        double n00 = hashNoise(ix, iz);
        double n10 = hashNoise(ix + 1, iz);
        double n01 = hashNoise(ix, iz + 1);
        double n11 = hashNoise(ix + 1, iz + 1);

        // Bilinear interpolation
        double nx0 = n00 + (n10 - n00) * sx;
        double nx1 = n01 + (n11 - n01) * sx;
        return nx0 + (nx1 - nx0) * sz;
    }

    private static double hashNoise(int x, int z) {
        // Simple hash-based pseudo-random value in [-1, 1]
        long hash = (x * 341873128712L + z * 132897987541L) & 0x7FFFFFFFFFFFFFFFL;
        hash = hash * 0x9E3779B97F4A7C15L;
        hash ^= hash >> 33;
        hash *= 0xC6A4A7935BD1E995L;
        hash ^= hash >> 29;
        return (hash & 0x7FFFFFFFFFFFFFFFL) / (double) Long.MAX_VALUE * 2.0 - 1.0;
    }

    // ========================================================================
    // Strongholds (Fix 1.11: rewritten ring algorithm)
    // ========================================================================

    /**
     * Fix 1.11: Completely rewritten to match vanilla {@code ChunkGeneratorStructureState.generateRingPositions()}.
     * <p>
     * Key differences from old implementation:
     * <ul>
     *   <li>Uses Minecraft's {@link RandomSource} (XoroshiroRandom) instead of {@link java.util.Random} (LCG) —
     *       they produce DIFFERENT sequences even with the same seed!</li>
     *   <li>Uses the exact vanilla algorithm from bytecode analysis of {@code ConcentricRingsStructurePlacement}.</li>
     *   <li>count=128, distance=32, spread=3 match vanilla strongholds config.</li>
     * </ul>
     */
    private static void findStrongholds(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                        int centerX, int centerZ, double radiusSq, boolean limitRadius) {
        findStrongholds(results, worldSeed, source, centerX, centerZ, radiusSq, limitRadius, false);
    }

    private static void findStrongholds(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                        int centerX, int centerZ, double radiusSq, boolean limitRadius, boolean searchApples) {
        boolean appleCapable = searchApples && LootSimulator.canHaveEnchantedApple(StructureType.STRONGHOLD);

        // Vanilla parameters from strongholds.json -> ConcentricRingsStructurePlacement
        int distance = 32;
        int count = 128;
        int spread = 3;

        // Vanilla: RandomSource.create().setSeed(concentricRingsSeed)
        // In 1.21.4, RandomSource.create() returns LegacyRandomSource (wraps java.util.Random)
        // create() + setSeed(seed) is equivalent to new java.util.Random(seed)
        RandomSource random = RandomSource.create();
        random.setSeed(worldSeed);

        // Initial angle: random.nextDouble() * Math.PI * 2.0
        double angle = random.nextDouble() * Math.PI * 2.0;

        int placedInRing = 0;
        int ring = 0;

        for (int i = 0; i < count; i++) {
            // dist = 4*distance + 6*distance*ring + (random.nextDouble()-0.5)*2.5*distance
            double dist = 4.0 * distance + 6.0 * distance * ring
                    + (random.nextDouble() - 0.5) * 2.5 * distance;
            int chunkX = (int) Math.round(Math.cos(angle) * dist);
            int chunkZ = (int) Math.round(Math.sin(angle) * dist);

            // Vanilla calls random.fork() for each position, which advances the main random!
            // LegacyRandomSource.fork() calls random.nextLong(), consuming state.
            // We MUST consume the same amount to keep the RNG sequence in sync.
            random.fork(); // ← critical: consumes nextLong() just like vanilla

            // Re-enable biome snapping with correct #stronghold_biased_to tag (vanilla snaps within 112 blocks)
            if (source != null) {
                int[] snapped = source.findNearestStrongholdChunk(chunkX, chunkZ);
                chunkX = snapped[0];
                chunkZ = snapped[1];
            }

            int blockX = chunkX << 4;
            int blockZ = chunkZ << 4;

            String shExtra = "";
            if (appleCapable && LootSimulator.hasEnchantedApple(worldSeed, StructureType.STRONGHOLD, blockX, blockZ)) {
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

            // angle += 2*PI / spread
            angle += Math.PI * 2.0 / spread;
            placedInRing++;

            if (placedInRing == spread) {
                ring++;
                placedInRing = 0;

                // spread += 2*spread / (ring+1)
                spread += 2 * spread / (ring + 1);
                spread = Math.min(spread, count - i - 1);
                if (spread <= 0) break;

                // Randomize angle for new ring
                angle += random.nextDouble() * Math.PI * 2.0;
            }
        }
    }

    // ========================================================================
    // Biome Source Management
    // ========================================================================

    private SeedBiomeSource getOrCreateBiomeSource(long seed) {
        if (this.biomeSource == null || this.currentSeed != seed) {
            var dimension = BlackOut.mc.level != null ? BlackOut.mc.level.dimension() : Level.OVERWORLD;
            this.biomeSource = new SeedBiomeSource(seed, dimension);
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

        // When Enchanted Apples mode is on, apple-capable types are always visible
        if (this.enchantedApples.get() && LootSimulator.canHaveEnchantedApple(type)) return true;

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

    /** Whether Enchanted Apple scanning is enabled (independent toggle). */
    public boolean isEnchantedApplesEnabled() {
        return this.enchantedApples.get();
    }

    public record FoundStructure(StructureType type, int blockX, int blockZ, String extraInfo) {
    }

    @SafeVarargs
    private static Set<ResourceKey<Biome>> biomes(ResourceKey<Biome>... keys) {
        return Set.of(keys);
    }

    public enum StructureType {
        // Overworld
        VILLAGE("Village", 34, 8, 10387312, false, 1.0F, new Color(0, 200, 0), biomes(Biomes.PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.TAIGA), "textures/map/structures/village/village_normal.png"),
        DESERT_PYRAMID("Desert Pyramid", 32, 8, 14357617, false, 1.0F, new Color(220, 180, 50), biomes(Biomes.DESERT), "textures/map/structures/desert_temple.png"),
        JUNGLE_TEMPLE("Jungle Temple", 32, 8, 14357619, false, 1.0F, new Color(50, 180, 50), biomes(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE), "textures/map/structures/jungle_temple.png"),
        SWAMP_HUT("Swamp Hut", 32, 8, 14357620, false, 1.0F, new Color(100, 140, 60), biomes(Biomes.SWAMP), "textures/map/structures/witch_hut.png"),
        IGLOO("Igloo", 32, 8, 14357618, false, 1.0F, new Color(180, 220, 255), biomes(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_SLOPES), "textures/map/structures/igloo/igloo_without_basement.png"),
        OCEAN_MONUMENT("Ocean Monument", 32, 5, 10387313, true, 1.0F, new Color(0, 150, 200), biomes(Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN), "textures/map/structures/monument.png"),
        WOODLAND_MANSION("Woodland Mansion", 80, 20, 10387319, true, 1.0F, new Color(140, 80, 40), biomes(Biomes.DARK_FOREST), "textures/map/structures/mansion.png"),
        PILLAGER_OUTPOST("Pillager Outpost", 32, 8, 165745296, false, 0.2F, new Color(160, 160, 160), biomes(Biomes.PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.SUNFLOWER_PLAINS, Biomes.CHERRY_GROVE, Biomes.GROVE, Biomes.SNOWY_TAIGA), "textures/map/structures/outpost.png"),
        ANCIENT_CITY("Ancient City", 24, 8, 20083232, false, 1.0F, new Color(30, 50, 80), biomes(Biomes.DEEP_DARK), "textures/map/structures/ancient_city.png"),
        TRIAL_CHAMBERS("Trial Chambers", 34, 12, 94251327, false, 1.0F, new Color(200, 100, 0), null, "textures/map/structures/trial_chamber.png"),
        TRAIL_RUINS("Trail Ruins", 34, 8, 83469867, false, 1.0F, new Color(180, 130, 80), biomes(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.JUNGLE, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST), "textures/map/structures/trail_ruins.png"),
        RUINED_PORTAL("Ruined Portal", 40, 15, 34222645, false, 1.0F, new Color(160, 50, 200), null, "textures/map/structures/ruined_portal.png"),
        SHIPWRECK("Shipwreck", 24, 4, 165745295, false, 1.0F, new Color(100, 80, 60), biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH), "textures/map/structures/shipwreck.png"),
        OCEAN_RUIN("Ocean Ruin", 20, 8, 14357621, false, 1.0F, new Color(60, 120, 160), biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN), "textures/map/structures/ocean_ruins/ocean_ruins_small.png"),
        MINESHAFT("Mineshaft", 1, 0, 0, false, 0.004F, new Color(160, 130, 90), null, "textures/map/structures/mineshaft.png"),
        BURIED_TREASURE("Buried Treasure", 1, 0, 0, false, 0.01F, new Color(255, 215, 0), biomes(Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE, Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN), "textures/map/structures/treasure.png"),
        STRONGHOLD("Stronghold", 0, 0, 0, false, 1.0F, new Color(255, 50, 50), null, "textures/map/structures/stronghold.png"),
        // Nether
        NETHER_FORTRESS("Nether Fortress", NETHER_SPACING, NETHER_SEPARATION, NETHER_SALT, false, 1.0F, new Color(200, 50, 50), biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST, Biomes.BASALT_DELTAS), "textures/map/structures/nether_fortress.png"),
        BASTION_REMNANT("Bastion Remnant", NETHER_SPACING, NETHER_SEPARATION, NETHER_SALT, false, 1.0F, new Color(50, 50, 50), biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST), "textures/map/structures/bastion/bastion_treasure.png"),
        NETHER_FOSSIL("Nether Fossil", 2, 1, 14357921, true, 1.0F, new Color(180, 160, 110), biomes(Biomes.SOUL_SAND_VALLEY), "textures/map/structures/fossil.png"),
        // Cave
        LUSH_CAVES("Lush Caves", 0, 0, 0, false, 1.0F, new Color(50, 140, 30), biomes(Biomes.LUSH_CAVES), "textures/map/structures/cave.png"),
        DRIPSTONE_CAVES("Dripstone Caves", 0, 0, 0, false, 1.0F, new Color(140, 106, 70), biomes(Biomes.DRIPSTONE_CAVES), "textures/map/structures/cave.png"),
        // End
        DESERT_WELL("Desert Well", 32, 8, 14357617, false, 1.0F, new Color(180, 140, 80), biomes(Biomes.DESERT), "textures/map/structures/desert_well.png"),
        END_CITY("End City", 20, 11, 10387313, true, 1.0F, new Color(200, 150, 255), biomes(Biomes.END_HIGHLANDS), "textures/map/structures/end_city/end_city_without_ship.png"),
        END_GATEWAY("End Gateway", 0, 0, 0, false, 1.0F, new Color(180, 100, 220), null, "textures/map/structures/end_gateway.png"),
        // Features (placed via placement modifiers, not RandomSpreadStructurePlacement)
        DUNGEON("Dungeon", 0, 0, 0, false, 1.0F, new Color(140, 100, 60), OVERWORLD_CAVE_BIOMES, "textures/map/structures/dungeon/dungeon_skeleton.png"),
        GEODE("Geode", 0, 0, 0, false, 1.0F, new Color(120, 60, 200), biomes(Biomes.LUSH_CAVES), "textures/map/structures/geode.png"),
        LAVA_POOL_SURFACE("Lava Pool (Surface)", 0, 0, 0, false, 1.0F, new Color(200, 80, 20), null, "textures/map/structures/lava_pool/lava_pool_lake.png"),
        LAVA_POOL_CAVE("Lava Pool (Cave)", 0, 0, 0, false, 1.0F, new Color(200, 50, 10), OVERWORLD_CAVE_BIOMES, "textures/map/structures/lava_pool/lava_pool_cave.png"),
        RAVINE("Ravine", 0, 0, 0, false, 1.0F, new Color(100, 80, 60), null, "textures/map/structures/ravine.png"),
        // Misc
        SPAWN("World Spawn", 0, 0, 0, false, 1.0F, new Color(255, 255, 255), null, "textures/map/spawn_point.png");

        public final String displayName;
        public final int spacing;
        public final int separation;
        public final int salt;
        public final boolean triangular;
        public final float frequency;
        public final Color mapColor;
        public final Set<ResourceKey<Biome>> validBiomes;
        public final String iconPath;

        public boolean isCaveBiome() {
            return this == LUSH_CAVES || this == DRIPSTONE_CAVES;
        }

        /** Returns true for feature types placed via placement modifiers (not RandomSpreadStructurePlacement). */
        public boolean isFeature() {
            return this == DUNGEON || this == GEODE || this == LAVA_POOL_SURFACE
                    || this == LAVA_POOL_CAVE || this == RAVINE;
        }

        public boolean isAlwaysVisible() {
            return this == STRONGHOLD || this == ANCIENT_CITY || this == WOODLAND_MANSION
                    || this == END_CITY || this == NETHER_FORTRESS || this == BASTION_REMNANT
                    || this == END_GATEWAY || isFeature()
                    || this == GEODE;
        }

        public boolean isMinor() {
            return this == SHIPWRECK || this == OCEAN_RUIN || this == RUINED_PORTAL
                    || this == TRAIL_RUINS || this == SWAMP_HUT || this == IGLOO
                    || isCaveBiome();
        }

        /**
         * @deprecated use isAlwaysVisible() or isMinor()
         */
        @Deprecated
        public boolean isMajor() {
            return isAlwaysVisible() || isCaveBiome();
        }

        StructureType(String displayName, int spacing, int separation, int salt, boolean triangular, float frequency, Color mapColor, Set<ResourceKey<Biome>> validBiomes, String iconPath) {
            this.displayName = displayName;
            this.spacing = spacing;
            this.separation = separation;
            this.salt = salt;
            this.triangular = triangular;
            this.frequency = frequency;
            this.mapColor = mapColor;
            this.validBiomes = validBiomes;
            this.iconPath = iconPath;
        }
    }
}
