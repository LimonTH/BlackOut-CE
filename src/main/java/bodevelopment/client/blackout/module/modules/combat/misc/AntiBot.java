package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class AntiBot extends Module {
    private static AntiBot INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<HandlingMode> mode = this.sgGeneral.enumSetting("How to handle bots", HandlingMode.Ignore,
            "What to do with detected bots. 'Ignore' keeps them in world but combat modules skip them, 'Remove' deletes them client-side.");
    public final Setting<Boolean> WD = this.sgGeneral.booleanSetting("Watchdog", true,
            "Specific check for Hypixel's Watchdog bots. Flags entities with invalid UUIDs.");
    public final Setting<Boolean> smart = this.sgGeneral.booleanSetting("Smart", false,
            "Advanced check based on entity age and distance to filter out instant-spawned anticheat bots.");
    private final Setting<Integer> range = this.sgGeneral.intSetting("Check Range", 24, 0, 64, 1,
            "The radius around you to perform smart bot checks.", this.smart::get);
    public final Setting<Boolean> inv = this.sgGeneral.booleanSetting("Invisible", false,
            "Automatically flags any invisible player as a bot. Use with caution around legit invisibility potions.");
    public final Setting<Boolean> nameCheck = this.sgGeneral.booleanSetting("Name Check", false,
            "Filters players by common NPC name patterns (e.g., '[NPC]', 'CIT-', or special symbols).");
    public final Setting<Boolean> bedWars = this.sgGeneral.booleanSetting("Bed Wars", false,
            "Specific filter for BedWars shop and upgrade NPCs.");
    public final Setting<Boolean> notif = this.sgGeneral.booleanSetting("Send Notification", false,
            "Shows a client-side notification whenever a bot is identified and added to the list.");
    public final Setting<Boolean> remove = this.sgGeneral.booleanSetting("Remove Notification", false,
            "Alerts you when an entity is no longer considered a bot.");

    private final List<AbstractClientPlayerEntity> bots = new ArrayList<>();
    private String info = "";

    public AntiBot() {
        super("Anti Bot", "Detects and filters out fake entities spawned by AntiCheats to protect your combat modules.", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static AntiBot getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.getInformation();
    }

    @Override
    public void onDisable() {
        this.bots.clear();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            OLEPOSSUtils.limitList(this.bots, 100);
            BlackOut.mc
                    .world
                    .getPlayers()
                    .forEach(
                            player -> {
                                if (this.WD.get()) {
                                    if (player.getUuid() == null) {
                                        this.addBot(player);
                                    } else {
                                        this.removeBot(player);
                                    }

                                    this.info = this.WD.name;
                                }

                                if (this.smart.get()) {
                                    if (player.age < 10
                                            && BlackOut.mc.player.age > 10
                                            && BlackOut.mc.player.distanceTo(player) < this.range.get()
                                            && player != BlackOut.mc.player) {
                                        this.addBot(player);
                                    }

                                    if (BlackOut.mc.player.distanceTo(player) > this.range.get()) {
                                        this.removeBot(player);
                                    }

                                    this.info = this.smart.name;
                                }

                                if (this.inv.get()) {
                                    if (player.isInvisible()) {
                                        this.bots.add(player);
                                    } else {
                                        this.removeBot(player);
                                    }

                                    this.info = this.inv.name;
                                }

                                if (this.nameCheck.get()) {
                                    if (player.getName().getString().contains("[NPC]")
                                            || player.getName().getString().contains("§")
                                            || player.getName().getString().contains("CIT-")) {
                                        this.addBot(player);
                                    }

                                    this.info = this.nameCheck.name;
                                }

                                if (this.bedWars.get()) {
                                    if (player.getName().getString().contains("SHOP") || player.getName().getString().contains("UPGRADES")) {
                                        this.addBot(player);
                                    }

                                    this.info = this.bedWars.name;
                                }
                            }
                    );
            if (this.mode.get() == HandlingMode.Remove) {
                this.getBots().forEach(bot -> BlackOut.mc.world.removeEntity(bot.getId(), Entity.RemovalReason.DISCARDED));
            }
        }
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        this.bots.clear();
    }

    private void addBot(AbstractClientPlayerEntity bot) {
        if (!this.bots.contains(bot)) {
            this.bots.add(bot);
            if (this.notif.get()) {
                Managers.NOTIFICATIONS
                        .addNotification(bot.getName().getString() + " has been flagged as a bot!", this.getDisplayName(), 2.0, Notifications.Type.Info);
            }
        }
    }

    private void removeBot(AbstractClientPlayerEntity bot) {
        this.bots.remove(bot);
        if (this.notif.get() && this.remove.get()) {
            Managers.NOTIFICATIONS.addNotification(bot.getName().getString() + " was set as a player!", this.getDisplayName(), 2.0, Notifications.Type.Info);
        }
    }

    public List<AbstractClientPlayerEntity> getBots() {
        return this.bots;
    }

    private String getInformation() {
        return this.getEnabled() == 1 ? this.info + " " + this.bots.size() : this.mode.get().name() + " " + (this.bots.isEmpty() ? "0" : this.bots.size());
    }

    private int getEnabled() {
        int i = 0;
        if (this.WD.get()) {
            i++;
        }

        if (this.inv.get()) {
            i++;
        }

        if (this.nameCheck.get()) {
            i++;
        }

        if (this.smart.get()) {
            i++;
        }

        if (this.bedWars.get()) {
            i++;
        }

        return i;
    }

    public enum HandlingMode {
        Remove,
        Ignore
    }
}
