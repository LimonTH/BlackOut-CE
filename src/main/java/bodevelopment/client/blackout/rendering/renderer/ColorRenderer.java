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

package bodevelopment.client.blackout.rendering.renderer;

import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.RenderState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.ARGB;

@Internal
public class ColorRenderer extends Renderer {
    private static final ColorRenderer INSTANCE = new ColorRenderer();

    public static ColorRenderer getInstance() {
        return INSTANCE;
    }

    public static void renderRounded(PoseStack stack, float x, float y, float width, float height, float rad, int steps, float r, float g, float b, float a) {
        try (RenderState state = INSTANCE.begin(stack, VertexFormat.Mode.TRIANGLE_FAN)) {
            INSTANCE.rounded(x, y, width, height, rad, steps, r, g, b, a);
        }
    }

    public static void renderFitRounded(PoseStack stack, float x, float y, float width, float height, float rad, int steps, float r, float g, float b, float a) {
        try (RenderState state = INSTANCE.begin(stack, VertexFormat.Mode.TRIANGLE_FAN)) {
            INSTANCE.fitRounded(x, y, width, height, rad, steps, r, g, b, a);
        }
    }

    public static void renderCircle(PoseStack stack, float x, float y, float rad, int steps, float r, float g, float b, float a) {
        try (RenderState state = INSTANCE.begin(stack, VertexFormat.Mode.TRIANGLE_FAN)) {
            INSTANCE.circle(x, y, 0.0F, r, g, b, a, rad, steps);
        }
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, int color) {
        this.quad(stack, x, y, w, h, ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color));
    }

    public void quad(PoseStack stack, float x, float y, float w, float h, float r, float g, float b, float a) {
        this.quad(stack, x, y, 0.0F, w, h, r, g, b, a);
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h) {
        this.quad(stack, x, y, z, w, h, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h, int color) {
        this.quad(stack, x, y, z, w, h, ARGB.red(color), ARGB.green(color), ARGB.blue(color), ARGB.alpha(color));
    }

    public void quad(PoseStack stack, float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        try (RenderState state = this.begin(stack, VertexFormat.Mode.QUADS)) {
            this.vertex(x, y, z, r, g, b, a);
            this.vertex(x, y + h, z, r, g, b, a);
            this.vertex(x + w, y + h, z, r, g, b, a);
            this.vertex(x + w, y, z, r, g, b, a);
        }
    }

    public void quadOutlineShape(float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        this.vertex(x, y, z, r, g, b, a);
        this.vertex(x, y + h, z, r, g, b, a);
        this.vertex(x + w, y + h, z, r, g, b, a);
        this.vertex(x + w, y, z, r, g, b, a);
        this.vertex(x, y, z, r, g, b, a);
    }

    public RenderState begin(PoseStack stack, VertexFormat.Mode drawMode) {
        return this.begin(stack, drawMode, DefaultVertexFormat.POSITION_COLOR);
    }

    public RenderState beginLines(PoseStack stack) {
        return this.begin(stack, VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
    }

    private RenderState begin(PoseStack stack, VertexFormat.Mode drawMode, VertexFormat format) {
        this.renderMatrix = stack.last().pose();
        RenderSystem.enableBlend();
        this.renderBuffer = Tesselator.getInstance().begin(drawMode, format);
        return RenderState.of(() -> {
            Shaders.color.render(this.renderBuffer, null);
            RenderSystem.disableBlend();
        });
    }
}
