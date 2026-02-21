package bodevelopment.client.blackout.addon;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.ClassUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class AddonLoader {
    public static final List<BlackoutAddon> addons = new ArrayList<>();

    public static void load() {
        BOLogger.info("Loading BlackOut addons...");

        FabricLoader.getInstance()
                .getEntrypointContainers("bodevelopment/client/blackout", BlackoutAddon.class)
                .forEach(container -> {
                    try {
                        BlackoutAddon addon = container.getEntrypoint();
                        ClassLoader addonLoader = addon.getClass().getClassLoader();

                        BOLogger.info(String.format("Found addon: %s (version %s)", addon.getName(), addon.getVersion()));

                        if (addon.modulePath != null) {
                            scan(addonLoader, addon.modulePath, Module.class, addon.modules::add);
                        }

                        if (addon.commandPath != null) {
                            scan(addonLoader, addon.commandPath, Command.class, addon.commands::add);
                        }

                        if (addon.hudPath != null) {
                            scan(addonLoader, addon.hudPath, HudElement.class, addon.hudElements::add);
                        }

                        addon.onInitialize();

                        addon.modules.forEach(Managers.MODULES::add);
                        addon.commands.forEach(Managers.COMMANDS::add);
                        addon.hudElements.forEach(Managers.HUD::add);

                        addons.add(addon);
                    } catch (Exception e) {
                        BOLogger.error("Failed to load addon: " + container.getProvider().getMetadata().getId(), e);
                    }
                });
    }

    private static <T> void scan(ClassLoader loader, String path, Class<T> type, java.util.function.Consumer<T> action) {
        ClassUtils.forEachClass(clazz -> {
            if (type.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
                try {
                    Class<? extends T> targetClazz = clazz.asSubclass(type);
                    T instance = ClassUtils.instance(targetClazz);

                    action.accept(instance);
                } catch (ClassCastException e) {
                    BOLogger.error("Type mismatch during addon scanning: " + clazz.getName());
                } catch (Exception e) {
                    BOLogger.error("Failed to instantiate addon component: " + clazz.getName(), e);
                }
            }
        }, path, loader);
    }
}