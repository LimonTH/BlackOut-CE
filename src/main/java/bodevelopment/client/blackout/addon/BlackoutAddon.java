package bodevelopment.client.blackout.addon;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.ParentCategory;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

public abstract class BlackoutAddon {
    private final String name;
    public final String modulePath;
    public final String commandPath;
    public final String hudPath;

    public final List<AbstractModule> modules = new ArrayList<>();
    public final List<Command> commands = new ArrayList<>();
    public final List<HudElement> hudElements = new ArrayList<>();

    private TextureRenderer iconRenderer;
    private BufferedImage pendingIcon;

    protected BlackoutAddon(String name, String modulePath, String commandPath, String hudPath) {
        this.name = name;
        this.modulePath = modulePath;
        this.commandPath = commandPath;
        this.hudPath = hudPath;
    }

    public abstract void onInitialize();

    public void onEnable() {}

    public void onDisable() {}

    public String getName() { return name; }
    public String getAuthor() { return "Limon_TH"; }
    public String getDescription() { return "A simple addon for BlackOut Client."; }
    public String getVersion() { return BlackOut.VERSION; }
    public String getUrl() { return null; }
    public String getMinClientVersion() { return null; }

    void loadIconFromMod(BufferedImage image) {
        this.pendingIcon = image;
    }

    public void uploadIcon() {
        if (iconRenderer == null && pendingIcon != null) {
            RenderSystem.assertOnRenderThread();
            iconRenderer = new TextureRenderer(name + "-icon");
            iconRenderer.load(BOTextures.upload(pendingIcon));
            pendingIcon = null;
        }
    }

    public TextureRenderer getIcon() {
        return iconRenderer;
    }

    protected SubCategory addSubCategory(String name, ParentCategory parent) {
        return new SubCategory(name, parent);
    }
}
