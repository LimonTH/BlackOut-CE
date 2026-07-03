/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.util.GuiColorUtils;
import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

public class RegistrySetting<T> extends ListSetting<T> {
    private static final Map<Registry<?>, List<?>> registries = new HashMap<>();
    private final Registry<T> registry;

    @SafeVarargs
    public RegistrySetting(String name, Registry<T> registry, EpicInterface<T, String> getName, String description, SingleOut<Boolean> visible, Predicate<T> filter, T... val) {
        super(name, getFilteredList(registry, filter), getName, description, visible, val);
        this.registry = registry;
    }

    @SafeVarargs
    public RegistrySetting(String name, Registry<T> registry, EpicInterface<T, String> getName, String description, SingleOut<Boolean> visible, T... val) {
        super(name, getList(registry), getName, description, visible, val);
        this.registry = registry;
    }

    private static <T> List<T> getFilteredList(Registry<T> registry, Predicate<T> filter) {
        return registry.entrySet().stream()
                .map(Entry::getValue)
                .filter(filter)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(Registry<T> registry) {
        if (registries.containsKey(registry)) {
            return (List<T>) registries.get(registry);
        } else {
            List<T> list = new ArrayList<>(registry.entrySet().stream().map(Entry::getValue).toList());
            registries.put(registry, list);
            return list;
        }
    }

    @Override
    public float render() {
        float textScale = 2.0F;
        float baseH = 26.0F;
        float middleY = this.y + (baseH / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, middleY, GuiColorUtils.getSettingText(this.y), false, true);

        String text = String.valueOf(this.get().size());

        float valueX = this.x + this.width - 10.0F;
        float textWidth = BlackOut.FONT.getWidth(text) * textScale;

        BlackOut.FONT.text(
                this.stack,
                text,
                textScale,
                valueX - textWidth,
                middleY,
                GuiColorUtils.getSettingText(this.y),
                false,
                true
        );

        return this.getHeight();
    }

    @Override
    protected String join() {
        StringBuilder builder = new StringBuilder();
        boolean b = false;

        for (T item : this.get()) {
            if (!b) {
                b = true;
            } else {
                builder.append(",");
            }

            builder.append(this.registry.getKey(item));
        }

        return builder.toString();
    }

    @Override
    public void set(JsonElement element) {
        this.get().clear();

        for (String string : element.getAsString().split(",")) {
            ResourceLocation id = ResourceLocation.parse(string);
            if (this.registry.containsKey(id)) {
                this.get().add(this.registry.getValue(id));
            }
        }
        this.checkChange();
    }

    public Registry<T> getRegistry() {
        return this.registry;
    }
}
