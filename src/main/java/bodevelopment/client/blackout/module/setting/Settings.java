package bodevelopment.client.blackout.module.setting;

import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.module.setting.settings.*;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class Settings {
    public static Setting<Boolean> booleanSetting(String name, boolean value, String description, SingleOut<Boolean> visible) {
        return new BoolSetting(name, value, description, visible);
    }

    public static Setting<Double> doubleSetting(String name, double value, double min, double max, double step, String description, SingleOut<Boolean> visible) {
        return new DoubleSetting(name, value, min, max, step, description, visible);
    }

    public static <T extends Enum<?>> Setting<T> enumSetting(String name, T value, String description, SingleOut<Boolean> visible) {
        return new EnumSetting<>(name, value, description, visible);
    }

    public static Setting<Integer> intSetting(String name, int value, int min, int max, int step, String description, SingleOut<Boolean> visible) {
        return new IntSetting(name, value, min, max, step, description, visible);
    }

    public static Setting<KeyBind> keySetting(String name, String description, SingleOut<Boolean> visible) {
        return new KeyBindSetting(name, description, visible);
    }

    public static Setting<String> stringSetting(String name, String value, String description, SingleOut<Boolean> visible) {
        return new StringSetting(name, value, description, visible);
    }

    public static Setting<BlackOutColor> colorSetting(String name, BlackOutColor value, String description, SingleOut<Boolean> visible) {
        return new ColorSetting(name, value, description, visible);
    }

    public static Setting<List<Block>> blockListSetting(String name, String description, SingleOut<Boolean> visible, Block... value) {
        return new RegistrySetting<>(name, BuiltInRegistries.BLOCK, block -> block.getName().getString(), description, visible, value);
    }

    public static Setting<List<Item>> itemListSetting(String name, String description, SingleOut<Boolean> visible, Item... value) {
        return registrySetting(name, description, visible, BuiltInRegistries.ITEM, item -> item.getName().getString(), value);
    }

    public static Setting<List<Item>> itemFilterdListSetting(String name, String description, SingleOut<Boolean> visible, Predicate<Item> filter, Item... value) {
        return filteredRegistrySetting(name, description, visible, BuiltInRegistries.ITEM, entity -> entity.getName().getString(), filter, value);
    }

    public static Setting<List<EntityType<?>>> entityListSetting(String name, String description, SingleOut<Boolean> visible, EntityType<?>... value) {
        return registrySetting(name, description, visible, BuiltInRegistries.ENTITY_TYPE, entity -> entity.getDescription().getString(), value);
    }

    public static Setting<List<EntityType<?>>> entityFilterdListSetting(String name, String description, SingleOut<Boolean> visible, Predicate<EntityType<?>> filter, EntityType<?>... value) {
        return filteredRegistrySetting(name, description, visible, BuiltInRegistries.ENTITY_TYPE, entity -> entity.getDescription().getString(), filter, value);
    }

    @SafeVarargs
    public static <T> Setting<List<T>> registrySetting(
            String name, String description, SingleOut<Boolean> visible, Registry<T> registry, EpicInterface<T, String> getName, T... value) {
        return new RegistrySetting<>(name, registry, getName, description, visible, value);
    }

    @SafeVarargs
    public static <T> Setting<List<T>> filteredRegistrySetting(
            String name, String description, SingleOut<Boolean> visible, Registry<T> registry, EpicInterface<T, String> getName, Predicate<T> filter, T... value) {
        return new RegistrySetting<T>(name, registry, getName, description, visible, filter, value);
    }

    @SafeVarargs
    public static <T> Setting<List<T>> listSetting(String name, String description, SingleOut<Boolean> visible, List<T> list, EpicInterface<T, String> getName, T... value) {
        return new ListSetting<>(name, list, getName, description, visible, value);
    }
}
