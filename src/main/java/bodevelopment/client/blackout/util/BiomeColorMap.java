package bodevelopment.client.blackout.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;

public final class BiomeColorMap {
    private static final Map<ResourceKey<Biome>, Integer> COLORS = new HashMap<>();
    private static final int DEFAULT_COLOR = 0xFF3B7A3B;

    static {
        // Ocean
        put(Biomes.OCEAN, 0xFF000070);
        put(Biomes.DEEP_OCEAN, 0xFF000030);
        put(Biomes.COLD_OCEAN, 0xFF2D2D8A);
        put(Biomes.DEEP_COLD_OCEAN, 0xFF1A1A5E);
        put(Biomes.FROZEN_OCEAN, 0xFF7070D6);
        put(Biomes.DEEP_FROZEN_OCEAN, 0xFF4040A0);
        put(Biomes.LUKEWARM_OCEAN, 0xFF000090);
        put(Biomes.DEEP_LUKEWARM_OCEAN, 0xFF000050);
        put(Biomes.WARM_OCEAN, 0xFF0000AC);

        // Beach / Shore
        put(Biomes.BEACH, 0xFFFADE55);
        put(Biomes.SNOWY_BEACH, 0xFFFAF0C0);
        put(Biomes.STONY_SHORE, 0xFFA2A284);

        // River
        put(Biomes.RIVER, 0xFF0000FF);
        put(Biomes.FROZEN_RIVER, 0xFFA0A0FF);

        // Plains / Meadow
        put(Biomes.PLAINS, 0xFF8DB360);
        put(Biomes.SUNFLOWER_PLAINS, 0xFFB5DB88);
        put(Biomes.MEADOW, 0xFF75B554);
        put(Biomes.CHERRY_GROVE, 0xFFE9A3BD);

        // Forest
        put(Biomes.FOREST, 0xFF056621);
        put(Biomes.FLOWER_FOREST, 0xFF2D8E49);
        put(Biomes.BIRCH_FOREST, 0xFF307444);
        put(Biomes.OLD_GROWTH_BIRCH_FOREST, 0xFF3A7A4A);
        put(Biomes.DARK_FOREST, 0xFF40511A);
        put(Biomes.PALE_GARDEN, 0xFFB5C4AD);

        // Taiga
        put(Biomes.TAIGA, 0xFF0B6659);
        put(Biomes.SNOWY_TAIGA, 0xFF31554A);
        put(Biomes.OLD_GROWTH_PINE_TAIGA, 0xFF596651);
        put(Biomes.OLD_GROWTH_SPRUCE_TAIGA, 0xFF687942);

        // Desert
        put(Biomes.DESERT, 0xFFFA9418);

        // Savanna
        put(Biomes.SAVANNA, 0xFFBDB25F);
        put(Biomes.SAVANNA_PLATEAU, 0xFFA79D64);
        put(Biomes.WINDSWEPT_SAVANNA, 0xFFE5DA87);

        // Jungle
        put(Biomes.JUNGLE, 0xFF537B09);
        put(Biomes.SPARSE_JUNGLE, 0xFF628B17);
        put(Biomes.BAMBOO_JUNGLE, 0xFF768E14);

        // Badlands
        put(Biomes.BADLANDS, 0xFFD94515);
        put(Biomes.ERODED_BADLANDS, 0xFFFF6D3D);
        put(Biomes.WOODED_BADLANDS, 0xFFB09765);

        // Swamp
        put(Biomes.SWAMP, 0xFF07F9B2);
        put(Biomes.MANGROVE_SWAMP, 0xFF67A23F);

        // Mountain / Windswept
        put(Biomes.WINDSWEPT_HILLS, 0xFF606060);
        put(Biomes.WINDSWEPT_GRAVELLY_HILLS, 0xFF808080);
        put(Biomes.WINDSWEPT_FOREST, 0xFF589C6C);
        put(Biomes.STONY_PEAKS, 0xFF848E8E);
        put(Biomes.JAGGED_PEAKS, 0xFFD2D7D7);
        put(Biomes.FROZEN_PEAKS, 0xFFE0E8EC);
        put(Biomes.SNOWY_SLOPES, 0xFFCDD8D8);
        put(Biomes.GROVE, 0xFF88A488);

        // Ice / Snow
        put(Biomes.SNOWY_PLAINS, 0xFFFFFFFF);
        put(Biomes.ICE_SPIKES, 0xFFB4DCDC);

        // Underground Biomes markers
        put(Biomes.DRIPSTONE_CAVES, 0xFF886644);
        put(Biomes.LUSH_CAVES, 0xFF2C6E1E);
        put(Biomes.DEEP_DARK, 0xFF0B1520);

        // Mushroom
        put(Biomes.MUSHROOM_FIELDS, 0xFFFF00FF);

        // Nether
        put(Biomes.NETHER_WASTES, 0xFFBF3B3B);
        put(Biomes.SOUL_SAND_VALLEY, 0xFF5E3830);
        put(Biomes.CRIMSON_FOREST, 0xFFDD0808);
        put(Biomes.WARPED_FOREST, 0xFF49907B);
        put(Biomes.BASALT_DELTAS, 0xFF403636);

        // End
        put(Biomes.THE_END, 0xFFDBD3A0);
        put(Biomes.END_HIGHLANDS, 0xFFE2DAA8);
        put(Biomes.END_MIDLANDS, 0xFFD4CC98);
        put(Biomes.END_BARRENS, 0xFF0C0822);
        put(Biomes.SMALL_END_ISLANDS, 0xFF0A0620);

        // The Void
        put(Biomes.THE_VOID, 0xFF000000);
    }

    private static void put(ResourceKey<Biome> key, int color) {
        COLORS.put(key, color);
    }

    public static int getColor(ResourceKey<Biome> biome) {
        return COLORS.getOrDefault(biome, DEFAULT_COLOR);
    }

    private BiomeColorMap() {}
}
