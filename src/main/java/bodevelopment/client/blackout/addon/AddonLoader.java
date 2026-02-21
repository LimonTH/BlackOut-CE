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
                            ClassUtils.forEachClass(clazz -> {
                                if (Module.class.isAssignableFrom(clazz)
                                        && !clazz.isInterface()
                                        && !Modifier.isAbstract(clazz.getModifiers())) {

                                    addon.modules.add((Module) ClassUtils.instance(clazz));
                                }
                            }, addon.modulePath, addonLoader);
                        }

                        if (addon.commandPath != null) {
                            ClassUtils.forEachClass(clazz -> {
                                if (Command.class.isAssignableFrom(clazz)
                                        && !clazz.isInterface()
                                        && !Modifier.isAbstract(clazz.getModifiers())) {

                                    addon.commands.add((Command) ClassUtils.instance(clazz));
                                }
                            }, addon.commandPath, addonLoader);
                        }

                        if (addon.hudPath != null) {
                            ClassUtils.forEachClass(clazz -> {
                                if (HudElement.class.isAssignableFrom(clazz)
                                        && !clazz.isInterface()
                                        && !Modifier.isAbstract(clazz.getModifiers())) {

                                    Managers.HUD.add(clazz.asSubclass(HudElement.class));
                                }
                            }, addon.hudPath, addonLoader);
                        }
                        addon.onInitialize();

                        addon.modules.forEach(Managers.MODULES::add);
                        addon.commands.forEach(Managers.COMMANDS::add);
                        addon.hudElements.forEach(Managers.HUD::add);

                        addons.add(addon);
                    } catch (Exception e) {
                        BOLogger.error("Failed to load addon: " + container.getProvider().getMetadata().getId());
                    }
                });

        BOLogger.info(String.format("Successfully loaded %s addons.", addons.size()));
    }
}