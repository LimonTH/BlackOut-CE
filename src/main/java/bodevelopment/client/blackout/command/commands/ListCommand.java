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

package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.ParentCategory;
import net.minecraft.ChatFormatting;

import java.util.*;

public class ListCommand extends Command {
    public ListCommand() {
        super("list", "Usage: list [category] [enabled]");
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }

    @Override
    public String execute(String[] args) {
        List<Module> modules = Managers.MODULES.getToggleableModules();
        boolean enabledOnly = false;
        String filterCategory = null;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("enabled")) {
                enabledOnly = true;
            } else {
                filterCategory = arg;
            }
        }

        Map<String, List<Module>> grouped = new LinkedHashMap<>();
        for (Module m : modules) {
            String catName = m.category.parent().name();
            if (filterCategory != null && !catName.equalsIgnoreCase(filterCategory)) continue;
            if (enabledOnly && !m.enabled) continue;
            grouped.computeIfAbsent(catName, k -> new ArrayList<>()).add(m);
        }

        if (grouped.isEmpty()) {
            return "No modules found.";
        }

        StringBuilder sb = new StringBuilder();
        for (var entry : grouped.entrySet()) {
            sb.append(ChatFormatting.AQUA).append("--- ").append(entry.getKey()).append(" ---\n");
            List<Module> mods = entry.getValue();
            mods.sort(Comparator.comparing(m -> m.name));
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                ChatFormatting clr = m.enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY;
                sb.append(clr).append(m.name);
                if (i < mods.size() - 1) sb.append(ChatFormatting.DARK_GRAY).append(", ");
            }
            sb.append("\n");
        }

        return sb.toString().stripTrailing();
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length <= 1) {
            for (ParentCategory cat : ParentCategory.categories) {
                suggestions.add(cat.name());
            }
            suggestions.add("enabled");
        } else if (args.length == 2) {
            suggestions.add("enabled");
        }
        return suggestions;
    }
}
