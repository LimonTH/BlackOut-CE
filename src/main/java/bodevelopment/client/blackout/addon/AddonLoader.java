package bodevelopment.client.blackout.addon;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.module.modules.client.MenuMusicSettings;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.randomstuff.mainmenu.MainMenuRenderer;
import bodevelopment.client.blackout.theme.Theme;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.ClassUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Internal
public class AddonLoader {
    public static final List<BlackoutAddon> addons = new ArrayList<>();
    private static boolean anyAddonHadWorld = false;

    public static void load() {
        BOLogger.info("Loading BlackOut addons...");

        FabricLoader.getInstance()
                .getEntrypointContainers("bodevelopment/client/blackout", BlackoutAddon.class)
                .forEach(container -> {
                    try {
                        BlackoutAddon addon = container.getEntrypoint();
                        ClassLoader addonLoader = addon.getAddonClassLoader();

                        BOLogger.info(String.format("Found addon: %s (version %s, api %d)",
                                addon.getName(), addon.getVersion(), addon.getApiVersion()));

                        if (!checkApiCompatibility(addon)) {
                            return;
                        }

                        if (!checkClientVersion(addon)) {
                            return;
                        }

                        addon.onInitialize();

                        if (addon.modulePath != null) {
                            scan(addonLoader, addon.modulePath, AbstractModule.class, instance -> {
                                Managers.MODULES.add(instance);
                                addon.modules.add(instance);
                            });
                        }

                        if (addon.commandPath != null) {
                            scan(addonLoader, addon.commandPath, Command.class, instance -> {
                                Managers.COMMANDS.add(instance);
                                addon.commands.add(instance);
                            });
                        }

                        if (addon.hudPath != null) {
                            scan(addonLoader, addon.hudPath, HudElement.class, instance -> {
                                if (Managers.HUD.getElements().stream().noneMatch(p -> p.getB().equals(instance.getClass()))) {
                                    Managers.HUD.add(instance);
                                    addon.hudElements.add(instance);
                                }
                            });
                        }

                        if (addon.guiPath != null) {
                            scan(addonLoader, addon.guiPath, ClickGuiScreen.class, addon.guiScreens::add);
                            scan(addonLoader, addon.guiPath, MainMenuRenderer.class, instance -> {
                                String id = instance.getClass().getSimpleName();
                                addon.menuRenderers.put(id, instance);
                            });
                        }

                        for (Theme theme : addon.themes) {
                            if (!ThemeSettings.themes.contains(theme)) {
                                ThemeSettings.themes.add(theme);
                            }
                        }
                        addon.menuRenderers.forEach(MainMenuSettings.CUSTOM_RENDERERS::putIfAbsent);
                        addon.musicTracks.forEach(MenuMusicSettings.CUSTOM_TRACKS::putIfAbsent);

                        BlackOut.EVENT_BUS.subscribe(addon, () -> false);

                        addon.onEnable();

                        loadAddonIcon(addon, container.getProvider());

                        addons.add(addon);
                    } catch (Exception e) {
                        BOLogger.error("Failed to load addon: " + container.getProvider().getMetadata().getId(), e);
                    }
                });

        BlackOut.EVENT_BUS.subscribe(new AddonLifecycleBridge(), () -> false);
    }

    public static void unloadAll() {
        for (BlackoutAddon addon : addons) {
            try {
                addon.onDisable();
                BlackOut.EVENT_BUS.unsubscribe(addon);
            } catch (Exception e) {
                BOLogger.error("Error disabling addon: " + addon.getName(), e);
            }
        }
        addons.clear();
        anyAddonHadWorld = false;
    }

    /**
     * Primary compatibility check based on the API contract version.
     *
     * <h3>Rules</h3>
     * <ul>
     *   <li>{@code addon.api > client.api} — <b>HARD REJECT</b>:
     *       the addon uses API features not yet available in this client.</li>
     *   <li>{@code addon.api < client.api} — <b>SOFT WARN</b>:
     *       the addon targets an older API; deprecated symbols may have been removed.</li>
     *   <li>{@code addon.api == client.api} — <b>OK</b>.</li>
     * </ul>
     *
     * @return {@code true} if the addon passed the API gate
     */
    private static boolean checkApiCompatibility(BlackoutAddon addon) {
        int addonApi = addon.getApiVersion();
        int clientApi = BlackOut.API_VERSION;

        if (addonApi > clientApi) {
            BOLogger.error(String.format(
                    "[%s] HARD REJECT: addon requires API v%d but client provides v%d. "
                            + "Update BlackOut Client to use this addon.",
                    addon.getName(), addonApi, clientApi));
            return false;
        }

        if (addonApi < clientApi) {
            BOLogger.warn(String.format(
                    "[%s] SOFT WARN: addon targets API v%d but client is v%d. "
                            + "Deprecated APIs may have been removed. "
                            + "If the addon misbehaves, ask the author to update it.",
                    addon.getName(), addonApi, clientApi));
        }

        return true;
    }

