package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
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
import bodevelopment.client.blackout.util.SeedBiomeSource;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

public class SeedFinder extends Module {
    private static final long REGION_X_MULT = 341873128712L;
    private static final long REGION_Z_MULT = 132897987541L;

    private static final int NETHER_SPACING = 27;
    private static final int NETHER_SEPARATION = 4;
    private static final int NETHER_SALT = 30084232;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgStructures = this.addGroup("Structures");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<String> seed = this.sgGeneral.stringSetting("Seed", "", "World seed for structure generation.");
    private final Setting<Integer> searchRadius = this.sgGeneral.intSetting("Radius", 10000, 1000, 100000, 1000, "Search radius in blocks from the player.");
    private final Setting<Integer> renderDistance = this.sgGeneral.intSetting("Render Distance", 2000, 100, 20000, 100, "Max distance for 3D beam rendering.");
    private final Setting<Integer> recalcDistance = this.sgGeneral.intSetting("Recalc Distance", 512, 64, 2048, 64, "Distance the player must move before recalculating.");
    private final Setting<KeyBind> mapKey = this.sgGeneral.keySetting("Map Key", "Keybind to open the 2D seed map screen.");

    private final Setting<Boolean> villages = this.sgStructures.booleanSetting("Village", true, "Show village locations.");
    private final Setting<Boolean> desertPyramids = this.sgStructures.booleanSetting("Desert Pyramid", false, "Show desert pyramid locations.");
    private final Setting<Boolean> jungleTemples = this.sgStructures.booleanSetting("Jungle Temple", false, "Show jungle temple locations.");
    private final Setting<Boolean> swampHuts = this.sgStructures.booleanSetting("Swamp Hut", false, "Show swamp hut locations.");
    private final Setting<Boolean> igloos = this.sgStructures.booleanSetting("Igloo", false, "Show igloo locations.");
    private final Setting<Boolean> oceanMonuments = this.sgStructures.booleanSetting("Ocean Monument", false, "Show ocean monument locations.");
    private final Setting<Boolean> woodlandMansions = this.sgStructures.booleanSetting("Woodland Mansion", false, "Show woodland mansion locations.");
    private final Setting<Boolean> pillagerOutposts = this.sgStructures.booleanSetting("Pillager Outpost", false, "Show pillager outpost locations.");
    private final Setting<Boolean> ancientCities = this.sgStructures.booleanSetting("Ancient City", false, "Show ancient city locations.");
    private final Setting<Boolean> trialChambers = this.sgStructures.booleanSetting("Trial Chambers", false, "Show trial chamber locations.");
    private final Setting<Boolean> trailRuins = this.sgStructures.booleanSetting("Trail Ruins", false, "Show trail ruin locations.");
    private final Setting<Boolean> ruinedPortals = this.sgStructures.booleanSetting("Ruined Portal", false, "Show ruined portal locations.");
    private final Setting<Boolean> shipwrecks = this.sgStructures.booleanSetting("Shipwreck", false, "Show shipwreck locations.");
    private final Setting<Boolean> oceanRuins = this.sgStructures.booleanSetting("Ocean Ruin", false, "Show ocean ruin locations.");
    private final Setting<Boolean> strongholds = this.sgStructures.booleanSetting("Stronghold", true, "Show stronghold locations.");
    private final Setting<Boolean> deepDark = this.sgStructures.booleanSetting("Deep Dark", false, "Search for deep dark biome patches.");
    private final Setting<Boolean> lushCaves = this.sgStructures.booleanSetting("Lush Caves", false, "Search for lush caves biome patches.");
    private final Setting<Boolean> dripstoneCaves = this.sgStructures.booleanSetting("Dripstone Caves", false, "Search for dripstone caves biome patches.");
    private final Setting<Boolean> fortresses = this.sgStructures.booleanSetting("Nether Fortress", false, "Show nether fortress locations.");
    private final Setting<Boolean> bastions = this.sgStructures.booleanSetting("Bastion Remnant", false, "Show bastion remnant locations.");
    private final Setting<Boolean> endCities = this.sgStructures.booleanSetting("End City", false, "Show end city locations.");

