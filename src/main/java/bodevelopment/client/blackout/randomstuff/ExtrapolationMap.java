package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class ExtrapolationMap {
    private final Map<Entity, AABB> boxMap = new ConcurrentHashMap<>();

    public void update(EpicInterface<Entity, Integer> extrapolation) {
        Managers.EXTRAPOLATION.extrapolateMap(this.boxMap, extrapolation);
    }

    public AABB get(Entity player) {
        AABB cached = this.boxMap.get(player);
        return cached != null ? cached : player.getBoundingBox();
    }

    public Map<Entity, AABB> getMap() {
        return this.boxMap;
    }

    public int size() {
        return this.boxMap.size();
    }

    public boolean contains(Entity player) {
        return this.boxMap.containsKey(player);
    }

    public Set<Entry<Entity, AABB>> entrySet() {
        return this.boxMap.entrySet();
    }

    public void forEach(BiConsumer<Entity, AABB> consumer) {
        this.boxMap.forEach(consumer);
    }

    public void clear() {
        this.boxMap.clear();
    }
}
