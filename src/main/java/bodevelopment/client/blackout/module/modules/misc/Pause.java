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
import bodevelopment.client.blackout.randomstuff.Pair;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayList;
import java.util.List;

public class Pause extends Module {
    private static Pause INSTANCE;

    public final List<Pair<ChannelHandlerContext, Packet<?>>> packets = new ArrayList<>();
    public boolean emptying = false;

    public Pause() {
        super("Pause", "Temporarily halts the processing of incoming network packets, allowing you to buffer server data.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static Pause getInstance() {
        return INSTANCE;
    }
}
