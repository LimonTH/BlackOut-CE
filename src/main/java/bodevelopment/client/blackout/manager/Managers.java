package bodevelopment.client.blackout.manager;

import bodevelopment.client.blackout.manager.managers.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;

public class Managers {
    public static final AltManager ALT = new AltManager();
    public static final BlockManager BLOCK = new BlockManager();
    public static final ClickGuiManager CLICK_GUI = new ClickGuiManager();
    public static final CommandManager COMMANDS = new CommandManager();
    public static final ConfigManager CONFIG = new ConfigManager();
    public static final EntityManager ENTITY = new EntityManager();
    public static final ExtrapolationManager EXTRAPOLATION = new ExtrapolationManager();
    public static final FakePlayerManager FAKE_PLAYER = new FakePlayerManager();
    public static final FrameBufferManager FRAME_BUFFER = new FrameBufferManager();
    public static final FriendsManager FRIENDS = new FriendsManager();
    public static final HUDManager HUD = new HUDManager();
    public static final ModuleManager MODULES = new ModuleManager();
    public static final NotificationManager NOTIFICATIONS = new NotificationManager();
    public static final PacketManager PACKET = new PacketManager();
    public static final ParticleManager PARTICLE = new ParticleManager();
    public static final PingManager PING = new PingManager();
    public static final RotationManager ROTATION = new RotationManager();
    public static final StatsManager STATS = new StatsManager();
    public static final TPSManager TPS = new TPSManager();
    public static final UtilsManager UTILS = new UtilsManager();

    /**
     * Initializes all managers in dependency order.
     * <p>
     * <b>Dependency chain:</b>
     * <ol>
     *   <li>{@link ConfigManager} — file system init (must be first)</li>
     *   <li>{@link ModuleManager} — loads modules before other managers</li>
     *   <li>{@link PacketManager}, {@link RotationManager} — core systems</li>
     *   <li>All other managers — order-independent</li>
     * </ol>
     * Phase 1 ({@code init}): one-time setup, event subscriptions, loading.<br>
     * Phase 2 ({@code postInit}): cross-manager wiring (requires all inited).
     * <p>
     * Uses reflection via field declaration order as a fallback;
     * the JLS does not guarantee field order but HotSpot preserves it.
     */
    public static void init() {
        forEach(field -> {
            try {
                Object value = field.get(null);
                if (value instanceof Manager manager) {
                    manager.init();
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        forEach(field -> {
            try {
                Object value = field.get(null);
                if (value instanceof Manager manager) {
                    manager.postInit();
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void forEach(Consumer<? super Field> consumer) {
        Arrays.stream(Managers.class.getDeclaredFields())
                .filter(field -> java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                .forEach(consumer);
    }
}
