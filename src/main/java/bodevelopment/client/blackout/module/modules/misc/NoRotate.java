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

public class NoRotate extends Module {
    private static NoRotate INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<NoRotateMode> mode = this.sgGeneral.enumSetting("Bypass Mode", NoRotateMode.Cancel, "Determines how server-mandated rotation packets are handled during a rubberband or teleport event.");

    public float relYaw = 0;
    public float relPitch = 0;

    public NoRotate() {
        super("No Rotate", "Prevents the server from forcefully resetting your camera rotation, helping maintain focus during movement desync.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static NoRotate getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    public enum NoRotateMode {
        Cancel,
        Set,
        Spoof,
        Rel
    }
}
