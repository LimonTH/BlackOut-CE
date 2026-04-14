package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.OnlyDev;
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
import bodevelopment.client.blackout.util.SelectedComponent;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final Setting<Boolean> lushCaves = this.sgStructures.booleanSetting("Lush Caves", false, "Search for lush caves biome patches.");
    private final Setting<Boolean> dripstoneCaves = this.sgStructures.booleanSetting("Dripstone Caves", false, "Search for dripstone caves biome patches.");
    private final Setting<Boolean> fortresses = this.sgStructures.booleanSetting("Nether Fortress", false, "Show nether fortress locations.");
    private final Setting<Boolean> bastions = this.sgStructures.booleanSetting("Bastion Remnant", false, "Show bastion remnant locations.");
    private final Setting<Boolean> endCities = this.sgStructures.booleanSetting("End City", false, "Show end city locations.");
    private final Setting<Boolean> worldSpawn = this.sgStructures.booleanSetting("World Spawn", false, "Show world spawn position.");

    private final Setting<BlackOutColor> beamColor = this.sgRender.colorSetting("Beam Color", new BlackOutColor(255, 255, 50, 120), "Color for the structure beam.");
    private final Setting<BlackOutColor> textColor = this.sgRender.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), "Color for the structure label.");
    private final Setting<Double> textScale = this.sgRender.doubleSetting("Text Scale", 3.0, 0.5, 10.0, 0.5, "Scale of the structure label.");
    private final Setting<Integer> beamHeight = this.sgRender.intSetting("Beam Height", 256, 32, 512, 16, "Height of the structure beam.");
    private final Setting<Double> beamWidth = this.sgRender.doubleSetting("Beam Width", 0.5, 0.1, 2.0, 0.1, "Width of the structure beam.");

    private final List<FoundStructure> found = new CopyOnWriteArrayList<>();
    public List<FoundStructure> getFound() { return this.found; }
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

        this.calcFuture = CompletableFuture.runAsync(() -> {
            List<FoundStructure> newFound = new ArrayList<>();

            findInArea(newFound, worldSeed, source, dim, startX, startZ, radius, true, t -> this.isDimensionEnabled(t, dim));

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
            if (enabledCheck.test(StructureType.SPAWN)) findSpawnPos(results, source);
            for (StructureType type : StructureType.values()) {
                if (!enabledCheck.test(type)) continue;
                if (type.isCaveBiome()) continue;

                if (type == StructureType.STRONGHOLD) {
                    findStrongholds(results, worldSeed, source, centerX, centerZ, radiusSq, limitRadius);
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
        // TODO: Сuts off some existing cities and adds some phantom ones, and also sometimes incorrectly detects ships
        int spacing = StructureType.END_CITY.spacing;
        int separation = StructureType.END_CITY.separation;
        int range = spacing - separation;

        int minRegX = Math.floorDiv(playerChunkX - radiusChunks, spacing);
        int maxRegX = Math.floorDiv(playerChunkX + radiusChunks, spacing);
        int minRegZ = Math.floorDiv(playerChunkZ - radiusChunks, spacing);
        int maxRegZ = Math.floorDiv(playerChunkZ + radiusChunks, spacing);

        // Precompute rotation seed components — vanilla setLargeFeatureSeed: new Random(seed) -> nextLong(), nextLong()
        // These depend only on worldSeed, not on chunk position, so compute once outside the loop.
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

                // 1) Primary biome check at chunk center.
                // End biome distribution is seed-independent (TheEndBiomeSource uses fixed island noise).
                ResourceKey<Biome> biome = source.getBiome(locateX + 8, locateZ + 8);
                if (biome != Biomes.END_HIGHLANDS && biome != Biomes.END_MIDLANDS) continue;

                // 2) Corner height check — vanilla requires min(4 corner surface heights) >= 60.
                // Uses finalDensity(blockX, 60, blockZ) > 0 via SinglePointContext, which is the
                // exact equivalent of getFirstOccupiedHeight >= 60 for the End dimension.
                // All End noise is seed-independent so any SeedBiomeSource gives correct results.
                long rotSeed = (long) chunkX * la ^ (long) chunkZ * lb ^ worldSeed;
                int rot = new Random(rotSeed).nextInt(4);
                int di = (rot == 1 || rot == 2) ? -5 : 5;
                int dj = (rot == 2 || rot == 3) ? -5 : 5;
                int k = locateX + 7;
                int l = locateZ + 7;

                if (!source.hasSolidTerrainAtY60(k, l) || !source.hasSolidTerrainAtY60(k, l + dj) ||
                    !source.hasSolidTerrainAtY60(k + di, l) || !source.hasSolidTerrainAtY60(k + di, l + dj)) {
                    continue;
                }

                boolean hasShip = simulateEndCityShip(worldSeed, chunkX, chunkZ);
                results.add(new FoundStructure(StructureType.END_CITY, locateX, locateZ, hasShip ? " Ship" : ""));
            }
        }
    }

    // End City ship simulation (vanilla-accurate with collision detection
    // Template sizes from vanilla 1.21.4 NBT: {sizeX, sizeY, sizeZ}
    private static final int T_BASE_FLOOR = 0, T_BASE_ROOF = 1, T_BRIDGE_END = 2, T_BRIDGE_GENTLE = 3,
            T_BRIDGE_PIECE = 4, T_BRIDGE_STEEP = 5, T_FAT_BASE = 6, T_FAT_MID = 7, T_FAT_TOP = 8,
            T_FLOOR2_1 = 9, T_FLOOR2_2 = 10, T_ROOF2 = 11, T_SHIP = 12, T_FLOOR3_1 = 13, T_FLOOR3_2 = 14,
            T_ROOF3 = 15, T_TOWER_BASE = 16, T_TOWER_PIECE = 17, T_TOWER_TOP = 18;
    private static final int[][] TSIZES = {
            {10,4,10},{12,2,12},{5,6,2},{5,7,8},{5,6,4},{5,7,4},{13,4,13},{13,8,13},{17,6,17},
            {12,8,12},{12,8,12},{14,2,14},{13,24,29},{14,8,14},{14,8,14},{16,2,16},{7,7,7},{7,4,7},{9,5,9}
    };
    private static final int[][] TOWER_BD = {{0,1,-1,0},{1,6,-1,1},{3,0,-1,5},{2,5,-1,6}};
    private static final int[][] FAT_BD = {{0,4,-1,0},{1,12,-1,4},{3,0,-1,8},{2,8,-1,12}};

    // Rotate (x,z) by rotation ordinal (0=NONE,1=CW90,2=CW180,3=CCW90), pivot (0,0)
    private static int rx(int x, int z, int r) { return switch(r){case 1->z;case 2-> -x;case 3-> -z;default->x;}; }
    private static int rz(int x, int z, int r) { return switch(r){case 1-> -x;case 2-> -z;case 3->x;default->z;}; }

    // Simulated piece: position + bounding box + genDepth + rotation
    private record SP(int px,int py,int pz, int x1,int y1,int z1, int x2,int y2,int z2, int gd, int rot) {}

    private static SP mkp(int px, int py, int pz, int t, int rot, int gd) {
        int sx=TSIZES[t][0]-1, sy=TSIZES[t][1]-1, sz=TSIZES[t][2]-1;
        int cx=rx(sx,sz,rot), cz=rz(sx,sz,rot);
        return new SP(px,py,pz, px+Math.min(0,cx),py,pz+Math.min(0,cz), px+Math.max(0,cx),py+sy,pz+Math.max(0,cz), gd, rot);
    }

    // Child piece: position = parent.pos + rotate(offset, parentRot)
    private static SP child(SP par, int ox, int oy, int oz, int t, int rot) {
        return mkp(par.px+rx(ox,oz,par.rot), par.py+oy, par.pz+rz(ox,oz,par.rot), t, rot, par.gd);
    }

    private static boolean overlaps(SP a, SP b) {
        return a.x2>=b.x1 && a.x1<=b.x2 && a.y2>=b.y1 && a.y1<=b.y2 && a.z2>=b.z1 && a.z1<=b.z2;
    }

    private static SP findHit(List<SP> pieces, SP test) {
        for (SP p : pieces) if (overlaps(p, test)) return p;
        return null;
    }

    private static boolean simulateEndCityShip(long worldSeed, int chunkX, int chunkZ) {
        Random r = new Random(worldSeed);
        long a = r.nextLong(), b = r.nextLong();
        r = new Random((long)chunkX * a + (long)chunkZ * b ^ worldSeed);
        int rot = r.nextInt(4); // Rotation.getRandom

        // startHouseTower: initial pieces (no random calls)
        List<SP> pieces = new ArrayList<>();
        SP p = mkp(0, 0, 0, T_BASE_FLOOR, rot, 0); pieces.add(p);
        p = child(p, -1, 0, -1, T_FLOOR2_1, rot); pieces.add(p);
        p = child(p, -1, 4, -1, T_FLOOR3_1, rot); pieces.add(p);
        p = child(p, -1, 8, -1, T_ROOF3, rot); pieces.add(p);

        boolean[] ship = {false};
        ecRC(pieces, r, 0, 1, p, rot, ship);
        return ship[0];
    }

    // recursiveChildren: genType 0=TOWER, 1=BRIDGE, 2=HOUSE, 3=FAT
    private static boolean ecRC(List<SP> main, Random r, int gt, int depth, SP parent, int rot, boolean[] ship) {
        return ecRC(main, r, gt, depth, parent, rot, ship, 0, 0, 0);
    }

    private static boolean ecRC(List<SP> main, Random r, int gt, int depth, SP parent, int rot, boolean[] ship,
                                int bpX, int bpY, int bpZ) {
        if (depth > 8) return false;
        List<SP> tmp = new ArrayList<>();
        boolean ok = switch(gt) {
            case 0 -> ecTower(tmp, r, depth, parent, rot, ship);
            case 1 -> ecBridge(tmp, r, depth, parent, rot, ship);
            case 2 -> ecHouse(tmp, r, depth, parent, rot, ship, bpX, bpY, bpZ);
            case 3 -> ecFat(tmp, r, depth, parent, rot, ship);
            default -> false;
        };
        if (ok) {
            int gd = r.nextInt();
            boolean collision = false;
            for (SP tp : tmp) {
                SP tp2 = new SP(tp.px,tp.py,tp.pz, tp.x1,tp.y1,tp.z1, tp.x2,tp.y2,tp.z2, gd, tp.rot);
                SP hit = findHit(main, tp2);
                if (hit != null && hit.gd != parent.gd) { collision = true; break; }
            }
            if (!collision) {
                for (SP tp : tmp) main.add(new SP(tp.px,tp.py,tp.pz, tp.x1,tp.y1,tp.z1, tp.x2,tp.y2,tp.z2, gd, tp.rot));
                return true;
            }
        }
        return false;
    }

    private static boolean ecTower(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship) {
        int tox = 3+r.nextInt(2), toz = 3+r.nextInt(2);
        SP p = child(par, tox, -3, toz, T_TOWER_BASE, rot); L.add(p);
        p = child(p, 0, 7, 0, T_TOWER_PIECE, rot); L.add(p);
        SP anchor = r.nextInt(3)==0 ? p : null;
        int layers = 1 + r.nextInt(3);
        for (int k=0; k<layers; k++) {
            p = child(p, 0, 4, 0, T_TOWER_PIECE, rot); L.add(p);
            if (k < layers-1 && r.nextBoolean()) anchor = p;
        }
        if (anchor != null) {
            for (int[] bd : TOWER_BD) {
                if (r.nextBoolean()) {
                    int br = (rot+bd[0])%4;
                    SP be = child(anchor, bd[1], bd[2], bd[3], T_BRIDGE_END, br); L.add(be);
                    ecRC(L, r, 1, d+1, be, br, ship);
                }
            }
            SP top = child(p, -1, 4, -1, T_TOWER_TOP, rot); L.add(top);
        } else {
            if (d != 7) ecRC(L, r, 3, d+1, p, rot, ship);
            SP top = child(p, -1, 4, -1, T_TOWER_TOP, rot); L.add(top);
        }
        return true;
    }

    private static boolean ecBridge(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship) {
        int blen = r.nextInt(4)+1;
        SP p = child(par, 0, 0, -4, T_BRIDGE_PIECE, rot); L.add(p);
        int k = 0;
        for (int l=0; l<blen; l++) {
            if (r.nextBoolean()) {
                p = child(p, 0, k, -4, T_BRIDGE_PIECE, rot); L.add(p); k = 0;
            } else {
                if (r.nextBoolean()) { p = child(p, 0, k, -4, T_BRIDGE_STEEP, rot); }
                else { p = child(p, 0, k, -8, T_BRIDGE_GENTLE, rot); }
                L.add(p); k = 4;
            }
        }
        if (!ship[0] && r.nextInt(10-d)==0) {
            SP s = child(p, -8+r.nextInt(8), k, -70+r.nextInt(10), T_SHIP, rot); L.add(s);
            ship[0] = true;
        } else if (!ecRC(L, r, 2, d+1, p, rot, ship, -3, k+1, -11)) {
            return false;
        }
        SP end = child(p, 4, k, 0, T_BRIDGE_END, (rot+2)%4); L.add(end);
        return true;
    }

    private static boolean ecHouse(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship,
                                   int bpX, int bpY, int bpZ) {
        if (d > 8) return false;
        SP p = child(par, bpX, bpY, bpZ, T_BASE_FLOOR, rot); L.add(p);
        int j = r.nextInt(3);
        if (j==0) {
            p = child(p, -1, 4, -1, T_BASE_ROOF, rot); L.add(p);
        } else if (j==1) {
            p = child(p, -1, 0, -1, T_FLOOR2_2, rot); L.add(p);
            p = child(p, -1, 8, -1, T_ROOF2, rot); L.add(p);
            ecRC(L, r, 0, d+1, p, rot, ship);
        } else {
            p = child(p, -1, 0, -1, T_FLOOR2_2, rot); L.add(p);
            p = child(p, -1, 4, -1, T_FLOOR3_2, rot); L.add(p);
            p = child(p, -1, 8, -1, T_ROOF3, rot); L.add(p);
            ecRC(L, r, 0, d+1, p, rot, ship);
        }
        return true;
    }

    private static boolean ecFat(List<SP> L, Random r, int d, SP par, int rot, boolean[] ship) {
        SP p = child(par, -3, 4, -3, T_FAT_BASE, rot); L.add(p);
        p = child(p, 0, 4, 0, T_FAT_MID, rot); L.add(p);
        for (int j=0; j<2 && r.nextInt(3)!=0; j++) {
            p = child(p, 0, 8, 0, T_FAT_MID, rot); L.add(p);
            for (int[] bd : FAT_BD) {
                if (r.nextBoolean()) {
                    int br = (rot+bd[0])%4;
                    SP be = child(p, bd[1], bd[2], bd[3], T_BRIDGE_END, br); L.add(be);
                    ecRC(L, r, 1, d+1, be, br, ship);
                }
            }
        }
        SP top = child(p, -2, 8, -2, T_FAT_TOP, rot); L.add(top);
        return true;
    }
    // End of End City simulation

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

                ResourceKey<Biome> biome = null;
                if (type.validBiomes != null) {
                    biome = source.getBiomeAt(blockX + 8, 64, blockZ + 8);
                    if (!type.validBiomes.contains(biome)) continue;
                }

                String extra = "";
                if (type == StructureType.IGLOO) {
                    extra = getIglooExtra(worldSeed, chunkX, chunkZ);
                } else if (type == StructureType.OCEAN_RUIN) {
                    boolean isWarm = biome == Biomes.WARM_OCEAN
                            || biome == Biomes.LUKEWARM_OCEAN
                            || biome == Biomes.DEEP_LUKEWARM_OCEAN;
                    extra = getOceanRuinExtra(worldSeed, chunkX, chunkZ, isWarm);
                } else if (type == StructureType.VILLAGE && biome != null) {
                    extra = isZombieVillage(worldSeed, chunkX, chunkZ)
                            ? "zombie_" + biome.location().getPath()
                            : biome.location().getPath();
                }

                results.add(new FoundStructure(type, blockX, blockZ, extra));
            }
        }
    }

    private static void findNetherStructures(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                             int playerChunkX, int playerChunkZ, int radiusChunks, double radiusSq,
                                             boolean limitRadius, Predicate<StructureType> enabledCheck) {
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

                String extra = "";
                if (type == StructureType.BASTION_REMNANT) {
                    extra = getBastionType(worldSeed, chunkX, chunkZ);
                }

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

                results.add(new FoundStructure(type, blockX, blockZ, extra));
            }
        }
    }

    private static boolean isZombieVillage(long worldSeed, int chunkX, int chunkZ) {
        // Mirrors VillageStructure.findGenerationPoint → setLargeFeatureSeed → nextFloat() < 0.02
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long)chunkX * a ^ (long)chunkZ * b ^ worldSeed);
        rand.nextInt(4); // Rotation.getRandom
        return rand.nextFloat() < 0.02F;
    }

    public static StructureType getNetherStructureType(long worldSeed, int chunkX, int chunkZ) {
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ worldSeed);

        int roll = rand.nextInt(5);
        return roll < 2 ? StructureType.NETHER_FORTRESS : StructureType.BASTION_REMNANT;
    }

    public static String getBastionType(long worldSeed, int chunkX, int chunkZ) {
        // Mirrors BastionRemnantStructure.findGenerationPoint → setLargeFeatureSeed → WeightedRandomList.getRandom(nextInt(4))
        // Order matches BastionPieces.BastionType enum: HOGLIN_STABLES, HOUSING_UNITS, BRIDGE, TREASURE
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long)chunkX * a ^ (long)chunkZ * b ^ worldSeed);
        return switch (rand.nextInt(4)) {
            case 0 -> "hoglin";    // bastion_hoglin.png
            case 1 -> "housing";   // bastion_housing.png
            case 2 -> "bridges";   // bastion_bridges.png
            default -> "treasure"; // bastion_treasure.png
        };
    }

    private static String getIglooExtra(long worldSeed, int chunkX, int chunkZ) {
        // Mirrors IglooStructure.findGenerationPoint → setLargeFeatureSeed
        // → Rotation.getRandom(random) consumes nextInt(4)
        // → IglooStructurePieces: nextInt(10) == 0 → basement exists
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long)chunkX * a ^ (long)chunkZ * b ^ worldSeed);
        rand.nextInt(4); // Rotation.getRandom
        return rand.nextInt(10) == 0 ? "Laboratory" : "";
    }

    private static String getOceanRuinExtra(long worldSeed, int chunkX, int chunkZ, boolean isWarm) {
        // Mirrors OceanRuinStructure.findGenerationPoint → setLargeFeatureSeed
        // → nextFloat() < largeProbability: warm=0.3 (30% large), cold=0.9 (90% large)
        Random rand = new Random(worldSeed);
        long a = rand.nextLong();
        long b = rand.nextLong();
        rand.setSeed((long)chunkX * a ^ (long)chunkZ * b ^ worldSeed);
        boolean large = rand.nextFloat() < (isWarm ? 0.3F : 0.9F);
        return large ? "Big" : "Small";
    }

    private static void findSpawnPos(List<FoundStructure> results, SeedBiomeSource source) {
        // Mirrors ServerLevel.findSpawnBiome — expand outward from (0,0) in quart steps,
        // pick the first position that has a valid overworld spawn biome (non-ocean, non-nether, non-end).
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
        // All overworld biomes except ocean/deep-ocean variants are valid for spawn
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

    private static void findCaveBiomes(List<FoundStructure> results, SeedBiomeSource source, int centerX, int centerZ,
                                       int radius, double radiusSq, boolean limitRadius,
                                       java.util.function.Predicate<StructureType> enabledCheck) {
        int scanStep = 128;
        int sectionSize = 512;

        StructureType[] caveTypes = {StructureType.LUSH_CAVES, StructureType.DRIPSTONE_CAVES };

        for (StructureType type : caveTypes) {
            if (!enabledCheck.test(type)) continue;
            ResourceKey<Biome> targetBiome = type.validBiomes.iterator().next();
            int scanY = 16;

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

    private static void findStrongholds(List<FoundStructure> results, long worldSeed, SeedBiomeSource source,
                                        int centerX, int centerZ, double radiusSq, boolean limitRadius) {
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

            // TODO
            if (source != null) {
                int[] snapped = source.findNearestStrongholdChunk(chunkX, chunkZ);
                chunkX = snapped[0];
                chunkZ = snapped[1];
            }

            int blockX = (chunkX << 4) + 8;
            int blockZ = (chunkZ << 4) + 8;

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
            case LUSH_CAVES -> this.lushCaves.get();
            case DRIPSTONE_CAVES -> this.dripstoneCaves.get();
            case SPAWN -> this.worldSpawn.get();
        };
    }

    public record FoundStructure(StructureType type, int blockX, int blockZ, String extraInfo) {}

    @SafeVarargs
    private static Set<ResourceKey<Biome>> biomes(ResourceKey<Biome>... keys) {
        return Set.of(keys);
    }

    public enum StructureType {
        // Overworld
        VILLAGE("Village", 34, 8, 10387312, false, new Color(0, 200, 0), biomes(Biomes.PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.TAIGA), "textures/map/structures/village/village_normal.png"),
        DESERT_PYRAMID("Desert Pyramid", 32, 8, 14357617, false, new Color(220, 180, 50), biomes(Biomes.DESERT), "textures/map/structures/desert_temple.png"),
        JUNGLE_TEMPLE("Jungle Temple", 32, 8, 14357619, false, new Color(50, 180, 50), biomes(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE), "textures/map/structures/jungle_temple.png"),
        SWAMP_HUT("Swamp Hut", 32, 8, 14357620, false, new Color(100, 140, 60), biomes(Biomes.SWAMP), "textures/map/structures/witch_hut.png"),
        IGLOO("Igloo", 32, 8, 14357618, false, new Color(180, 220, 255), biomes(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_SLOPES), "textures/map/structures/igloo/igloo_without_basement.png"),
        OCEAN_MONUMENT("Ocean Monument", 32, 5, 10387313, true, new Color(0, 150, 200), biomes(Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN), "textures/map/structures/monument.png"),
        WOODLAND_MANSION("Woodland Mansion", 80, 20, 10387319, true, new Color(140, 80, 40), biomes(Biomes.DARK_FOREST), "textures/map/structures/mansion.png"),
        PILLAGER_OUTPOST("Pillager Outpost", 32, 8, 165745296, false, new Color(160, 160, 160), biomes(Biomes.PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA, Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.SUNFLOWER_PLAINS, Biomes.CHERRY_GROVE, Biomes.GROVE, Biomes.SNOWY_TAIGA), "textures/map/structures/outpost.png"),
        ANCIENT_CITY("Ancient City", 24, 8, 20083232, false, new Color(30, 50, 80), biomes(Biomes.DEEP_DARK), "textures/map/structures/ancient_city.png"),
        TRIAL_CHAMBERS("Trial Chambers", 34, 12, 94251327, false, new Color(200, 100, 0), null, "textures/map/structures/trial_chamber.png"),
        TRAIL_RUINS("Trail Ruins", 34, 8, 83469867, false, new Color(180, 130, 80), biomes(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.JUNGLE, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST), "textures/map/structures/trail_ruins.png"),
        RUINED_PORTAL("Ruined Portal", 40, 15, 34222645, false, new Color(160, 50, 200), null, "textures/map/structures/ruined_portal.png"),
        SHIPWRECK("Shipwreck", 24, 4, 165745295, false, new Color(100, 80, 60), biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH), "textures/map/structures/shipwreck.png"),
        OCEAN_RUIN("Ocean Ruin", 20, 8, 14357621, false, new Color(60, 120, 160), biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN), "textures/map/structures/ocean_ruins/ocean_ruins_small.png"),
        STRONGHOLD("Stronghold", 0, 0, 0, false, new Color(255, 50, 50), null, "textures/map/structures/stronghold.png"),
        // Nether
        NETHER_FORTRESS("Nether Fortress", NETHER_SPACING, NETHER_SEPARATION, NETHER_SALT, false, new Color(200, 50, 50), biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST, Biomes.BASALT_DELTAS), "textures/map/structures/nether_fortress.png"),
        BASTION_REMNANT("Bastion Remnant", NETHER_SPACING, NETHER_SEPARATION, NETHER_SALT, false, new Color(50, 50, 50), biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY, Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST), "textures/map/structures/bastion/bastion_treasure.png"),
        // Cave
        LUSH_CAVES("Lush Caves", 0, 0, 0, false, new Color(50, 140, 30), biomes(Biomes.LUSH_CAVES), "textures/map/structures/cave.png"),
        DRIPSTONE_CAVES("Dripstone Caves", 0, 0, 0, false, new Color(140, 106, 70), biomes(Biomes.DRIPSTONE_CAVES), "textures/map/structures/cave.png"),
        // End
        END_CITY("End City", 20, 11, 10387313, true, new Color(200, 150, 255), biomes(Biomes.END_HIGHLANDS), "textures/map/structures/end_city/end_city_without_ship.png"),
        // Misc
        SPAWN("World Spawn", 0, 0, 0, false, new Color(255,255,255), null, "textures/map/spawn_point.png");

        public final String displayName;
        public final int spacing;
        public final int separation;
        public final int salt;
        public final boolean triangular;
        public final Color mapColor;
        public final Set<ResourceKey<Biome>> validBiomes;
        public final String iconPath;

        public boolean isCaveBiome() {
            return this == LUSH_CAVES || this == DRIPSTONE_CAVES;
        }

        /** Shown at ALL zoom levels (very important landmarks). */
        public boolean isAlwaysVisible() {
            return this == STRONGHOLD || this == ANCIENT_CITY || this == WOODLAND_MANSION
                    || this == END_CITY || this == NETHER_FORTRESS || this == BASTION_REMNANT;
        }

        /** Small/common structures hidden when zoomed out (bpp >= 12). */
        public boolean isMinor() {
            return this == SHIPWRECK || this == OCEAN_RUIN || this == RUINED_PORTAL
                    || this == TRAIL_RUINS || this == SWAMP_HUT || this == IGLOO
                    || isCaveBiome();
        }

        /** @deprecated use isAlwaysVisible() or isMinor() */
        public boolean isMajor() {
            return isAlwaysVisible() || isCaveBiome();
        }

        StructureType(String displayName, int spacing, int separation, int salt, boolean triangular, Color mapColor, Set<ResourceKey<Biome>> validBiomes, String iconPath) {
            this.displayName = displayName;
            this.spacing = spacing;
            this.separation = separation;
            this.salt = salt;
            this.triangular = triangular;
            this.mapColor = mapColor;
            this.validBiomes = validBiomes;
            this.iconPath = iconPath;
        }
    }
}
