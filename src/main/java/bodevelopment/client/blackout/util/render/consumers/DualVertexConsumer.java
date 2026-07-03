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

package bodevelopment.client.blackout.util.render.consumers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public record DualVertexConsumer(VertexConsumer first, VertexConsumer second) implements VertexConsumer {
    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        first.addVertex(x, y, z);
        second.addVertex(x, y, z);
        return this;
    }

    @Override
    public @NotNull VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        first.addVertex(matrix, x, y, z);
        second.addVertex(matrix, x, y, z);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
        first.setColor(r, g, b, a);
        second.setColor(r, g, b, a);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        first.setUv(u, v);
        second.setUv(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        first.setUv1(u, v);
        second.setUv1(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        first.setUv2(u, v);
        second.setUv2(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        first.setNormal(x, y, z);
        second.setNormal(x, y, z);
        return this;
    }
}
