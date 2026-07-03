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

package bodevelopment.client.blackout.rendering.shader;

import java.util.Arrays;

/**
 * Central registry of all shader programs.
 * Each static field is populated automatically by {@link #loadAll()}.
 */
public final class Shaders {

    public static Shader rainbow;
    public static Shader menu;
    public static Shader picker;
    public static Shader font;
    public static Shader fontshadow;
    public static Shader texture;
    public static Shader blur;
    public static Shader color;
    public static Shader screenblur;
    public static Shader glowesp;
    public static Shader outlineesp;
    public static Shader screentex;
    public static Shader rounded;
    public static Shader roundedshadow;
    public static Shader roundedfade;
    public static Shader shadowfade;
    public static Shader tenacity;
    public static Shader tenacityshadow;
    public static Shader roundedrainbow;
    public static Shader shadowrainbow;
    public static Shader gradientfont;
    public static Shader bloom;
    public static Shader textureUV;
    public static Shader blurUV;
    public static Shader smoke;
    public static Shader skeet;
    public static Shader shaderbloom;
    public static Shader bloomblur;
    public static Shader screentexcolor;
    public static Shader screentexoverlay;
    public static Shader fontwave;
    public static Shader convert;
    public static Shader subtract;

    public static void loadAll() {
        ShaderReader.loadAll();

        Arrays.stream(Shaders.class.getDeclaredFields())
                .filter(f -> f.getType() == Shader.class)
                .forEach(field -> {
                    try {
                        field.set(null, new Shader(field.getName()));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to initialise shader: " + field.getName(), e);
                    }
                });
    }

    private Shaders() {
    }
}
