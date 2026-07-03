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

package bodevelopment.client.blackout.module.setting;

import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.function.Consumer;

@PublicAPI
public class Setting<T> {
    public final String name;
    public final String description;
    protected final T defaultValue;
    private final SingleOut<Boolean> visible;
    protected T value;
    protected float x = 0;
    protected float y = 0;
    protected double mx = 0.0;
    protected double my = 0.0;
    protected float width = 0.0F;
    protected float frameTime = 0.0F;
    private Consumer<T> changeListener = null;
    protected PoseStack stack = null;
    private SingleOut<Boolean> visibilityOverride = null;

    public Setting(String name, T val, String description, SingleOut<Boolean> visible) {
        this.name = name;
        this.description = description;
        this.visible = visible;
        this.defaultValue = val;
        this.value = val;
    }

    public float getWidth() {
        return this.width;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public T get() {
        return this.value;
    }

    protected void setValue(T value) {
        this.value = value;
        this.checkChange();
    }

    public boolean isVisible() {
        if (this.visibilityOverride != null) return this.visibilityOverride.get();
        return this.visible == null || this.visible.get();
    }

    public float onRender(PoseStack stack, float frameTime, float width, float x, float y, double mx, double my, boolean shouldRender) {
        this.stack = stack;
        this.frameTime = frameTime;
        this.x = x;
        this.y = y;
        this.mx = mx;
        this.my = my;
        this.width = width;
        return shouldRender ? this.render() : this.getHeight();
    }

    public float render() {
        return 0.0F;
    }

    public boolean onMouse(int button, boolean pressed) {
        return false;
    }

    public void onKey(int key, boolean pressed) {
    }

    public Setting<T> onChanged(Consumer<T> listener) {
        this.changeListener = listener;
        return this;
    }

    public void checkChange() {
        if (this.changeListener != null) {
            this.changeListener.accept(this.value);
        }
    }

    public float getHeight() {
        return 0.0F;
    }

    public void write(JsonObject object) {
    }

    public void read(JsonObject object) {
        if (!object.has(this.name)) {
            this.reset();
            Managers.CONFIG.saveAll();
        } else {
            try {
                this.set(object.get(this.name));
            } catch (Exception e) {
                this.reset();
                Managers.CONFIG.saveAll();
            }
        }
    }

    protected void set(JsonElement element) {
    }

    public void reset() {
        this.value = this.defaultValue;
    }

    public Setting<T> hide(T goodbyeValue) {
        this.value = goodbyeValue;
        this.visibilityOverride = () -> false;
        return this;
    }
}
