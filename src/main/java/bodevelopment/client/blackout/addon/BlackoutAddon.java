package bodevelopment.client.blackout.addon;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.ParentCategory;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.MenuMusicSettings;
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;
import bodevelopment.client.blackout.randomstuff.mainmenu.MainMenuRenderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.theme.Theme;
import bodevelopment.client.blackout.util.SoundUtils;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@PublicAPI
public abstract class BlackoutAddon {
    /**
     * Current addon API version, sourced from {@code gradle.properties → api_version}.
     */
    public static final int API_VERSION = BlackOut.API_VERSION;
    public final String modulePath;
    public final String commandPath;
    public final String hudPath;
    public final String themePath;
    public final String soundPath;
    public final String guiPath;

    public final List<AbstractModule> modules = new ArrayList<>();
    public final List<Command> commands = new ArrayList<>();
    public final List<HudElement> hudElements = new ArrayList<>();
    public final List<Theme> themes = new ArrayList<>();
    public final Map<String, MainMenuRenderer> menuRenderers = new LinkedHashMap<>();
    public final Map<String, MenuMusicSettings.TrackProvider> musicTracks = new LinkedHashMap<>();
    public final List<ClickGuiScreen> guiScreens = new ArrayList<>();
    private final String name;
    private TextureRenderer iconRenderer;
    private BufferedImage pendingIcon;

    /**
     * Backward-compatible constructor for addons that only provide modules, commands and HUD elements.
     */
    protected BlackoutAddon(String name, String modulePath, String commandPath, String hudPath) {
        this(name, modulePath, commandPath, hudPath, null, null, null);
    }

    /**
     * Full constructor exposing all extension points.
     *
     * @param name        Addon display name.
     * @param modulePath  Package path scanned for {@link AbstractModule} subclasses, or null.
     * @param commandPath Package path scanned for {@link Command} subclasses, or null.
     * @param hudPath     Package path scanned for {@link HudElement} subclasses, or null.
     * @param themePath   Package path scanned for {@link Theme} enum constants, or null.
     * @param soundPath   Resource path prefix for addon-owned {@code .ogg} sounds, or null.
     * @param guiPath     Package path scanned for {@link ClickGuiScreen} and {@link MainMenuRenderer} implementations, or null.
     */
    protected BlackoutAddon(String name, String modulePath, String commandPath, String hudPath,
                            String themePath, String soundPath, String guiPath) {
        this.name = name;
        this.modulePath = modulePath;
        this.commandPath = commandPath;
        this.hudPath = hudPath;
        this.themePath = themePath;
        this.soundPath = soundPath;
        this.guiPath = guiPath;
    }

    /**
     * Called once during addon discovery, before any component scanning.
     */
    public abstract void onInitialize();

    /**
     * Called after all components have been registered.
     */
    public void onEnable() {
    }

    /**
     * Called when the client shuts down or addons are unloaded.
     */
    public void onDisable() {
    }

    /**
     * Called when the local player joins a world (null-safe).
     */
    public void onWorldJoin() {
    }

    /**
     * Called when the local player leaves a world (null-safe).
     */
    public void onWorldLeave() {
    }

    public String getName() {
        return name;
    }

    /**
     * Addon author shown in the addon list. Override to customise.
     */
    public String getAuthor() {
        return "Limon_TH";
    }

    /**
     * Short description shown in the addon list. Override to customise.
     */
    public String getDescription() {
        return "A simple addon for BlackOut Client.";
    }

    /**
     * Addon version string (informational). Defaults to the client version the addon was built against.
     */
    public String getVersion() {
        return BlackOut.VERSION;
    }

    /**
     * Optional URL (GitHub, website, etc.) shown in the addon list.
     */
    public String getUrl() {
        return null;
    }

    /**
     * Optional minimum BlackOut client (mod) version required.
     *
     * <p>This is a <b>secondary</b> compatibility gate. Use it only when the addon
     * depends on a specific client behaviour (e.g. a rendering fix, a non-API internal
     * change) that is not captured by {@link #getApiVersion()}.</p>
     *
     * <p>The primary compatibility contract is <b>API version</b> ({@link #getApiVersion()}).</p>
     *
     * <p>Format: dot-separated numeric, e.g. {@code "2.2"}, {@code "2.3.1"}.
     * Returns {@code null} (no restriction) by default.</p>
     */
    public String getMinClientVersion() {
        return null;
    }

    /**
     * API contract version this addon was compiled against.
     *
     * <p>This is the <b>primary</b> compatibility gate:</p>
     * <ul>
     *   <li>If the addon's API version is <b>higher</b> than the client's —
     *       <b>hard reject</b> (addon uses newer symbols).</li>
     *   <li>If the addon's API version is <b>lower</b> than the client's —
     *       <b>soft warning</b> (deprecated symbols may have been removed).</li>
     *   <li>If equal — <b>exact match</b>, guaranteed compatible.</li>
     * </ul>
     *
     * <p>Bump {@code api_version} in {@code gradle.properties} on breaking changes
     * to the {@code @PublicAPI} surface.</p>
     */
    public int getApiVersion() {
        return BlackOut.API_VERSION;
    }

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

    /**
     * Creates a new {@link SubCategory} under the given parent.
     */
    protected SubCategory addSubCategory(String name, ParentCategory parent) {
        return new SubCategory(name, parent);
    }

    /**
     * Registers a custom {@link Theme} that will appear in the Theme selector.
     * Call from {@link #onInitialize()}.
     */
    protected void registerTheme(Theme theme) {
        this.themes.add(theme);
    }

    /**
     * Registers a custom {@link MainMenuRenderer} that users can select via
     * {@code MainMenuSettings → Mode → Custom}.
     * Call from {@link #onInitialize()}.
     */
    protected void registerMainMenuRenderer(String id, MainMenuRenderer renderer) {
        this.menuRenderers.put(id, renderer);
    }

    /**
     * Registers a custom music track that will appear in the Menu Music selector
     * when the user selects {@code Track → Custom}.
     * Call from {@link #onInitialize()}.
     *
     * @param name     Display name shown in the {@code Custom Track Name} setting.
     * @param provider Supplier that opens a fresh OGG Vorbis {@link InputStream} on each play.
     */
    protected void registerMusicTrack(String name, MenuMusicSettings.TrackProvider provider) {
        this.musicTracks.put(name, provider);
    }

    /**
     * Plays an OGG sound from this addon's own resources.
     * The path is resolved relative to {@link #soundPath}.
     *
     * @param name   File name without the {@code .ogg} extension.
     * @param pitch  Playback pitch (1.0 = normal).
     * @param volume Playback volume (0.0–1.0+).
     */
    protected void playSound(String name, float pitch, float volume) {
        String fullPath = (soundPath != null ? soundPath + "/" : "") + name + ".ogg";
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fullPath);
        if (stream != null) {
            SoundUtils.playStream(pitch, volume, stream);
        }
    }

    /**
     * Convenience wrapper around {@link Managers#NOTIFICATIONS} for addons.
     */
    protected void sendNotification(String text, String bigText, double timeSeconds, NotificationsSettings.Type type) {
        Managers.NOTIFICATIONS.addNotification(text, bigText, timeSeconds, type);
    }

    public ClassLoader getAddonClassLoader() {
        return getClass().getClassLoader();
    }
}
