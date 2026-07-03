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

package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.annotations.Internal;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.CoreShaders;

/**
 * AutoCloseable GL state management. Automatically restores state on close.
 * <p>
 * Usage:
 * try (RenderState state = RenderState.blend2D()) {
 * // draw calls
 * }
 */
@Internal
public class RenderState implements AutoCloseable {
    private final Runnable restore;

    private RenderState(Runnable restore) {
        this.restore = restore;
    }

    /**
     * Creates a RenderState from an arbitrary cleanup action.
     * Used by renderers to wrap startRender/endRender in try-with-resources.
     */
    public static RenderState of(Runnable cleanup) {
        return new RenderState(cleanup);
    }

    /**
     * 2D blended color rendering (HUD quads, lines, circles).
     * Enables: blend, defaultBlendFunc, POSITION_COLOR shader.
     */
    public static RenderState blend2D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        return new RenderState(RenderSystem::disableBlend);
    }

    /**
     * 2D blended textured rendering.
     * Enables: blend, defaultBlendFunc, POSITION_TEX shader.
     */
    public static RenderState texture2D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        return new RenderState(() -> {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        });
    }

    /**
     * 3D blended rendering without depth test (ESP boxes, filled shapes).
     * Enables: blend, defaultBlendFunc, disables: cull, depthTest, depthMask.
     */
    public static RenderState blend3D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return new RenderState(() -> {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        });
    }

    /**
     * 3D line rendering (outlines, wireframes).
     * Same as blend3D + RENDERTYPE_LINES shader + lineWidth.
     */
    public static RenderState lines3D(float width) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
        RenderSystem.lineWidth(width);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return new RenderState(() -> {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        });
    }

    /**
     * 2D blended color rendering with depth and cull disabled (angled circles, overlays).
     */
    public static RenderState blend2DFull() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        return new RenderState(() -> {
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }

    @Override
    public void close() {
        this.restore.run();
    }
}
