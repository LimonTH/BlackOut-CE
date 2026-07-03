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

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Simulation extends Module {
    private static Simulation INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> autoMine = this.sgGeneral.booleanSetting("Predict Mining", true, "Simulates block breaking and item drops client-side for a more responsive mining experience.");
    private final Setting<Boolean> pickSwitch = this.sgGeneral.booleanSetting("Predict Tool Swap", true, "Simulates the tool switching process to reduce perceived visual latency.");
    private final Setting<Boolean> quiverShoot = this.sgGeneral.booleanSetting("Predict Quiver", true, "Simulates arrow consumption and projectile launching when using Quiver.");
    private final Setting<Boolean> hitReset = this.sgGeneral.booleanSetting("Attack Reset", true, "Resets the visual weapon swing and attack charge locally when a packet is sent.");
    private final Setting<Boolean> stopSprint = this.sgGeneral.booleanSetting("Sprint Stop", false, "Simulates the cessation of sprinting when executing an attack.");
    private final Setting<Double> stopManagerContainer = this.sgGeneral.doubleSetting("Manager Pause Delay", 0.5, 0.0, 5.0, 0.05, "The duration in seconds to suspend the Manager module after interacting with a container.");

    public Simulation() {
        super("Simulation", "Manages client-side predictions and local state simulations to synchronize visuals with server-side actions.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static Simulation getInstance() {
        return INSTANCE;
    }

    public boolean blocks() {
        return this.enumSetting(INSTANCE.autoMine);
    }

    public boolean pickSwitch() {
        return this.enumSetting(this.pickSwitch);
    }

    public boolean quiverShoot() {
        return this.enumSetting(this.quiverShoot);
    }

    public boolean hitReset() {
        return this.enumSetting(this.hitReset);
    }

    public boolean stopSprint() {
        return this.enumSetting(this.stopSprint);
    }

    public double managerStop() {
        return !this.enabled ? 0.0 : this.stopManagerContainer.get();
    }

    private boolean enumSetting(Setting<Boolean> s) {
        return this.enabled && s.get();
    }
}