    private final Setting<BlackOutColor> beamColor = this.sgRender.colorSetting("Beam Color", new BlackOutColor(255, 255, 50, 120), "Color for the structure beam.");
    private final Setting<BlackOutColor> textColor = this.sgRender.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), "Color for the structure label.");
    private final Setting<Double> textScale = this.sgRender.doubleSetting("Text Scale", 3.0, 0.5, 10.0, 0.5, "Scale of the structure label.");
    private final Setting<Integer> beamHeight = this.sgRender.intSetting("Beam Height", 256, 32, 512, 16, "Height of the structure beam.");
    private final Setting<Double> beamWidth = this.sgRender.doubleSetting("Beam Width", 0.5, 0.1, 2.0, 0.1, "Width of the structure beam.");

    private final List<FoundStructure> found = new ArrayList<>();
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

        KeyBind key = this.mapKey.get();
        boolean keyDown = key != null && key.isPressed();
        if (keyDown && !this.mapKeyWasDown) this.openSeedMap();
        this.mapKeyWasDown = keyDown;
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
        this.found.clear();
        long worldSeed = this.parseSeed();
        if (BlackOut.mc.player == null || BlackOut.mc.level == null) return;

        this.lastSeed = this.seed.get();
        this.lastCalcX = BlackOut.mc.player.getX();
        this.lastCalcZ = BlackOut.mc.player.getZ();

        SeedBiomeSource source = this.getOrCreateBiomeSource(worldSeed);
        ResourceKey<Level> dim = BlackOut.mc.level.dimension();

        findInArea(this.found, worldSeed, source, dim, (int) this.lastCalcX, (int) this.lastCalcZ, this.searchRadius.get(), true, t -> this.isDimensionEnabled(t, dim));

        this.found.sort(Comparator.comparingDouble(s -> {
            double ddx = s.blockX - this.lastCalcX;
            double ddz = s.blockZ - this.lastCalcZ;
            return ddx * ddx + ddz * ddz;
        }));
    }

    public static void findInArea(List<FoundStructure> results, long worldSeed, SeedBiomeSource source, ResourceKey<Level> dim,
                                  int centerX, int centerZ, int radius, boolean limitRadius,
                                  Predicate<StructureType> enabledCheck) {

        int playerChunkX = centerX >> 4;
        int playerChunkZ = centerZ >> 4;
        int radiusChunks = radius >> 4;
        double radiusSq = (double) radius * radius;

        if (dim == Level.NETHER) {
            findNetherStructures(results, worldSeed, source, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, enabledCheck);
        } else if (dim == Level.END) {
            if (enabledCheck.test(StructureType.END_CITY)) {
                findEndCities(results, worldSeed, source, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ);
            }
        } else {
            for (StructureType type : StructureType.values()) {
                if (!enabledCheck.test(type)) continue;
                if (type.isCaveBiome()) continue;

                if (type == StructureType.STRONGHOLD) {
                    findStrongholds(results, worldSeed, centerX, centerZ, radiusSq, limitRadius);
                } else {
                    findRandomSpread(results, worldSeed, source, type, playerChunkX, playerChunkZ, radiusChunks, radiusSq, limitRadius, centerX, centerZ);
                }
            }
            findCaveBiomes(results, source, centerX, centerZ, radius, radiusSq, limitRadius, enabledCheck);
        }
    }

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

                int checkX = locateX + 8;
                int checkZ = locateZ + 8;
                ResourceKey<Biome> biome = source.getBiome(checkX, checkZ);

                if (biome != Biomes.END_HIGHLANDS && biome != Biomes.END_MIDLANDS) {
                    continue;
                }

                if (!source.hasSolidTerrainAt(checkX, 60, checkZ)) {
                    continue;
                }

                long structureSeed = (long) chunkX * REGION_X_MULT + (long) chunkZ * REGION_Z_MULT ^ worldSeed;
                Random jigsawRandom = new Random(structureSeed);

                boolean hasShip = jigsawRandom.nextInt(2) == 0;
                String extraInfo = hasShip ? " Ship" : "";

                results.add(new FoundStructure(StructureType.END_CITY, locateX, locateZ, extraInfo));
            }
        }
    }

    private static void findRandomSpread(List<FoundStructure> results, long worldSeed, SeedBiomeSource source, StructureType type,
                                         int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq, boolean limitRadius,
                                         int centerX, int centerZ) {

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

                int offsetX = regionRandom.nextInt(range);
                int offsetZ = regionRandom.nextInt(range);

                int chunkX = rx * spacing + offsetX;
                int chunkZ = rz * spacing + offsetZ;

                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                if (type.validBiomes != null) {
                    ResourceKey<Biome> biome = source.getBiomeAt(blockX + 8, 64, blockZ + 8);
                    if (!type.validBiomes.contains(biome)) {
                        continue;
                    }
                }

                results.add(new FoundStructure(type, blockX, blockZ, ""));
            }
        }
    }

    private static void findNetherStructures(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                             int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq,
                                             boolean limitRadius, java.util.function.Predicate<StructureType> enabledCheck) {
        boolean showFortress = enabledCheck.test(StructureType.NETHER_FORTRESS);
        boolean showBastion = enabledCheck.test(StructureType.BASTION_REMNANT);
        if (!showFortress && !showBastion) return;

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

                boolean isBastion = type == StructureType.BASTION_REMNANT;
                if (isBastion && !showBastion) continue;
                if (!isBastion && !showFortress) continue;

                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;

                if (limitRadius) {
                    double dx = blockX - (playerChunkX << 4);
                    double dz = blockZ - (playerChunkZ << 4);
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                if (type.validBiomes != null && !type.validBiomes.contains(source.getBiome(blockX + 8, blockZ + 8))) continue;

                results.add(new FoundStructure(type, blockX, blockZ, ""));
            }
        }
    }

    public static StructureType getNetherStructureType(long worldSeed, int chunkX, int chunkZ) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);

        int roll = rand.nextInt(5);
        return roll < 2 ? StructureType.NETHER_FORTRESS : StructureType.BASTION_REMNANT;
    }

    private static void findCaveBiomes(List<FoundStructure> results, SeedBiomeSource source, int centerX, int centerZ,
                                       int radius, double radiusSq, boolean limitRadius,
                                       java.util.function.Predicate<StructureType> enabledCheck) {
        int scanStep = 128;
        int sectionSize = 512;

        StructureType[] caveTypes = { StructureType.DEEP_DARK, StructureType.LUSH_CAVES, StructureType.DRIPSTONE_CAVES };

        for (StructureType type : caveTypes) {
            if (!enabledCheck.test(type)) continue;
            ResourceKey<Biome> targetBiome = type.validBiomes.iterator().next();
            int scanY = (type == StructureType.DEEP_DARK) ? -52 : 16;

            int minSecX = Math.floorDiv(centerX - radius, sectionSize);
            int maxSecX = Math.floorDiv(centerX + radius, sectionSize);
            int minSecZ = Math.floorDiv(centerZ - radius, sectionSize);
            int maxSecZ = Math.floorDiv(centerZ + radius, sectionSize);

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

                        boolean tooClose = false;
                        for (FoundStructure s : results) {
                            if (s.type == type && Math.abs(s.blockX - finalX) < 400 && Math.abs(s.blockZ - finalZ) < 400) {
                                tooClose = true;
                                break;
                            }
                        }
                        if (!tooClose) {
                            results.add(new FoundStructure(type, finalX, finalZ, ""));
                        }
                    }
                }
            }
        }
    }

    private static void findStrongholds(List<FoundStructure> results, long worldSeed, int centerX, int centerZ,
                                        double radiusSq, boolean limitRadius) {
        Random random = new Random(worldSeed);
        double angle = random.nextDouble() * Math.PI * 2.0;
        int placedInRing = 0;
        int ring = 0;
        int currentSpread = 3;
        int count = 128;
        int distance = 32;

        for (int i = 0; i < count; i++) {
            double dist = (4 * distance + distance * 6 * ring) + (random.nextDouble() - 0.5) * distance * 2.5;
            int chunkX = (int) Math.round(Math.cos(angle) * dist);
            int chunkZ = (int) Math.round(Math.sin(angle) * dist);

            int blockX = chunkX << 4;
            int blockZ = chunkZ << 4;

            if (limitRadius) {
                double dx = blockX - centerX;
                double dz = blockZ - centerZ;
                if (dx * dx + dz * dz <= radiusSq) {
                    results.add(new FoundStructure(StructureType.STRONGHOLD, blockX, blockZ, ""));
                }
            } else {
                results.add(new FoundStructure(StructureType.STRONGHOLD, blockX, blockZ, ""));
            }

            angle += Math.PI * 2.0 / currentSpread;
            placedInRing++;
            if (placedInRing == currentSpread) {
                ring++;
                placedInRing = 0;
                currentSpread += 2 * currentSpread / (ring + 1);
                currentSpread = Math.min(currentSpread, count - i - 1);
                if (currentSpread <= 0) break;
                angle += random.nextDouble() * Math.PI * 2.0;
            }
        }
    }

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

        boolean isNetherStruct = type == StructureType.NETHER_FORTRESS || type == StructureType.BASTION_REMNANT;
        boolean isEndStruct = type == StructureType.END_CITY;

        if (dim == Level.NETHER) return isNetherStruct && isStructureEnabled(type);
        if (dim == Level.END) return isEndStruct && isStructureEnabled(type);
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
        return switch (type) {
            case VILLAGE -> this.villages.get();
            case DESERT_PYRAMID -> this.desertPyramids.get();
            case JUNGLE_TEMPLE -> this.jungleTemples.get();
            case SWAMP_HUT -> this.swampHuts.get();
            case IGLOO -> this.igloos.get();
            case OCEAN_MONUMENT -> this.oceanMonuments.get();
            case WOODLAND_MANSION -> this.woodlandMansions.get();
            case PILLAGER_OUTPOST -> this.pillagerOutposts.get();
            case ANCIENT_CITY -> this.ancientCities.get();
            case TRIAL_CHAMBERS -> this.trialChambers.get();
            case TRAIL_RUINS -> this.trailRuins.get();
            case RUINED_PORTAL -> this.ruinedPortals.get();
            case SHIPWRECK -> this.shipwrecks.get();
            case OCEAN_RUIN -> this.oceanRuins.get();
            case STRONGHOLD -> this.strongholds.get();
            case NETHER_FORTRESS -> this.fortresses.get();
            case BASTION_REMNANT -> this.bastions.get();
            case END_CITY -> this.endCities.get();
            case DEEP_DARK -> this.deepDark.get();
            case LUSH_CAVES -> this.lushCaves.get();
            case DRIPSTONE_CAVES -> this.dripstoneCaves.get();
        };
    }

    public record FoundStructure(StructureType type, int blockX, int blockZ, String extraInfo) {}

    @SafeVarargs
    private static Set<ResourceKey<Biome>> biomes(ResourceKey<Biome>... keys) {
        return Set.of(keys);
    }

    public enum StructureType {
        // Overworld
        VILLAGE("Village", 34, 8, 10387312, false, new Color(0, 200, 0), biomes(Biomes.PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.TAIGA)),
        DESERT_PYRAMID("Desert Pyramid", 32, 8, 14357617, false, new Color(220, 180, 50), biomes(Biomes.DESERT)),
        JUNGLE_TEMPLE("Jungle Temple", 32, 8, 14357619, false, new Color(50, 180, 50), biomes(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE)),
        SWAMP_HUT("Swamp Hut", 32, 8, 14357620, false, new Color(100, 140, 60), biomes(Biomes.SWAMP)),
        IGLOO("Igloo", 32, 8, 14357618, false, new Color(180, 220, 255), biomes(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_SLOPES)),
        OCEAN_MONUMENT("Ocean Monument", 32, 5, 10387313, true, new Color(0, 150, 200), biomes(Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN)),
        WOODLAND_MANSION("Woodland Mansion", 80, 20, 10387319, true, new Color(140, 80, 40), biomes(Biomes.DARK_FOREST)),
        PILLAGER_OUTPOST("Pillager Outpost", 32, 8, 165745296, false, new Color(160, 160, 160), biomes(Biomes.PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.SUNFLOWER_PLAINS, Biomes.CHERRY_GROVE, Biomes.GROVE, Biomes.SNOWY_TAIGA)),
        ANCIENT_CITY("Ancient City", 24, 8, 20083232, false, new Color(30, 50, 80), biomes(Biomes.DEEP_DARK)),
        TRIAL_CHAMBERS("Trial Chambers", 34, 12, 94251327, false, new Color(200, 100, 0), null),
        TRAIL_RUINS("Trail Ruins", 34, 8, 83469867, false, new Color(180, 130, 80), biomes(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.JUNGLE, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST)),
        RUINED_PORTAL("Ruined Portal", 40, 15, 34222645, false, new Color(160, 50, 200), null),
        SHIPWRECK("Shipwreck", 24, 4, 165745295, false, new Color(100, 80, 60), biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH)),
        OCEAN_RUIN("Ocean Ruin", 20, 8, 14357621, false, new Color(60, 120, 160), biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN)),
        STRONGHOLD("Stronghold", 0, 0, 0, false, new Color(255, 50, 50), null),
        // Nether
        NETHER_FORTRESS("Nether Fortress", NETHER_SPACING, NETHER_SEPARATION, NETHER_SALT, false, new Color(200, 50, 50), biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST, Biomes.BASALT_DELTAS)),
        BASTION_REMNANT("Bastion Remnant", NETHER_SPACING, NETHER_SEPARATION, NETHER_SALT, false, new Color(50, 50, 50), biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST)),
        // Cave
        DEEP_DARK("Deep Dark", 0, 0, 0, false, new Color(10, 20, 35), biomes(Biomes.DEEP_DARK)),
        LUSH_CAVES("Lush Caves", 0, 0, 0, false, new Color(50, 140, 30), biomes(Biomes.LUSH_CAVES)),
        DRIPSTONE_CAVES("Dripstone Caves", 0, 0, 0, false, new Color(140, 106, 70), biomes(Biomes.DRIPSTONE_CAVES)),
        // End
        END_CITY("End City", 20, 11, 10387313, false, new Color(200, 150, 255), biomes(Biomes.END_MIDLANDS, Biomes.END_HIGHLANDS));

        public final String displayName;
        public final int spacing;
        public final int separation;
        public final int salt;
        public final boolean triangular;
        public final Color mapColor;
        public final Set<ResourceKey<Biome>> validBiomes;

        public boolean isCaveBiome() {
            return this == DEEP_DARK || this == LUSH_CAVES || this == DRIPSTONE_CAVES;
        }

        public boolean isMajor() {
            return this == STRONGHOLD || isCaveBiome();
        }

        StructureType(String displayName, int spacing, int separation, int salt, boolean triangular, Color mapColor, Set<ResourceKey<Biome>> validBiomes) {
            this.displayName = displayName;
            this.spacing = spacing;
            this.separation = separation;
            this.salt = salt;
            this.triangular = triangular;
            this.mapColor = mapColor;
            this.validBiomes = validBiomes;
        }
    }
}
