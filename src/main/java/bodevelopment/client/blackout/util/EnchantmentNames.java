package bodevelopment.client.blackout.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class EnchantmentNames {
    public static final List<ResourceKey<Enchantment>> enchantments = new ArrayList<>();
    private static final Map<ResourceKey<Enchantment>, String[]> map = new HashMap<>();

    public static String getName(Holder<Enchantment> enchantment, boolean shortName) {
        return enchantment.unwrapKey().map(key -> shortName ? getShortName(key) : getLongName(key)).orElse("Unknown");
    }

    public static String getLongName(ResourceKey<Enchantment> key) {
        String[] names = map.get(key);
        return names != null ? names[0] : key.location().getPath();
    }

    public static String getShortName(ResourceKey<Enchantment> key) {
        String[] names = map.get(key);
        return names != null ? names[1] : key.location().getPath().substring(0, Math.min(3, key.location().getPath().length()));
    }

    public static void init() {
        map.clear();
        enchantments.clear();

        Map<ResourceKey<Enchantment>, String> shortNames = getShortNames();

        shortNames.forEach((key, shortName) -> {
            String translationKey = "enchantment." + key.location().getNamespace() + "." + key.location().getPath();
            String longName = Component.translatable(translationKey).getString();

            map.put(key, new String[]{longName, shortName});
            enchantments.add(key);
        });
    }

    private static Map<ResourceKey<Enchantment>, String> getShortNames() {
        Map<ResourceKey<Enchantment>, String> map = new HashMap<>();
        put(map, Enchantments.PROTECTION, "prot");
        put(map, Enchantments.FIRE_PROTECTION, "fire");
        put(map, Enchantments.FEATHER_FALLING, "feat");
        put(map, Enchantments.BLAST_PROTECTION, "bla");
        put(map, Enchantments.PROJECTILE_PROTECTION, "proj");
        put(map, Enchantments.RESPIRATION, "resp");
        put(map, Enchantments.AQUA_AFFINITY, "aqua");
        put(map, Enchantments.THORNS, "tho");
        put(map, Enchantments.DEPTH_STRIDER, "dep");
        put(map, Enchantments.FROST_WALKER, "frost");
        put(map, Enchantments.BINDING_CURSE, "bind");
        put(map, Enchantments.SOUL_SPEED, "soul");
        put(map, Enchantments.SWIFT_SNEAK, "swi");
        put(map, Enchantments.SHARPNESS, "sha");
        put(map, Enchantments.SMITE, "smi");
        put(map, Enchantments.BANE_OF_ARTHROPODS, "bane");
        put(map, Enchantments.KNOCKBACK, "kno");
        put(map, Enchantments.FIRE_ASPECT, "asp");
        put(map, Enchantments.LOOTING, "loot");
        put(map, Enchantments.SWEEPING_EDGE, "swe");
        put(map, Enchantments.EFFICIENCY, "eff");
        put(map, Enchantments.SILK_TOUCH, "silk");
        put(map, Enchantments.UNBREAKING, "unb");
        put(map, Enchantments.FORTUNE, "for");
        put(map, Enchantments.POWER, "pow");
        put(map, Enchantments.PUNCH, "pun");
        put(map, Enchantments.FLAME, "fla");
        put(map, Enchantments.INFINITY, "inf");
        put(map, Enchantments.LUCK_OF_THE_SEA, "luck");
        put(map, Enchantments.LURE, "lure");
        put(map, Enchantments.LOYALTY, "loy");
        put(map, Enchantments.IMPALING, "imp");
        put(map, Enchantments.RIPTIDE, "rip");
        put(map, Enchantments.CHANNELING, "cha");
        put(map, Enchantments.MULTISHOT, "mul");
        put(map, Enchantments.QUICK_CHARGE, "qui");
        put(map, Enchantments.PIERCING, "pie");
        put(map, Enchantments.MENDING, "men");
        put(map, Enchantments.VANISHING_CURSE, "van");
        put(map, Enchantments.WIND_BURST, "wind");
        put(map, Enchantments.DENSITY, "dens");
        put(map, Enchantments.BREACH, "brea");

        return map;
    }

    private static void put(Map<ResourceKey<Enchantment>, String> map, ResourceKey<Enchantment> key, String name) {
        map.put(key, name);
    }
}