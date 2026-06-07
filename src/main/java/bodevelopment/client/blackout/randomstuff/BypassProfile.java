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