    /**
     * Secondary compatibility check based on the client (mod) version.
     *
     * <p>This is <b>optional</b> — only triggered when the addon overrides
     * {@link BlackoutAddon#getMinClientVersion()}. Use this when the addon
     * depends on a specific client behaviour (bugfix, rendering change, etc.)
     * that is not reflected in the API version.</p>
     *
     * @return {@code true} if the addon passed the version gate (or didn't set one)
     */
    private static boolean checkClientVersion(BlackoutAddon addon) {
        String minVersion = addon.getMinClientVersion();
        if (minVersion == null) {
            return true;
        }

        if (!isVersionCompatible(minVersion, BlackOut.VERSION)) {
            BOLogger.error(String.format(
                    "[%s] HARD REJECT: addon requires BlackOut >= %s but current is %s.",
                    addon.getName(), minVersion, BlackOut.VERSION));
            return false;
        }

        return true;
    }

    /**
     * Compares two dot-separated version strings using natural numeric ordering.
     * <p>
     * The {@code minVersion} may contain {@code *} wildcards that match any value
     * at that position. This allows addons to declare compatibility with entire
     * minor/patch ranges without listing every version.
     * <p>
     * Examples:
     * <pre>{@code
     * // Exact / range
     * isVersionCompatible("2.2", "2.2")   → true
     * isVersionCompatible("2.2", "2.3")   → true
     * isVersionCompatible("2.3", "2.2")   → false
     * isVersionCompatible("2.2.1", "2.2") → true
     * isVersionCompatible("2.10", "2.2")  → true  (numeric: 10 > 2, not lexicographic)
     *
     * // Wildcards
     * isVersionCompatible("2.*", "2.0")   → true
     * isVersionCompatible("2.*", "2.99")  → true
     * isVersionCompatible("2.*", "3.0")   → false (major mismatch)
     * isVersionCompatible("*", "any")     → true  (matches anything)
     * isVersionCompatible("2.2.*", "2.2.5") → true
     * }</pre>
     */
    private static boolean isVersionCompatible(String minVersion, String currentVersion) {
        if (minVersion == null || currentVersion == null) return true;

        String[] minParts = minVersion.split("\\.");
        String[] curParts = currentVersion.split("\\.");

        int len = Math.max(minParts.length, curParts.length);
        for (int i = 0; i < len; i++) {
            String minPart = i < minParts.length ? minParts[i] : "0";
            String curPart = i < curParts.length ? curParts[i] : "0";

            if ("*".equals(minPart)) {
                continue;
            }

            int min = parseVersionPart(minPart);
            int cur = parseVersionPart(curPart);

            if (cur > min) return true;
            if (cur < min) return false;
        }
        return true;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            BOLogger.warn("Non-numeric version component: '" + part + "', treating as 0");
            return 0;
        }
    }

    private static void loadAddonIcon(BlackoutAddon addon, ModContainer modContainer) {
        try {
            ModMetadata metadata = modContainer.getMetadata();
            Optional<String> iconPath = metadata.getIconPath(64);
            if (iconPath.isEmpty()) iconPath = metadata.getIconPath(0);
            if (iconPath.isEmpty()) return;

            Optional<Path> resolved = modContainer.findPath(iconPath.get());
            if (resolved.isEmpty()) return;

            try (InputStream is = Files.newInputStream(resolved.get())) {
                BufferedImage image = ImageIO.read(is);
                addon.loadIconFromMod(image);
            }
        } catch (Exception e) {
            BOLogger.error("Failed to load icon for addon: " + addon.getName(), e);
        }
    }

    private static <T> void scan(ClassLoader loader, String path, Class<T> type, java.util.function.Consumer<T> action) {
        ClassUtils.forEachClass(clazz -> {
            if (type.isAssignableFrom(clazz) && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {

                if (!BlackOut.TYPE.isDevBuild() && clazz.isAnnotationPresent(bodevelopment.client.blackout.annotations.OnlyDev.class)) {
                    BOLogger.debug("Skipping @OnlyDev addon component: " + clazz.getName());
                    return;
                }

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

    /**
     * Internal bridge that listens to {@link GameJoinEvent} and delegates to
     * {@link BlackoutAddon#onWorldJoin()} / {@link BlackoutAddon#onWorldLeave()}.
     */
    private static class AddonLifecycleBridge {
        @Event
        public void onGameJoin(GameJoinEvent event) {
            if (anyAddonHadWorld) {
                for (BlackoutAddon addon : addons) {
                    try {
                        addon.onWorldLeave();
                    } catch (Exception e) {
                        BOLogger.error("Error in onWorldLeave for addon: " + addon.getName(), e);
                    }
                }
            }
            anyAddonHadWorld = true;
            for (BlackoutAddon addon : addons) {
                try {
                    addon.onWorldJoin();
                } catch (Exception e) {
                    BOLogger.error("Error in onWorldJoin for addon: " + addon.getName(), e);
                }
            }
        }
    }
}
