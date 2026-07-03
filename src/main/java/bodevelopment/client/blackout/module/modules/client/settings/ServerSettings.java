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

package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class ServerSettings extends SettingsModule {
    private static ServerSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> cc = this.sgGeneral.booleanSetting("CC Hitboxes", false,
            "Calculates crystal placement by ensuring there is a 1-block tall space free of any entity hitboxes, preventing placement failures on modern servers.");
    public final Setting<Boolean> oldCrystals = this.sgGeneral.booleanSetting("1.12.2 Crystals", false,
            "Enforces the legacy 1.12.2 placement rule which requires a 2-block high air gap to place End Crystals.");
    public final Setting<Boolean> grimMovement = this.sgGeneral.booleanSetting("Move Fix", false,
            "Synchronizes your movement packets with actual player inputs to satisfy GrimAC's strict motion simulation and prediction checks.");
    public final Setting<Boolean> strictSprint = this.sgGeneral.booleanSetting("Strict Sprint", false,
            "Only allows the sprint state to be active when your movement vector closely matches your look direction to bypass directional sprint checks.",
            () -> !this.grimMovement.get());
    public final Setting<Boolean> grimPackets = this.sgGeneral.booleanSetting("Grim Packets", false,
            "Modifies packet order to send interaction packets before movement updates, helping with action-sequence validation on GrimAC.");
    public final Setting<Boolean> grimUsing = this.sgGeneral.booleanSetting("Grim Using", false,
            "Injects a dedicated rotation packet immediately before any interaction (like using an item) to ensure the server sees you looking at the target on that specific frame.");

    public ServerSettings() {
        super("Server", false, true);
        INSTANCE = this;
    }

    public static ServerSettings getInstance() {
        return INSTANCE;
    }
}
