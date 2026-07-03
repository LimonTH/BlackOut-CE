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

package bodevelopment.client.blackout.randomstuff;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Foundation for JSON-configurable anti-cheat bypass profiles (MISS-01).
 * <p>
 * Allows module behavior to be tuned per anti-cheat (Grim, Vulcan, Verus, etc.)
 * without recompilation. Profiles are stored as JSON files in
 * {@code blackout/bypass_profiles/} and loaded at startup.
 * <p>
 * <b>Current status:</b> Data structure defined. Full integration with
 * module settings and GUI editor is future work.
 */
public class BypassProfile {
    private final String name;
    private final Map<String, String> settings = new HashMap<>();

    public BypassProfile(String name) {
        this.name = name;
    }

    public static BypassProfile fromJson(JsonObject obj) {
        BypassProfile profile = new BypassProfile(obj.get("name").getAsString());
        if (obj.has("settings")) {
            JsonObject settingsObj = obj.getAsJsonObject("settings");
            settingsObj.entrySet().forEach(e ->
                    profile.set(e.getKey(), e.getValue().getAsString()));
        }
        return profile;
    }

    public String getName() {
        return name;
    }

    public void set(String key, String value) {
        settings.put(key, value);
    }

    public String get(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        JsonObject settingsObj = new JsonObject();
        settings.forEach(settingsObj::addProperty);
        obj.add("settings", settingsObj);
        return obj;
    }
}
