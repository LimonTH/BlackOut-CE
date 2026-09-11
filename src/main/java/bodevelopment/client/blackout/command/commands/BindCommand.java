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
import bodevelopment.client.blackout.enums.ConfigType;
import bodevelopment.client.blackout.keys.Key;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.keys.Keys;
import bodevelopment.client.blackout.keys.MouseButton;
import bodevelopment.client.blackout.keys.MouseButtons;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.module.modules.client.HUDSettings;
import bodevelopment.client.blackout.util.StringUtils;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BindCommand extends Command {
    private static final Map<String, Integer> KEY_ALIASES = new HashMap<>();
    private static final List<String> KEY_SUGGESTIONS = new ArrayList<>();

    static {
        alias(340, "shift", "lshift", "left_shift", "leftshift", "l_shift");
        alias(344, "rshift", "right_shift", "rightshift", "r_shift");
        alias(341, "ctrl", "control", "lctrl", "left_ctrl", "leftctrl", "l_control", "lcontrol");
        alias(345, "rctrl", "r_ctrl", "right_ctrl", "rightctrl", "r_control", "rcontrol", "right_control");
        alias(342, "alt", "lalt", "left_alt", "leftalt", "l_alt");
        alias(346, "ralt", "r_alt", "right_alt", "rightalt", "r_alt");
        alias(343, "lwin", "l_win", "left_win", "leftwin", "super", "lsuper");
        alias(347, "rwin", "r_win", "right_win", "rightwin", "rsuper");
        alias(348, "menu", "apps");

        alias(32, "space", "spacebar");
        alias(257, "enter", "return", "kp_enter");
        alias(258, "tab");
        alias(259, "back", "backspace");
        alias(261, "del", "delete");
        alias(260, "ins", "insert");
        alias(256, "esc", "escape");

        alias(265, "up", "arrow_up", "arrowup", "arrow-up");
        alias(264, "down", "arrow_down", "arrowdown", "arrow-down");
        alias(263, "left", "arrow_left", "arrowleft", "arrow-left");
        alias(262, "right", "arrow_right", "arrowright", "arrow-right");
        alias(268, "home");
        alias(269, "end");
        alias(266, "pgup", "page_up", "pageup", "pg_up");
        alias(267, "pgdn", "pg_down", "pagedown", "pg_dn", "pagedn");
        alias(280, "caps", "capslock", "caps_lock");
        alias(281, "scr", "scrlk", "scrolllock", "scroll_lock");
        alias(282, "num", "numlock", "num_lock", "num_lck");
        alias(283, "prtsc", "printscreen", "print_screen", "prntscrn");
        alias(284, "pause", "break");

        alias(39, "apostrophe", "'", "quote");
        alias(44, ",", "comma");
        alias(45, "-", "minus", "dash");
        alias(46, ".", "period", "dot");
        alias(47, "/", "slash");
        alias(59, ";", "semicolon");
        alias(61, "=", "equal", "equals");
        alias(91, "[", "left_bracket", "lbracket");
        alias(92, "\\", "backslash");
        alias(93, "]", "right_bracket", "rbracket");
        alias(96, "`", "grave", "grave_accent", "backtick", "tilde");

        alias(330, "num_dot", "numpad_dot", "kp_decimal", "kp_.");
        alias(331, "num_div", "numpad_div", "kp_divide", "kp_/");
        alias(332, "num_mul", "numpad_mul", "kp_multiply", "kp_*");
        alias(333, "num_sub", "numpad_sub", "kp_subtract", "kp_-");
        alias(334, "num_add", "numpad_add", "kp_add", "kp_+");
        alias(335, "num_enter", "numpad_enter", "numpad_ent");
        alias(336, "num_eq", "numpad_eq", "kp_equal", "kp_=");
        for (int i = 0; i <= 9; i++) {
            alias(320 + i, "num_" + i, "numpad_" + i, "kp_" + i, "numpad" + i);
        }

        for (char c = 'A'; c <= 'Z'; c++) {
            KEY_SUGGESTIONS.add(String.valueOf(c));
        }
        for (char c = '0'; c <= '9'; c++) {
            KEY_SUGGESTIONS.add(String.valueOf(c));
        }
        for (int i = 1; i <= 12; i++) {
            KEY_SUGGESTIONS.add("F" + i);
        }
        KEY_SUGGESTIONS.add("RSHIFT");
        KEY_SUGGESTIONS.add("INSERT");
        KEY_SUGGESTIONS.add("DELETE");
        KEY_SUGGESTIONS.add("HOME");
        KEY_SUGGESTIONS.add("END");
        KEY_SUGGESTIONS.add("MOUSE3");
        KEY_SUGGESTIONS.add("MOUSE4");
        KEY_SUGGESTIONS.add("NONE");
    }

    private static void alias(int key, String... names) {
        for (String name : names) {
            KEY_ALIASES.put(name, key);
            KEY_SUGGESTIONS.add(name.toUpperCase(Locale.ROOT));
        }
    }

    public BindCommand() {
        super("bind", "Usage: bind <clickgui/hud/module> <key|none>");
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }

    @Override
    public String execute(String[] args) {
        if (args.length == 0) {
            return this.format;
        }

        String first = args[0].toLowerCase(Locale.ROOT);
        if (isClickGuiTarget(first)) {
            if (args.length == 1) {
                return "ClickGUI bind is " + ChatFormatting.WHITE + currentClickGuiName();
            }
            if (args.length > 2) {
                return this.format;
            }
            return bindClickGui(args[1]);
        }
        if (isHudTarget(first)) {
            if (args.length == 1) {
                return "HUD editor bind is " + ChatFormatting.WHITE + currentHudName();
            }
            if (args.length > 2) {
                return this.format;
            }
            return bindHud(args[1]);
        }

        if (args.length < 2) {
            return this.format;
        }
        String keyName = args[args.length - 1];
        String moduleName = String.join(" ", java.util.Arrays.copyOf(args, args.length - 1));

        AbstractModule found = getModule(moduleName);
        if (found == null) {
            AbstractModule similar = similar(moduleName);
            return similar != null
                    ? ChatFormatting.RED + String.format("couldn't find %s from modules, did you mean %s", moduleName, similar.name)
                    : ChatFormatting.RED + String.format("couldn't find %s from modules", moduleName);
        }
        if (!(found instanceof Module module)) {
            return ChatFormatting.RED + found.name + " is not toggleable";
        }
        return bindModule(module, keyName);
    }

    private String bindClickGui(String keyName) {
        GuiSettings settings = GuiSettings.getInstance();
        if (settings == null) {
            return ChatFormatting.RED + "GUI settings not loaded yet.";
        }
        ParsedBind parsed = parse(keyName);
        if (parsed == null) {
            return ChatFormatting.RED + "Unknown key \"" + keyName + "\". Use e.g. RSHIFT, INSERT, F6, MOUSE4 or NONE.";
        }
        if (parsed.isNone()) {
            settings.openKey.get().value = null;
            Managers.CONFIG.save(ConfigType.Client);
            return "ClickGUI bind " + ChatFormatting.RED + "cleared" + ChatFormatting.GRAY + " (use " + ChatFormatting.WHITE + "bind clickgui <key>" + ChatFormatting.GRAY + " to set a new one)";
        }
        if (parsed.isMouse()) {
            settings.openKey.get().value = new MouseButton(parsed.code);
            Managers.CONFIG.save(ConfigType.Client);
            return "ClickGUI bind set to " + ChatFormatting.WHITE + MouseButtons.getKeyName(parsed.code)
                    + ChatFormatting.GRAY + " (mouse binds open the GUI, close with ESC)";
        }
        if (isReservedKey(parsed.code)) {
            return ChatFormatting.RED + "Cannot bind ClickGUI to " + Keys.getKeyName(parsed.code) + ". ESC/BACK/DEL are reserved, use another key or NONE.";
        }
        settings.openKey.get().value = new Key(parsed.code);
        Managers.CONFIG.save(ConfigType.Client);
        return "ClickGUI bind set to " + ChatFormatting.WHITE + Keys.getKeyName(parsed.code);
    }

    private String bindHud(String keyName) {
        HUDSettings settings = HUDSettings.getInstance();
        if (settings == null) {
            return ChatFormatting.RED + "HUD settings not loaded yet.";
        }
        ParsedBind parsed = parse(keyName);
        if (parsed == null) {
            return ChatFormatting.RED + "Unknown key \"" + keyName + "\". Use e.g. RSHIFT, INSERT, F6, MOUSE4 or NONE.";
        }
        if (parsed.isNone()) {
            settings.editorKey.get().value = null;
            Managers.CONFIG.save(ConfigType.Client);
            return "HUD editor bind " + ChatFormatting.RED + "cleared";
        }
        if (parsed.isMouse()) {
            settings.editorKey.get().value = new MouseButton(parsed.code);
            Managers.CONFIG.save(ConfigType.Client);
            return "HUD editor bind set to " + ChatFormatting.WHITE + MouseButtons.getKeyName(parsed.code);
        }
        if (isReservedKey(parsed.code)) {
            return ChatFormatting.RED + "Cannot bind HUD editor to " + Keys.getKeyName(parsed.code) + ". ESC/BACK/DEL are reserved, use another key or NONE.";
        }
        settings.editorKey.get().value = new Key(parsed.code);
        Managers.CONFIG.save(ConfigType.Client);
        return "HUD editor bind set to " + ChatFormatting.WHITE + Keys.getKeyName(parsed.code);
    }

    private String bindModule(Module module, String keyName) {
        ParsedBind parsed = parse(keyName);
        if (parsed == null) {
            return ChatFormatting.RED + "Unknown key \"" + keyName + "\". Use e.g. R, F6, MOUSE4 or NONE.";
        }
        KeyBind bind = module.bind.get();
        if (parsed.isNone()) {
            bind.value = null;
            Managers.CONFIG.save(ConfigType.Binds);
            return "Unbound " + ChatFormatting.WHITE + module.name;
        }
        if (parsed.isMouse()) {
            bind.value = new MouseButton(parsed.code);
            Managers.CONFIG.save(ConfigType.Binds);
            return "Bound " + ChatFormatting.WHITE + module.name + ChatFormatting.GRAY + " to " + ChatFormatting.WHITE + MouseButtons.getKeyName(parsed.code);
        }
        if (isReservedKey(parsed.code)) {
            return ChatFormatting.RED + "Cannot bind " + module.name + " to " + Keys.getKeyName(parsed.code) + ". ESC/BACK/DEL are reserved, use another key or NONE.";
        }
        bind.value = new Key(parsed.code);
        Managers.CONFIG.save(ConfigType.Binds);
        return "Bound " + ChatFormatting.WHITE + module.name + ChatFormatting.GRAY + " to " + ChatFormatting.WHITE + Keys.getKeyName(parsed.code);
    }

    private String currentClickGuiName() {
        GuiSettings settings = GuiSettings.getInstance();
        if (settings == null) return "RSHIFT";
        KeyBind bind = settings.openKey.get();
        if (bind == null || bind.value == null) return "NONE";
        return bind.getName();
    }

    private String currentHudName() {
        HUDSettings settings = HUDSettings.getInstance();
        if (settings == null) return "NONE";
        KeyBind bind = settings.editorKey.get();
        if (bind == null || bind.value == null) return "NONE";
        return bind.getName();
    }

    private boolean isClickGuiTarget(String name) {
        return name.equals("clickgui") || name.equals("gui") || name.equals("click_gui") || name.equals("click-gui");
    }

    private boolean isHudTarget(String name) {
        return name.equals("hud") || name.equals("editor") || name.equals("hudeditor") || name.equals("hud_editor");
    }

    private boolean isReservedKey(int key) {
        return key == 256 || key == 259 || key == 261;
    }

    private AbstractModule getModule(String name) {
        String cleaned = name.toLowerCase(Locale.ROOT).replace(" ", "");
        AbstractModule display = null;
        for (AbstractModule module : Managers.MODULES.getModules()) {
            if (cleaned.equals(module.name.toLowerCase(Locale.ROOT).replace(" ", ""))) return module;
            if (cleaned.equals(module.getDisplayName().toLowerCase(Locale.ROOT).replace(" ", ""))) display = module;
        }
        return display;
    }

    private AbstractModule similar(String input) {
        AbstractModule best = null;
        double highest = 0.0;
        String cleaned = input.toLowerCase(Locale.ROOT).replace(" ", "");
        for (AbstractModule module : Managers.MODULES.getModules()) {
            double similarity = Math.max(
                    StringUtils.similarity(cleaned, module.name.toLowerCase(Locale.ROOT).replace(" ", "")),
                    StringUtils.similarity(cleaned, module.getDisplayName().toLowerCase(Locale.ROOT).replace(" ", ""))
            );
            if (similarity > highest) {
                best = module;
                highest = similarity;
            }
        }
        return best;
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            out.add("clickgui");
            out.add("hud");
            for (AbstractModule m : Managers.MODULES.getToggleableModules()) {
                out.add(m.name);
            }
            return out;
        }
        if (args.length == 2) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (isClickGuiTarget(first) || isHudTarget(first) || getModule(args[0]) != null) {
                return KEY_SUGGESTIONS;
            }
            return Collections.emptyList();
        }
        String joined = String.join(" ", java.util.Arrays.copyOf(args, args.length - 1));
        if (getModule(joined) != null) {
            return KEY_SUGGESTIONS;
        }
        return Collections.emptyList();
    }

    private static ParsedBind parse(String input) {
        if (input == null) return null;
        String s = input.toLowerCase(Locale.ROOT).trim().replace("-", "_").replace(" ", "_");
        if (s.equals("none") || s.equals("null") || s.equals("clear") || s.equals("unbind") || s.equals("off")) {
            return ParsedBind.none();
        }
        if (s.equals("mouse_left") || s.equals("m_left") || s.equals("mouseleft")) return ParsedBind.mouse(0);
        if (s.equals("mouse_right") || s.equals("m_right") || s.equals("mouseright")) return ParsedBind.mouse(1);
        if (s.equals("mouse_middle") || s.equals("m_middle") || s.equals("mousemiddle")) return ParsedBind.mouse(2);
        if (s.startsWith("mouse")) {
            String num = s.substring("mouse".length()).replace("_", "");
            try {
                return ParsedBind.mouse(Integer.parseInt(num));
            } catch (NumberFormatException ignored) {
            }
        }
        if ((s.startsWith("m") && s.length() <= 3)) {
            try {
                return ParsedBind.mouse(Integer.parseInt(s.substring(1)));
            } catch (NumberFormatException ignored) {
            }
        }
        if (KEY_ALIASES.containsKey(s)) {
            return ParsedBind.key(KEY_ALIASES.get(s));
        }
        if (s.matches("f\\d{1,2}")) {
            try {
                int n = Integer.parseInt(s.substring(1));
                if (n >= 1 && n <= 25) return ParsedBind.key(289 + n);
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if (s.length() == 1) {
            char c = Character.toUpperCase(s.charAt(0));
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || ";='`,./\\[]-".indexOf(c) >= 0 || c == '\'') {
                return ParsedBind.key(c);
            }
            if (c == ' ') return ParsedBind.key(32);
        }
        return null;
    }

    private record ParsedBind(int code, boolean mouseBind, boolean clear) {
        static ParsedBind key(int code) {
            return new ParsedBind(code, false, false);
        }

        static ParsedBind mouse(int code) {
            return new ParsedBind(code, true, false);
        }

        static ParsedBind none() {
            return new ParsedBind(-1, false, true);
        }

        boolean isMouse() {
            return mouseBind && !clear;
        }

        boolean isNone() {
            return clear;
        }
    }
}
