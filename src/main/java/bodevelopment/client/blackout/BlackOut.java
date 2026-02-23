package bodevelopment.client.blackout;

import bodevelopment.client.blackout.addon.AddonLoader;
import bodevelopment.client.blackout.event.EventBus;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.RegistryNames;
import bodevelopment.client.blackout.rendering.font.CustomFontRenderer;
import bodevelopment.client.blackout.util.ClassUtils;
import bodevelopment.client.blackout.util.EnchantmentNames;
import bodevelopment.client.blackout.util.FileUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.awt.*;
import java.io.File;

public class BlackOut implements ClientModInitializer {
    public static final String NAME = "BlackOut";
    public static final String VERSION = "2.2.2";
    public static final Type TYPE = Type.Dev;
    public static final Color TYPECOLOR = TYPE.getColor();
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final File RUN_DIRECTORY = mc.runDirectory;
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
