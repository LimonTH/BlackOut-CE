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

package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.rendering.shader.Shader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ShaderSetup {
    private final List<Consumer<Shader>> list = new ArrayList<>();

    public ShaderSetup(Consumer<Shader> consumer) {
        this.list.add(consumer);
    }

    public ShaderSetup() {
    }

    public ShaderSetup append(Consumer<Shader> consumer) {
        this.list.add(consumer);
        return this;
    }

    public void setup(Shader shader) {
        this.list.forEach(consumer -> consumer.accept(shader));
    }
}
