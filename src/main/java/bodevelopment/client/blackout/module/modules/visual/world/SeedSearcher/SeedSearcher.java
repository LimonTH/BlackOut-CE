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
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.world.LootSimulationEngine;
import bodevelopment.client.blackout.util.world.LootTableData;
import bodevelopment.client.blackout.util.world.SeedSourceUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

@Experimental
public class SeedSearcher extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgStructures = this.addGroup("Structures");
    private final SettingGroup sgLoot = this.addGroup("Loot");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Integer> renderDistance = sgGeneral.intSetting("Render Distance", 2000, 100, 20000, 100, "Max distance for 3D beam rendering.");
    private final Setting<Integer> recalcDistance = sgGeneral.intSetting("Recalc Distance", 512, 64, 2048, 64, "Distance the player must move before recalculating.");
    private final Setting<KeyBind> mapKey = sgGeneral.keySetting("Map Key", "Keybind to open the 2D seed map screen.");
    private final Setting<Boolean> lootSimulation = sgGeneral.booleanSetting("Loot Simulation", false, "Search for specific items in structure loot tables instead of finding structures.");
    private final Setting<String> seed = sgGeneral.stringSetting("Seed", "", "World seed for structure generation.").onChanged(v -> this.recalculate());
    private final Setting<Integer> searchRadius = sgGeneral.intSetting("Radius", 10000, 1000, 100000, 1000, "Search radius in blocks from the player.").onChanged(v -> this.recalculate());

    private final Setting<List<StructureType>> structures = sgStructures.listSetting("Worldgen Features", "Select which features to search for.", () -> !this.lootSimulation.get(), Arrays.asList(StructureType.values()), t -> t.displayName, StructureType.VILLAGE, StructureType.STRONGHOLD).onChanged(v -> this.recalculate());

    private final Setting<Boolean> egaple = sgLoot.booleanSetting("Enchanted Apple", true, "Search for Enchanted Golden Apples in structures. When enabled, overrides the item list.", () -> this.lootSimulation.get());
    private final Setting<List<StructureType>> lootStructures = sgLoot.listSetting("Loot Structures", "Select which structure types to search for loot.", () -> this.lootSimulation.get(), Arrays.asList(StructureType.values()), t -> t.displayName,
            StructureType.DESERT_PYRAMID, StructureType.DUNGEON, StructureType.MINESHAFT,
            StructureType.BASTION_REMNANT, StructureType.RUINED_PORTAL,
            StructureType.ANCIENT_CITY, StructureType.WOODLAND_MANSION
    ).onChanged(v -> this.recalculate());
    private final Setting<String> lootItems = sgLoot.stringSetting("Loot Items",
            "minecraft:diamond,minecraft:enchanted_golden_apple,minecraft:golden_apple",
            "Comma-separated item IDs to search for in structure loot. " +
                    "Only used when Enchanted Apple is OFF.", () -> this.lootSimulation.get() && !this.egaple.get());

    private final Setting<BlackOutColor> beamColor = sgRender.colorSetting("Beam Color", new BlackOutColor(255, 255, 50, 120), "Color for the structure beam.");
    private final Setting<BlackOutColor> textColor = sgRender.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), "Color for the structure label.");
    private final Setting<Double> textScale = sgRender.doubleSetting("Text Scale", 3.0, 0.5, 10.0, 0.5, "Scale of the structure label.");
    private final Setting<Integer> beamHeight = sgRender.intSetting("Beam Height", 256, 32, 512, 16, "Height of the structure beam.");
    private final Setting<Double> beamWidth = sgRender.doubleSetting("Beam Width", 0.5, 0.1, 2.0, 0.1, "Width of the structure beam.");

    private final List<FoundStructure> found = new CopyOnWriteArrayList<>();
    private CompletableFuture<Void> calcFuture = null;
    private double lastCalcX = Double.MAX_VALUE;
    private double lastCalcZ = Double.MAX_VALUE;
    private String lastSeed = "";
    private boolean mapKeyWasDown = false;
    private SeedSourceUtils biomeSource;
    private long currentSeed;
    private ResourceKey<Level> lastDimension;

    public SeedSearcher() {
        super("Seed Searcher", "Locates structures in a Minecraft world based on seed analysis.",
                SubCategory.WORLD, true);
    }

    public static void findInArea(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                  ResourceKey<Level> dim, int centerX, int centerZ, int radius, boolean limitRadius,
                                  Predicate<StructureType> enabledCheck) {
        StructureFinders.findInArea(results, worldSeed, source, dim, centerX, centerZ, radius,
                limitRadius, enabledCheck);
    }

    private static Set<String> parseItemList(String input) {
        if (input == null || input.isBlank()) return Set.of();
        Set<String> items = new HashSet<>();
        for (String part : input.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {

                if (!trimmed.contains(":")) {
                    trimmed = "minecraft:" + trimmed;
                }
                items.add(trimmed);
            }
        }
        return items;
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
        boolean hasBlockingSubScreen = isClickGuiOpen && clickGui.openedScreen != null
                && !(clickGui.openedScreen instanceof SeedMapScreen);
        boolean isTyping = SelectedComponent.isSelected();
        boolean canInteract = BlackOut.mc.screen == null
                || (isClickGuiOpen && !hasBlockingSubScreen && !isTyping);

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

                int structureY = s.blockY();
                boolean hasExactY = structureY != Integer.MIN_VALUE;
                double beamBottom = hasExactY ? structureY - 2.0 : -64.0;
                double beamTop = hasExactY ? structureY + height : (double) height;

                boolean hasLoot = !s.lootChestPositions().isEmpty();
                String extraInfo = s.extraInfo();
                if (!extraInfo.isEmpty() && !extraInfo.startsWith(" ")) {
                    extraInfo = " " + extraInfo;
                }

                String itemNames = "";
                if (!s.foundItems().isEmpty()) {
                    itemNames = " [";
                    boolean first = true;
                    for (String itemId : s.foundItems()) {
                        if (!first) itemNames += ",";
                        String shortName = itemId.replace("minecraft:", "");
                        itemNames += shortName;
                        first = false;
                    }
                    itemNames += "]";
                }

                if (!hasLoot) continue;
                Render3DUtils.box(new AABB(x - halfW, beamBottom, z - halfW,
                                x + halfW, beamTop, z + halfW),
                        new BlackOutColor(255, 215, 0, 200),
                        new BlackOutColor(255, 215, 0, 200), RenderShape.Full);
                for (int[] chestPos : s.lootChestPositions()) {
                    double cx = chestPos[0] + 0.5;
                    double cy = chestPos[1] + 0.5;
                    double cz = chestPos[2] + 0.5;
                    double cSize = 0.3;
                    AABB chestBox = new AABB(cx - cSize, cy - cSize, cz - cSize,
                            cx + cSize, cy + cSize, cz + cSize);
                    Render3DUtils.box(chestBox,
                            new BlackOutColor(255, 215, 0, 255),
                            new BlackOutColor(255, 255, 100, 255),
                            RenderShape.Full);
                }

                double dist = Math.sqrt(distSq);
                String yInfo = hasExactY ? " Y=" + structureY : "";
                Render3DUtils.text(s.type().displayName + extraInfo + itemNames + yInfo + " (" + (int) dist + "m)",
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

        boolean lootMode = this.lootSimulation.get();
        boolean appleOnly = lootMode && this.egaple.get();
        Set<String> targetItems = appleOnly
                ? Set.of("minecraft:enchanted_golden_apple")
                : parseItemList(this.lootItems.get());

        this.calcFuture = CompletableFuture.runAsync(() -> {
            List<FoundStructure> newFound = new ArrayList<>();

            if (lootMode) {
                Predicate<StructureType> enabledCheck = t -> this.lootStructures.get().contains(t);
                StructureFinders.findInArea(newFound, worldSeed, source, dim, startX, startZ,
                        radius, true, enabledCheck);

                for (int i = 0; i < newFound.size(); i++) {
                    FoundStructure s = newFound.get(i);
                    String tablePath = LootTableData.getLootTablePath(s.type());
                    if (tablePath == null) continue;

                    var result = LootSimulationEngine.simulateForItems(s, worldSeed, source, targetItems);
                    if (result.itemFound()) {
                        String extra = s.extraInfo();
                        String itemLabel = appleOnly ? "(Apple)" : "($" + result.foundItemIds().size() + ")";
                        extra = extra.isEmpty() ? itemLabel : extra + " " + itemLabel;
                        newFound.set(i, new FoundStructure(s.type(), s.blockX(), s.blockY(), s.blockZ(),
                                extra, result.chestPositions(), result.foundItemIds(),
                                result.allCheckedPositions()));
                    } else {
                        newFound.set(i, null);
                    }
                }

                newFound.removeIf(Objects::isNull);

            } else {
                Predicate<StructureType> enabledCheck = t -> this.isDimensionEnabled(t, dim)
                        && this.structures.get().contains(t);
                StructureFinders.findInArea(newFound, worldSeed, source, dim, startX, startZ,
                        radius, true, enabledCheck);
            }

            newFound.sort(Comparator.comparingDouble(s -> {
                double ddx = s.blockX() - startX;
                double ddz = s.blockZ() - startZ;
                return ddx * ddx + ddz * ddz;
            }));

            this.found.clear();
            this.found.addAll(newFound);
        });
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
        SeedMapScreen screen = new SeedMapScreen(this.seed.get(), worldSeed,
                this.getOrCreateBiomeSource(worldSeed));

        if (!clickGui.isOpen()) {
            clickGui.setOpen(true);
            clickGui.initGui();
            BlackOut.mc.setScreen(clickGui);
        }
        clickGui.setScreen(screen);
    }

    public boolean isDimensionEnabled(StructureType type, ResourceKey<Level> dim) {
        if (dim == null) return false;

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

    public boolean isStructureEnabled(StructureType type) {
        return this.structures.get().contains(type);
    }

    public List<FoundStructure> getFound() {
        return this.found;
    }

    public boolean isLootSimulationEnabled() {
        return this.lootSimulation.get();
    }

    public boolean isAppleOnly() {
        return this.lootSimulation.get() && this.egaple.get();
    }

    public boolean isLootStructureEnabled(StructureType type) {
        return this.lootStructures.get().contains(type);
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









}

