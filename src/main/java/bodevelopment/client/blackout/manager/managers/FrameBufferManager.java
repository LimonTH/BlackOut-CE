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

package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.hud.HudMergePass;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.rendering.framebuffer.BlendFrameBuffer;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.framebuffer.GuiAlphaFrameBuffer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public class FrameBufferManager extends Manager {
    private final Map<String, FrameBuffer> buffers = new Object2ObjectOpenHashMap<>();

    public FrameBuffer getBuffer(String name) {
        return this.buffers.computeIfAbsent(name, n -> this.add(n, new FrameBuffer()));
    }

    public BlendFrameBuffer getBlend(String name) {
        return (BlendFrameBuffer) this.buffers.computeIfAbsent(name, n -> this.add(n, new BlendFrameBuffer()));
    }

    public GuiAlphaFrameBuffer getGui() {
        return (GuiAlphaFrameBuffer) this.buffers.computeIfAbsent("gui", n -> this.add(n, new GuiAlphaFrameBuffer()));
    }

    public <T extends FrameBuffer> T add(String name, T buffer) {
        this.buffers.put(name, buffer);
        return buffer;
    }

    public void onResize() {
        this.buffers.forEach((name, buffer) -> buffer.resize());
        HudMergePass.onResize();
    }
}
