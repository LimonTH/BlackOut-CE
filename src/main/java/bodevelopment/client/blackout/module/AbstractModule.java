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

package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.WarningSettingGroup;
import com.google.gson.JsonObject;

import java.util.*;

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
@PublicAPI
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
