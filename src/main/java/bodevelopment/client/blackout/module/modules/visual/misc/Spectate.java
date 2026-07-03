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

package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.util.ScreenUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Spectate extends Module {
    private static Spectate INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> ignoreFriends = this.sgGeneral.booleanSetting("Exclude Friends", true, "Prevents the spectator cycle from including players on your friend list.");
    private final Setting<KeyBind> forwardKey = this.sgGeneral.keySetting("Next Target", "The hotkey used to cycle to the next available player in the sequence.");
    private final Setting<KeyBind> backKey = this.sgGeneral.keySetting("Previous Target", "The hotkey used to cycle to the previous player in the sequence.");

    private final List<Player> playerEntities = new ArrayList<>();
    private final PoseStack stack = new PoseStack();
    private Player target;
    private int prevI = 0;

    public Spectate() {
        super("Spectate", "Allows you to view the world from the perspective of other players without changing your actual world position.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static Spectate getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.Hud.Pre event) {
        if (PlayerUtils.isInGame()) {
            ScreenUtils.beginPixelSpace(this.stack);
            if (this.target instanceof AbstractClientPlayer) {
                BlackOut.FONT
                        .text(
                                this.stack,
                                "Spectating " + this.target.getName().getString(),
                                2.0F,
                                ScreenUtils.screenWidth() / 2.0F,
                                ScreenUtils.screenHeight() / 2.0F + BlackOut.FONT.getHeight() * 3.0F,
                                Color.WHITE,
                                true,
                                true
                        );
            }

            ScreenUtils.endPixelSpace(this.stack);
        }
    }

    @Event
    public void onKey(KeyEvent event) {
        if (event.pressed) {
            if (this.forwardKey.get().isKey(event.key)) {
                this.set(this.move(true));
            }

            if (this.backKey.get().isKey(event.key)) {
                this.set(this.move(false));
            }
        }
    }

    @Event
    public void onMouse(MouseButtonEvent event) {
        if (event.pressed) {
            if (this.forwardKey.get().isMouse(event.button)) {
                this.set(this.move(true));
            }

            if (this.backKey.get().isMouse(event.button)) {
                this.set(this.move(false));
            }
        }
    }

    public Entity getEntity() {
        this.updateList();
        if (!this.playerEntities.contains(this.target)) {
            this.set(this.move(false));
        } else {
            this.prevI = this.playerEntities.indexOf(this.target);
        }

        return this.target;
    }

    private void set(int i) {
        if (this.playerEntities.isEmpty()) {
            this.prevI = 0;
            this.target = BlackOut.mc.player;
        } else {
            this.prevI = i;
            this.target = this.playerEntities.get(i);
        }
    }

    private int move(boolean increase) {
        int max = this.playerEntities.size() - 1;
        if (increase) {
            return this.prevI == max ? 0 : this.prevI + 1;
        } else {
            return this.prevI == 0 ? max : this.prevI - 1;
        }
    }

    private void updateList() {
        this.playerEntities.clear();

        for (Player player : BlackOut.mc.level.players()) {
            if (player != BlackOut.mc.player && (!this.ignoreFriends.get() || !Managers.FRIENDS.isFriend(player))) {
                this.playerEntities.add(player);
            }
        }
    }
}
