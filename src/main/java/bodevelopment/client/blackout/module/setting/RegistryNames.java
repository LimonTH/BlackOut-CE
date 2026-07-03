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

package bodevelopment.client.blackout.module.setting;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class RegistryNames {
    private static final Map<ParticleType<?>, String> particles = new HashMap<>();

    public static void init() {
        particles.clear();

        BuiltInRegistries.PARTICLE_TYPE.forEach(particleType -> {
            ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);

            if (id != null) {
                String name = id.getPath();
                particles.put(particleType, capitalize(name.replace("_", " ")));
            }
        });
    }

    public static String get(ParticleType<?> particleType) {
        return particles.getOrDefault(particleType, "null");
    }

    private static String capitalize(String string) {
        return String.valueOf(string.charAt(0)).toUpperCase() + string.substring(1);
    }
}
