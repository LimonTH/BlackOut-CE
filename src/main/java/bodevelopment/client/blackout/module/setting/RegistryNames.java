package bodevelopment.client.blackout.module.setting;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class RegistryNames {
    private static final Map<ParticleType<?>, String> particles = new HashMap<>();

    public static void init() {
        particles.clear();

        Registries.PARTICLE_TYPE.forEach(particleType -> {
            Identifier id = Registries.PARTICLE_TYPE.getId(particleType);

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
