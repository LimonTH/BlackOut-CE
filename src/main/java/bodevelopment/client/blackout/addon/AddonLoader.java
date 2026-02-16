package bodevelopment.client.blackout.addon;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.ArrayList;
import java.util.List;

public class AddonLoader {
    public static final List<BlackoutAddon> addons = new ArrayList<>();

    public static void load() {
        FabricLoader.getInstance().getEntrypointContainers("bodevelopment/client/blackout", BlackoutAddon.class).stream().map(EntrypointContainer::getEntrypoint).forEach(addons::add);
    }
}
