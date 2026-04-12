package bodevelopment.client.blackout.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.Map;

public final class BiomeColorMap {
    private static final Map<ResourceKey<Biome>, Integer> COLORS = new HashMap<>();
    private static final int DEFAULT_COLOR = 0xFF3B7A3B;
    private static final Map<ResourceKey<Biome>, Integer> HEIGHTS = new HashMap<>();
    private static final int DEFAULT_HEIGHT = 65;

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

    static {
        // Ocean
        putH(Biomes.OCEAN,                   38);
        putH(Biomes.DEEP_OCEAN,              32);
        putH(Biomes.COLD_OCEAN,              40);
        putH(Biomes.DEEP_COLD_OCEAN,         34);
        putH(Biomes.FROZEN_OCEAN,            42);
        putH(Biomes.DEEP_FROZEN_OCEAN,       36);
        putH(Biomes.LUKEWARM_OCEAN,          40);
        putH(Biomes.DEEP_LUKEWARM_OCEAN,     34);
        putH(Biomes.WARM_OCEAN,              42);
        // Beach / Shore
        putH(Biomes.BEACH,                   62);
        putH(Biomes.SNOWY_BEACH,             62);
        putH(Biomes.STONY_SHORE,             64);
        // River
        putH(Biomes.RIVER,                   58);
        putH(Biomes.FROZEN_RIVER,            58);
        // Plains / Meadow
        putH(Biomes.PLAINS,                  65);
        putH(Biomes.SUNFLOWER_PLAINS,        65);
        putH(Biomes.MEADOW,                  68);
        putH(Biomes.CHERRY_GROVE,            78);
        // Forest
        putH(Biomes.FOREST,                  70);
        putH(Biomes.FLOWER_FOREST,           70);
        putH(Biomes.BIRCH_FOREST,            72);
        putH(Biomes.OLD_GROWTH_BIRCH_FOREST, 74);
        putH(Biomes.DARK_FOREST,             70);
        putH(Biomes.PALE_GARDEN,             68);
        // Taiga
        putH(Biomes.TAIGA,                   75);
        putH(Biomes.SNOWY_TAIGA,             75);
        putH(Biomes.OLD_GROWTH_PINE_TAIGA,   80);
        putH(Biomes.OLD_GROWTH_SPRUCE_TAIGA, 82);
        // Desert
        putH(Biomes.DESERT,                  72);
        // Savanna
        putH(Biomes.SAVANNA,                 72);
        putH(Biomes.SAVANNA_PLATEAU,         95);
        putH(Biomes.WINDSWEPT_SAVANNA,      100);
        // Jungle
        putH(Biomes.JUNGLE,                  68);
        putH(Biomes.SPARSE_JUNGLE,           68);
        putH(Biomes.BAMBOO_JUNGLE,           68);
        // Badlands
        putH(Biomes.BADLANDS,                88);
        putH(Biomes.ERODED_BADLANDS,         95);
        putH(Biomes.WOODED_BADLANDS,         85);
        // Swamp
        putH(Biomes.SWAMP,                   60);
        putH(Biomes.MANGROVE_SWAMP,          60);
        // Mountain / Windswept
        putH(Biomes.WINDSWEPT_HILLS,        100);
        putH(Biomes.WINDSWEPT_GRAVELLY_HILLS,108);
        putH(Biomes.WINDSWEPT_FOREST,        97);
        putH(Biomes.STONY_PEAKS,            128);
        putH(Biomes.JAGGED_PEAKS,           148);
        putH(Biomes.FROZEN_PEAKS,           148);
        putH(Biomes.SNOWY_SLOPES,           118);
        putH(Biomes.GROVE,                  108);
        // Ice / Snow
        putH(Biomes.SNOWY_PLAINS,            65);
        putH(Biomes.ICE_SPIKES,              80);
        // Underground
        putH(Biomes.DRIPSTONE_CAVES,         64);
        putH(Biomes.LUSH_CAVES,              64);
        putH(Biomes.DEEP_DARK,               64);
        // Mushroom
        putH(Biomes.MUSHROOM_FIELDS,         68);
        // Nether
        putH(Biomes.NETHER_WASTES,           64);
        putH(Biomes.SOUL_SAND_VALLEY,        64);
        putH(Biomes.CRIMSON_FOREST,          64);
        putH(Biomes.WARPED_FOREST,           64);
        putH(Biomes.BASALT_DELTAS,           64);
        // End
        putH(Biomes.THE_END,                 45);
        putH(Biomes.END_HIGHLANDS,           80);
        putH(Biomes.END_MIDLANDS,            65);
        putH(Biomes.END_BARRENS,             45);
        putH(Biomes.SMALL_END_ISLANDS,       30);
        // The Void
        putH(Biomes.THE_VOID,                64);
    }

    private static void put(ResourceKey<Biome> key, int color) {
        COLORS.put(key, color);
    }

    private static void putH(ResourceKey<Biome> key, int height) {
        HEIGHTS.put(key, height);
    }

    public static int getColor(ResourceKey<Biome> biome) {
        return COLORS.getOrDefault(biome, DEFAULT_COLOR);
    }

    public static int getHeight(ResourceKey<Biome> biome) {
        return HEIGHTS.getOrDefault(biome, DEFAULT_HEIGHT);
    }

    /**
     * Returns color in low 32 bits and height in bits 32-63 — lets tile gen do
     * a single biome lookup instead of two separate ones.
     */
    public static long getSurfaceData(ResourceKey<Biome> biome) {
        return ((long) HEIGHTS.getOrDefault(biome, DEFAULT_HEIGHT) << 32)
                | (COLORS.getOrDefault(biome, DEFAULT_COLOR) & 0xFFFFFFFFL);
    }

    private BiomeColorMap() {}
}
