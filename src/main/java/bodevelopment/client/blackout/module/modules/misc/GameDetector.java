/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;
import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.util.SoundUtils;
import net.minecraft.world.item.Items;

public class GameDetector extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgDetect = this.addGroup("Detection Logic");

    public final Setting<Boolean> reEnable = this.sgGeneral.booleanSetting("Re-Enable on Match Start", false, "Automatically enables combat and utility modules when a new game session begins.");
    public final Setting<Boolean> disable = this.sgGeneral.booleanSetting("Disable on Match End", true, "Automatically disables modules when the game ends to prevent unintended behavior in lobbies.");

    public final Setting<Boolean> capabilities = this.sgDetect.booleanSetting("Check Abilities", true, "Detects lobby state by checking for flying or invulnerability permissions.");
    public final Setting<Boolean> compass = this.sgDetect.booleanSetting("Compass Detection", true, "Assumes lobby state if a compass (common server selector) is found in the inventory.");
    public final Setting<Boolean> slime = this.sgDetect.booleanSetting("Slime Ball Detection", true, "Assumes lobby state if a slime ball (common hide-player tool) is found in the inventory.");
    public final Setting<Boolean> test = this.sgDetect.booleanSetting("Force Lobby State", true, "Debug setting to manually override game detection and force a lobby state.");

    public boolean gameStarted = false;
    private boolean prevState = false;
    private boolean disabledAura = false;
    private boolean disabledStealer = false;
    private boolean disabledManager = false;

    public GameDetector() {
        super("Game Detector", "Analyzes environment and inventory state to automatically toggle modules between lobby and active gameplay.", SubCategory.MISC, true);
    }

    @Override
    public String getInfo() {
        return this.gameStarted ? "Started" : "Waiting for start";
    }

    @Override
    public void onDisable() {
        this.gameStarted = false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (BlackOut.mc.player.tickCount >= 20) {
                if (this.gameStarted != this.prevState) {
                    this.toggleModules(this.gameStarted);
                    this.prevState = this.gameStarted;
                }

                this.gameStarted = !this.capabilities.get()
                        || !BlackOut.mc.player.getAbilities().mayfly
                        && !BlackOut.mc.player.getAbilities().flying
                        && !BlackOut.mc.player.getAbilities().invulnerable;
                if (this.compass.get() && InvUtils.count(true, false, stack -> stack.getItem() == Items.COMPASS) > 0) {
                    this.gameStarted = false;
                }

                if (this.slime.get() && InvUtils.count(true, false, stack -> stack.getItem() == Items.SLIME_BALL) > 0) {
                    this.gameStarted = false;
                }

                if (this.test.get()) {
                    this.gameStarted = false;
                }
            }
        }
    }

    private void toggleModules(boolean enable) {
        Aura auraModule = Aura.getInstance();
        Stealer stealerModule = Stealer.getInstance();
        Manager managerModule = Manager.getInstance();
        if (enable && this.reEnable.get()) {
            if (this.disabledAura) {
                auraModule.silentEnable();
            }

            if (this.disabledStealer) {
                stealerModule.silentEnable();
            }

            if (this.disabledManager) {
                managerModule.silentEnable();
            }

            this.sendNotification(this.getDisplayName() + " enabled some modules", "Game start detected");
        } else if (this.disable.get()) {
            if (auraModule.enabled) {
                auraModule.silentDisable();
                this.disabledAura = true;
            }

            if (stealerModule.enabled) {
                stealerModule.silentDisable();
                this.disabledStealer = true;
            }

            if (managerModule.enabled) {
                managerModule.silentDisable();
                this.disabledManager = true;
            }

            this.sendNotification(this.getDisplayName() + " disabled some modules", "Game end detected");
        }
    }

    private void sendNotification(String message, String bigText) {
        NotificationsSettings notifications = NotificationsSettings.getInstance();
        if (notifications.chatNotifications.get()) {
            this.sendMessage(this.getDisplayName() + " " + message);
        }

        Managers.NOTIFICATIONS.addNotification(message == null ? "Disabled " + this.getDisplayName() : message, bigText, 5.0, NotificationsSettings.Type.Info);
        if (notifications.sound.get()) {
            SoundUtils.play(1.0F, 1.0F, "disable");
        }
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        this.gameStarted = false;
    }

    public boolean shouldHibernate() {
        return this.enabled && this.gameStarted;
    }
}
