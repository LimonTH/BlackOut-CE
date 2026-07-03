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

package bodevelopment.client.blackout.util.render.misc;

import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class FramebufferMultiBufferSource implements MultiBufferSource {
    private final Map<RenderType, ByteBufferBuilder> byteBuilders = new LinkedHashMap<>();
    private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType type) {
        BufferBuilder existing = builders.get(type);
        if (existing != null) return existing;

        ByteBufferBuilder bbb = new ByteBufferBuilder(8192);
        byteBuilders.put(type, bbb);
        BufferBuilder builder = new BufferBuilder(bbb, type.mode(), type.format());
        builders.put(type, builder);
        return builder;
    }

    public void drawToFramebuffer(FrameBuffer fbo) {
        for (var entry : builders.entrySet()) {
            RenderType type = entry.getKey();
            BufferBuilder builder = entry.getValue();

            MeshData mesh = builder.build();
            if (mesh != null) {
                type.setupRenderState();
                fbo.bind(false);
                BufferUploader.drawWithShader(mesh);
                type.clearRenderState();
            }
        }
        for (ByteBufferBuilder bbb : byteBuilders.values()) {
            bbb.close();
        }
        builders.clear();
        byteBuilders.clear();
    }
}
