package bodevelopment.client.blackout.module.setting;

import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class SettingGroup {
    public final String name;
    public final List<Setting<?>> settings = new ArrayList<>();

    public SettingGroup(String name) {
        this.name = name;
    }

    public Setting<Boolean> booleanSetting(String name, boolean value, String description, SingleOut<Boolean> visible) {
        return this.addSetting(Settings.booleanSetting(name, value, description, visible));
    }

    public Setting<Double> doubleSetting(String name, double value, double min, double max, double step, String description, SingleOut<Boolean> visible) {
        return this.addSetting(Settings.doubleSetting(name, value, min, max, step, description, visible));
    }

    public <T extends Enum<?>> Setting<T> enumSetting(String name, T value, String description, SingleOut<Boolean> visible) {
        return this.addSetting(Settings.enumSetting(name, value, description, visible));
    }

    public Setting<Integer> intSetting(String name, int value, int min, int max, int step, String description, SingleOut<Boolean> visible) {
        return this.addSetting(Settings.intSetting(name, value, min, max, step, description, visible));
    }

    public Setting<KeyBind> keySetting(String name, String description, SingleOut<Boolean> visible) {
        return this.addSetting(Settings.keySetting(name, description, visible));
    }

    public Setting<String> stringSetting(String name, String value, String description, SingleOut<Boolean> visible) {
        return this.addSetting(Settings.stringSetting(name, value, description, visible));
    }

    public Setting<BlackOutColor> colorSetting(String name, BlackOutColor value, String description, SingleOut<Boolean> visible) {
        return this.addSetting(Settings.colorSetting(name, value, description, visible));
    }

    public Setting<List<Block>> blockListSetting(String name, String description, SingleOut<Boolean> visible, Block... value) {
        return this.addSetting(Settings.blockListSetting(name, description, visible, value));
    }

    public Setting<List<Item>> itemListSetting(String name, String description, SingleOut<Boolean> visible, Item... value) {
        return this.addSetting(Settings.itemListSetting(name, description, visible, value));
    }

    public Setting<List<EntityType<?>>> entityListSetting(String name, String description, SingleOut<Boolean> visible, EntityType<?>... value) {
        return this.addSetting(Settings.entityListSetting(name, description, visible, value));
    }

    @SafeVarargs
    public final <T> Setting<List<T>> registrySetting(
            String name, String description, SingleOut<Boolean> visible, Registry<T> registry, EpicInterface<T, String> getName, T... value
    ) {
        return this.addSetting(Settings.registrySetting(name, description, visible, registry, getName, value));
    }

    @SafeVarargs
    public final <T> Setting<List<T>> listSetting(String name, String description, SingleOut<Boolean> visible, List<T> list, EpicInterface<T, String> getName, T... value) {
        return this.addSetting(Settings.listSetting(name, description, visible, list, getName, value));
    }

    public Setting<Boolean> booleanSetting(String name, boolean value, String description) {
        return this.addSetting(Settings.booleanSetting(name, value, description, null));
    }

    public Setting<Double> doubleSetting(String name, double value, double min, double max, double step, String description) {
        return this.addSetting(Settings.doubleSetting(name, value, min, max, step, description, null));
    }

    public <T extends Enum<?>> Setting<T> enumSetting(String name, T value, String description) {
        return this.addSetting(Settings.enumSetting(name, value, description, null));
    }

    public Setting<Integer> intSetting(String name, int value, int min, int max, int step, String description) {
        return this.addSetting(Settings.intSetting(name, value, min, max, step, description, null));
    }

    public Setting<KeyBind> keySetting(String name, String description) {
        return this.addSetting(Settings.keySetting(name, description, null));
    }

    public Setting<String> stringSetting(String name, String value, String description) {
        return this.addSetting(Settings.stringSetting(name, value, description, null));
    }

    public Setting<BlackOutColor> colorSetting(String name, BlackOutColor value, String description) {
        return this.addSetting(Settings.colorSetting(name, value, description, null));
    }

    public Setting<List<Block>> blockListSetting(String name, String description, Block... value) {
        return this.addSetting(Settings.blockListSetting(name, description, null, value));
    }

    public Setting<List<Item>> itemListSetting(String name, String description, Item... value) {
        return this.addSetting(Settings.itemListSetting(name, description, null, value));
    }

    public Setting<List<EntityType<?>>> entityListSetting(String name, String description, EntityType<?>... value) {
        return this.addSetting(Settings.entityListSetting(name, description, null, value));
    }

    public Setting<List<EntityType<?>>> entityFilterdListSetting(String name, String description, java.util.function.Predicate<EntityType<?>> filter, EntityType<?>... value) {
        return this.addSetting(Settings.entityFilterdListSetting(name, description, null, filter, value));
    }

    @SafeVarargs
    public final <T> Setting<List<T>> registrySetting(String name, String description, Registry<T> registry, EpicInterface<T, String> getName, T... value) {
        return this.addSetting(Settings.registrySetting(name, description, null, registry, getName, value));
    }

    @SafeVarargs
    public final <T> Setting<List<T>> listSetting(String name, String description, List<T> list, EpicInterface<T, String> getName, T... value) {
        return this.addSetting(Settings.listSetting(name, description, null, list, getName, value));
    }

    private <T> Setting<T> addSetting(Setting<T> setting) {
        this.settings.add(setting);
        return setting;
    }
}
