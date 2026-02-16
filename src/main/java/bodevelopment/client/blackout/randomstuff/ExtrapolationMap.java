package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ExtrapolationMap {
    private final Map<Entity, Box> boxMap = new ConcurrentHashMap<>();

    public void update(EpicInterface<Entity, Integer> extrapolation) {
        Managers.EXTRAPOLATION.extrapolateMap(this.boxMap, extrapolation);
    }

    public Box get(Entity player) {
        return !this.boxMap.containsKey(player) ? player.getBoundingBox() : this.boxMap.get(player);
    }

    public Map<Entity, Box> getMap() {
        return this.boxMap;
    }

    public int size() {
        return this.boxMap.size();
    }

    public boolean contains(Entity player) {
        return this.boxMap.containsKey(player);
    }

    public Set<Entry<Entity, Box>> entrySet() {
        return this.boxMap.entrySet();
    }

    public void forEach(BiConsumer<Entity, Box> consumer) {
        this.boxMap.forEach(consumer);
    }

    public void clear() {
        this.boxMap.clear();
    }
}
