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

package bodevelopment.client.blackout.command;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.util.StringUtils;
import net.minecraft.ChatFormatting;

public class ToggleCommand extends Command {
    private final String lowerCase;
    private final String color;

    public ToggleCommand(String action, ChatFormatting formatting) {
        super(action, action.toLowerCase() + " <name>");
        this.lowerCase = action.toLowerCase();
        this.color = formatting.toString();
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            String built = String.join(" ", args);
            String idkRurAmogus = this.doStuff(built);
            AbstractModule found = this.getModule(idkRurAmogus);

            if (found == null) {
                AbstractModule similar = this.similar(idkRurAmogus);
                return similar != null
                        ? ChatFormatting.RED + String.format("couldn't find %s from modules, did you mean %s", built, this.moduleNameString(similar))
                        : ChatFormatting.RED + String.format("couldn't find %s from modules", built);
            }

            if (found instanceof Module module) {
                if (this.lowerCase.equals("enable")) {
                    if (module.enabled) {
                        return ChatFormatting.YELLOW + this.moduleNameString(module) + " is already enabled!";
                    }
                    module.enable();
                } else if (this.lowerCase.equals("disable")) {
                    if (!module.enabled) {
                        return ChatFormatting.YELLOW + this.moduleNameString(module) + " is already disabled!";
                    }
                    module.disable();
                } else {
                    module.toggle();
                }

                return this.color + this.lowerCase + "d " + ChatFormatting.WHITE + this.moduleNameString(module);
            } else {
                return String.format("%s%s%s is not toggleable", ChatFormatting.GRAY, this.moduleNameString(found), ChatFormatting.RED);
            }
        } else {
            return this.format;
        }
    }

    private String moduleNameString(AbstractModule module) {
        return module.name.equals(module.getDisplayName()) ? module.name : String.format("%s (%s)", module.getDisplayName(), module.name);
    }

    private AbstractModule similar(String input) {
        AbstractModule best = null;
        double highest = 0.0;

        for (AbstractModule module : Managers.MODULES.getModules()) {
            double similarity = Math.max(
                    StringUtils.similarity(input, this.doStuff(module.name)),
                    StringUtils.similarity(input, this.doStuff(module.getDisplayName()))
            );
            if (similarity > highest) {
                best = module;
                highest = similarity;
            }
        }
        return best;
    }

    private AbstractModule getModule(String name) {
        AbstractModule display = null;
        for (AbstractModule module : Managers.MODULES.getModules()) {
            if (name.equals(this.doStuff(module.name))) return module;
            if (name.equals(this.doStuff(module.getDisplayName()))) display = module;
        }
        return display;
    }

    private String doStuff(String string) {
        return string.toLowerCase().replace(" ", "");
    }
}
