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
import bodevelopment.client.blackout.gui.clickgui.screens.ListScreen;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.GuiColorUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import bodevelopment.client.blackout.enums.RenderShape;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class ListSetting<T> extends Setting<List<T>> {
    public final List<T> list;
    private final EpicInterface<T, String> getName;

    /** Per-item extra data (e.g., Color per block). Keyed by item identity via getName. */
    private final Map<String, Map<String, Object>> itemData = new HashMap<>();
    /** Snapshot of the initial itemData taken after construction, used to restore defaults on reset. */
    private Map<String, Map<String, Object>> defaultItemData;
    /** Whether this list supports per-item colors in the ListScreen UI. */
    private boolean supportsItemColors = false;
    /** Live supplier for the default line color — tied to the module's ColorSetting. */
    private Supplier<Color> defaultLineColorSupplier = () -> new Color(255, 255, 255, 180);
    /** Live supplier for the default side color — tied to the module's ColorSetting. */
    private Supplier<Color> defaultSideColorSupplier = () -> new Color(255, 255, 255, 50);
    /** Supplier for RenderShape to synchronize shown rects with BoxMultiSetting shape. */
    private Supplier<RenderShape> shapeSupplier;

    @SafeVarargs
    public ListSetting(String name, List<T> list, EpicInterface<T, String> getName, String description, SingleOut<Boolean> visible, T... val) {
        super(name, new ArrayList<>(Arrays.asList(val)), description, visible);
        this.value = new ArrayList<>(this.defaultValue); // Ensure value is a separate copy to prevent in-place mutation of defaultValue
        this.list = list;
        this.getName = getName;
    }

    /** Enables per-item color support with static default colors (snapshots — for backward compat). */
    public ListSetting<T> withItemColors(Color defaultLineColor, Color defaultSideColor) {
        return withItemColors(defaultLineColor, defaultSideColor, null);
    }

    /** Enables per-item color support with static default colors and shape-aware rect visibility. */
    public ListSetting<T> withItemColors(Color defaultLineColor, Color defaultSideColor, Supplier<RenderShape> shapeSupplier) {
        return withItemColors(
                defaultLineColor != null ? () -> defaultLineColor : null,
                defaultSideColor != null ? () -> defaultSideColor : null,
                shapeSupplier
        );
    }

    /**
     * Enables per-item color support with live color suppliers tied to the module's ColorSetting.
     * When the module's color changes, items without a custom per-item color automatically reflect it.
     */
    public ListSetting<T> withItemColors(Supplier<Color> defaultLineColorSupplier, Supplier<Color> defaultSideColorSupplier) {
        return withItemColors(defaultLineColorSupplier, defaultSideColorSupplier, null);
    }

    /**
     * Enables per-item color support with live color suppliers and shape-aware rect visibility.
     */
    public ListSetting<T> withItemColors(Supplier<Color> defaultLineColorSupplier, Supplier<Color> defaultSideColorSupplier, Supplier<RenderShape> shapeSupplier) {
        this.supportsItemColors = true;
        if (defaultLineColorSupplier != null) this.defaultLineColorSupplier = defaultLineColorSupplier;
        if (defaultSideColorSupplier != null) this.defaultSideColorSupplier = defaultSideColorSupplier;
        this.shapeSupplier = shapeSupplier;
        return this;
    }

    public Supplier<RenderShape> getShapeSupplier() {
        return this.shapeSupplier;
    }

    public boolean supportsItemColors() {
        return this.supportsItemColors;
    }

    public Color getDefaultLineColor() {
        return this.defaultLineColorSupplier != null ? this.defaultLineColorSupplier.get() : null;
    }

    public Color getDefaultSideColor() {
        return this.defaultSideColorSupplier != null ? this.defaultSideColorSupplier.get() : null;
    }

    // ========================================================================
    // Per-item Data API
    // ========================================================================

    /** Sets a per-item data value (e.g., Color for a block). */
    public ListSetting<T> withItemData(T item, String key, Object value) {
        String id = this.getName.get(item);
        this.itemData.computeIfAbsent(id, k -> new HashMap<>()).put(key, value);
        return this;
    }

    /** Gets a per-item data value, or null if not set. */
    @SuppressWarnings("unchecked")
    public <V> V getItemData(T item, String key) {
        Map<String, Object> data = this.itemData.get(this.getName.get(item));
        return data != null ? (V) data.get(key) : null;
    }

    /** Returns the item data map for rendering in ListScreen. */
    public Map<String, Map<String, Object>> getItemData() {
        return this.itemData;
    }

    /** Gets the display name for an item. */
    public String getItemName(T item) {
        return this.getName.get(item);
    }

    // ========================================================================
    // Rendering
    // ========================================================================

    @Override
    public float render() {
        float textScale = 2.0F;
        float baseH = 26.0F;
        float middleY = this.y + (baseH / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, middleY, GuiColorUtils.getSettingText(this.y), false, true);

        String text = String.valueOf(this.get().size());

        float valueX = this.x + this.width - 10.0F;

        BlackOut.FONT.text(
                this.stack,
                text,
                textScale,
                valueX - (BlackOut.FONT.getWidth(text) * textScale),
                middleY,
                GuiColorUtils.getSettingText(this.y),
                false,
                true
        );

        return this.getHeight();
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        float clickOffset = -5.5F;

        if (key == 0 && pressed && this.mx > this.x && this.mx < this.x + this.width
                && this.my > this.y + clickOffset && this.my < this.y + this.getHeight() + clickOffset) {

            Managers.CLICK_GUI.openScreen(new ListScreen<>(this, this.getName));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public float getHeight() {
        return 26.0F;
    }

    // ========================================================================
    // Serialization
    // ========================================================================

    protected String join() {
        StringBuilder builder = new StringBuilder();
        boolean b = false;
        for (T item : this.get()) {
            if (!b) {
                b = true;
            } else {
                builder.append(",");
            }
            builder.append(this.getName.get(item));
        }
        return builder.toString();
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.name, this.join());

        // Write per-item data (only when colors are enabled)
        if (this.supportsItemColors && !this.itemData.isEmpty()) {
            JsonObject dataObj = new JsonObject();
            for (Map.Entry<String, Map<String, Object>> entry : this.itemData.entrySet()) {
                JsonObject perItem = new JsonObject();
                for (Map.Entry<String, Object> kv : entry.getValue().entrySet()) {
                    if (kv.getValue() instanceof Color c) {
                        perItem.addProperty(kv.getKey(), c.getRGB());
                    } else if (kv.getValue() instanceof Number n) {
                        perItem.addProperty(kv.getKey(), n);
                    } else {
                        perItem.addProperty(kv.getKey(), String.valueOf(kv.getValue()));
                    }
                }
                dataObj.add(entry.getKey(), perItem);
            }
            jsonObject.add(this.name + "_data", dataObj);
        }
    }

    /**
     * Overrides the base read to also restore per-item data (colors) from the module JSON.
     * The base {@link Setting#read(JsonObject)} only loads the list values — the per-item
     * colors stored under {@code this.name + "_data"} need a separate restore step.
     */
    @Override
    public void read(JsonObject object) {
        super.read(object);
        if (this.supportsItemColors) {
            this.restoreData(object);
        }
    }

    @Override
    public void set(JsonElement element) {
        // Build a new list instead of mutating in-place to avoid corrupting
        // defaultValue (which shares the same object reference via Setting constructor).
        List<T> newList = new ArrayList<>();
        Map<String, T> names = new HashMap<>();
        this.list.forEach(item -> names.put(this.getName.get(item), item));

        for (String string : element.getAsString().split(",")) {
            if (names.containsKey(string)) {
                newList.add(names.get(string));
            }
        }
        this.value = newList;
        this.checkChange();
    }

    @Override
    public void reset() {
        // Create a new list copy because Setting constructor shares value/defaultValue reference
        this.value = new ArrayList<>(this.defaultValue);
        if (this.supportsItemColors) {
            this.itemData.clear();
            // Restore from the snapshot taken after construction, if available
            if (this.defaultItemData != null) {
                for (Map.Entry<String, Map<String, Object>> entry : this.defaultItemData.entrySet()) {
                    this.itemData.put(entry.getKey(), new HashMap<>(entry.getValue()));
                }
            }
        }
    }

    /**
     * Must be called after all withItemData() calls are complete (e.g., at the end of module
     * setting initialization) to capture the initial itemData as the baseline for reset().
     */
    public ListSetting<T> snapshotDefaults() {
        if (this.supportsItemColors) {
            this.defaultItemData = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : this.itemData.entrySet()) {
                this.defaultItemData.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }
        return this;
    }

    /** Called after the full config JSON is loaded, to restore item data. */
    public void restoreData(JsonObject fullJson) {
        if (!fullJson.has(this.name + "_data")) return;
        JsonObject dataObj = fullJson.getAsJsonObject(this.name + "_data");
        for (Map.Entry<String, JsonElement> entry : dataObj.entrySet()) {
            String itemId = entry.getKey();
            JsonObject perItem = entry.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> kv : perItem.entrySet()) {
                JsonElement val = kv.getValue();
                if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isNumber()) {
                    this.itemData.computeIfAbsent(itemId, k -> new HashMap<>())
                            .put(kv.getKey(), new Color(val.getAsInt(), true));
                } else {
                    this.itemData.computeIfAbsent(itemId, k -> new HashMap<>())
                            .put(kv.getKey(), val.getAsString());
                }
            }
        }
    }
}
