package bodevelopment.client.blackout;

import bodevelopment.client.blackout.addon.AddonLoader;
import bodevelopment.client.blackout.event.EventBus;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.RegistryNames;
import bodevelopment.client.blackout.rendering.font.CustomFontRenderer;
import bodevelopment.client.blackout.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;

public final class BlackOut extends bodevelopment.client.blackout.BlackOutInfo implements ClientModInitializer {
    public static final String NAME = bodevelopment.client.blackout.BlackOutInfo.NAME;
    public static final String VERSION = bodevelopment.client.blackout.BlackOutInfo.VERSION;
    /**
     * Current BlackOut addon API version. Addons requiring a higher version are rejected.
     */
    public static final Integer API_VERSION = bodevelopment.client.blackout.BlackOutInfo.API_VERSION;

    public static final Type TYPE = Type.Beta;
    public static final Color TYPECOLOR = TYPE.getColor();
    public static final Minecraft mc = Minecraft.getInstance();
    public static final File RUN_DIRECTORY = mc.gameDirectory;
    public static final EventBus EVENT_BUS = new EventBus();

    public static final CustomFontRenderer FONT = new CustomFontRenderer("ubuntu");
    public static final CustomFontRenderer BOLD_FONT = new CustomFontRenderer("ubuntu-bold");

    public void onInitializeClient() {
        MainMenu.init();
        EnchantmentNames.init();
        ClassUtils.init();
        FileUtils.init();
        AddonLoader.load();
        Managers.init();
        Managers.CONFIG.readConfigs();
        Managers.CLICK_GUI.CLICK_GUI.initGui();
        RegistryNames.init();
        BlocklistUtil.loadBlocklist();
        validateAccessWidenerTargets();
    }

    /**
     * Validates that critical access widener targets are accessible at runtime.
     * If a Minecraft update removed a field/method, this logs a warning instead
     * of crashing with an inscrutable {@link NoSuchFieldError} later.
     */
    private static void validateAccessWidenerTargets() {
        String[] criticalTargets = {
                "net.minecraft.client.multiplayer.ClientLevel::getBlockStatePredictionHandler",
                "net.minecraft.client.multiplayer.MultiPlayerGameMode::startPrediction",
                "net.minecraft.client.multiplayer.MultiPlayerGameMode::carriedIndex",
                "net.minecraft.world.entity.LivingEntity::attackStrengthTicker",
        };
        for (String target : criticalTargets) {
            try {
                String[] parts = target.split("::");
                Class<?> clazz = Class.forName(parts[0]);
                String member = parts[1];
                try {
                    clazz.getDeclaredField(member);
                } catch (NoSuchFieldException e) {
                    boolean found = false;
                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.getName().equals(member)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        BOLogger.warn(
                                "Access widener target may be missing: " + target);
                    }
                }
            } catch (ClassNotFoundException e) {
                BOLogger.warn(
                        "Access widener target class missing: " + target);
            }
        }
    }

    public enum Type {
        Dev(new Color(0, 175, 0, 255)),
        Beta(new Color(150, 150, 255, 255)),
        Release(new Color(255, 0, 0, 255));

        private final Color color;

        Type(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return this.color;
        }

        public boolean isDevBuild() {
            return this == Dev;
        }
    }
}
