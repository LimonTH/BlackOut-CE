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
