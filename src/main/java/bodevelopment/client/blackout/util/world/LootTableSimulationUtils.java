package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.FoundStructure;
import bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType;

import java.util.List;
import java.util.Set;

@PublicAPI
@Deprecated
public final class LootTableSimulationUtils {
    private LootTableSimulationUtils() {
    }

    @PublicAPI
    @Deprecated
    public static boolean canHaveEnchantedApple(StructureType type) {
        return LootTableData.canHaveEnchantedApple(type);
    }

    @PublicAPI
    @Deprecated
    public static int getDefaultY(StructureType type) {
        return switch (type) {
            case DUNGEON -> -40;
            case ANCIENT_CITY -> -44;
            case BASTION_REMNANT -> 40;
            case DESERT_PYRAMID -> 53;
            case RUINED_PORTAL -> 64;
            case WOODLAND_MANSION -> 72;
            case TRIAL_CHAMBERS -> -20;
            case STRONGHOLD -> -40;
            case MINESHAFT -> -30;
            case SHIPWRECK -> 38;
            case OCEAN_RUIN -> 38;
            default -> 64;
        };
    }

    @PublicAPI
    @Deprecated
    public static List<int[]> simulateApples(FoundStructure structure, long worldSeed,
                                             SeedSourceUtils source) {
        var result = LootSimulationEngine.simulateForApples(structure, worldSeed, source);
        return result.chestPositions();
    }

    @PublicAPI
    @Deprecated
    public static List<int[]> simulateItems(FoundStructure structure, long worldSeed,
                                            SeedSourceUtils source, Set<String> targetItems) {
        var result = LootSimulationEngine.simulateForItems(structure, worldSeed, source, targetItems);
        return result.chestPositions();
    }

    @PublicAPI
    @Deprecated
    public static Set<String> simulateSingleChest(StructureType type, long worldSeed,
                                                  int chestX, int chestY, int chestZ) {
        return LootSimulationEngine.simulateSingleChest(type, worldSeed, chestX, chestY, chestZ);
    }

    @PublicAPI
    @Deprecated
    public static double getAppleProbability(StructureType type) {
        return switch (type) {
            case DESERT_PYRAMID -> computeProbability(1, 391, 4 * avg(2, 4));
            case DUNGEON -> computeProbability(1, 127, 2 * avg(1, 3));
            case MINESHAFT -> computeProbability(1, 155, 3 * avg(1, 3));
            case BASTION_REMNANT -> computeProbability(1, 87, 2 * avg(2, 6));
            case RUINED_PORTAL -> computeProbability(1, 398, 1 * avg(4, 8));
            case ANCIENT_CITY -> computeProbability(1, 302, 6 * avg(5, 10));
            case WOODLAND_MANSION -> computeProbability(1, 146, 4 * avg(1, 3));
            default -> 0.0;
        };
    }

    private static double computeProbability(int appleWeight, int totalWeight, double totalRolls) {
        double p = (double) appleWeight / totalWeight;
        return 1.0 - Math.pow(1.0 - p, totalRolls);
    }

    private static double avg(int a, int b) {
        return (a + b) / 2.0;
    }
}

