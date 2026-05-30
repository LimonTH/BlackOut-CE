package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.WarningSettingGroup;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Base class for all module types. Provides shared infrastructure:
 * name, description, category, settings groups, display name, and serialization.
 * <p>
 * Subclasses:
 * <ul>
 *   <li>{@link Module} — toggleable modules with keybinds, rotation, and combat helpers</li>
 *   <li>{@link SettingsModule} — non-toggleable global settings panels</li>
 * </ul>
 */
public abstract class AbstractModule {
    public final String name;
    public final String description;
    public final SubCategory category;
    public final Set<String> tags = new LinkedHashSet<>();
    public final List<SettingGroup> settingGroups = new ArrayList<>();
    public final SettingGroup sgModule = this.addGroup("Module");

    private final Setting<String> displayName;

    /**
     * @param name        The internal name of the module.
     * @param description A brief explanation of what the module does.
     * @param category    The sub-category for GUI organization.
     */
    public AbstractModule(String name, String description, SubCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.displayName = this.sgModule.stringSetting("Name", name, "The internal name used for this module in the interface.");
    }

    /**
     * Whether this module can be toggled on/off by the user.
     */
    public boolean toggleable() {
        return false;
    }

    public String getFileName() {
        return this.name.replaceAll(" ", "");
    }

    public String getDisplayName() {
        String dn = this.displayName.get();
        return dn.isEmpty() ? this.name : dn;
    }

    public String getInfo() {
        return null;
    }

    protected SettingGroup addGroup(String name) {
        SettingGroup group = new SettingGroup(name);
        this.settingGroups.add(group);
        return group;
    }

    protected SettingGroup addGroup(String name, String warning) {
        SettingGroup group = new WarningSettingGroup(name, warning);
        this.settingGroups.add(group);
        return group;
    }

    public void readSettings(JsonObject jsonObject) {
        this.settingGroups.forEach(group -> group.settings.forEach(s -> s.read(jsonObject)));
    }

    public void writeSettings(JsonObject jsonObject) {
        this.settingGroups.forEach(group -> group.settings.forEach(s -> s.write(jsonObject)));
    }

    /**
     * Resets all settings in this module to their default values
     * and persists the changes to config.
     */
    public void resetToDefaults() {
        this.settingGroups.forEach(group -> group.settings.forEach(Setting::reset));
    }

    /**
     * If true, event listeners for this module are skipped.
     */
    public boolean shouldSkipListeners() {
        return true;
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof AbstractModule module && module.name.equals(this.name);
    }

    /**
     * File-name based identity: two modules with the same name are considered equal.
     * This is intentional — module identity is name-based, not class-based.
     * A hypothetical addon module named "Manager" would conflict with the built-in
     * Manager module, which is the desired behavior (no duplicate module names).
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
