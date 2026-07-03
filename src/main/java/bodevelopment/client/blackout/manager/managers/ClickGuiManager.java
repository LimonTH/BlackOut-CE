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

package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.PlayerUtils;

@PublicAPI
public class ClickGuiManager extends Manager {
    public final ClickGui CLICK_GUI = new ClickGui();

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
        BlackOut.EVENT_BUS.subscribe(this.CLICK_GUI, () -> false);
    }

    @Event
    public void onKey(KeyEvent event) {
        if (!event.pressed || !PlayerUtils.isInGame()) return;

        if (event.key == 344) {
            if (BlackOut.mc.screen == null || Managers.CLICK_GUI.CLICK_GUI.isOpen()) {
                this.toggle();
            }
        }
    }

    public void openScreen(ClickGuiScreen screen) {
        if (Managers.CLICK_GUI.CLICK_GUI.isOpen()) {
            this.CLICK_GUI.setScreen(screen);
        } else {
            Managers.HUD.HUD_EDITOR.setScreen(screen);
        }
    }

    private void toggle() {
        if (this.CLICK_GUI.isOpen()) {
            if (System.currentTimeMillis() - this.CLICK_GUI.toggleTime < 500L) return;

            this.CLICK_GUI.toggleTime = System.currentTimeMillis();
            this.CLICK_GUI.setOpen(false);
        } else {
            if (System.currentTimeMillis() - this.CLICK_GUI.toggleTime < 250L) return;

            this.CLICK_GUI.toggleTime = System.currentTimeMillis();
            this.CLICK_GUI.setOpen(true);
            this.CLICK_GUI.initGui();
            BlackOut.mc.setScreen(this.CLICK_GUI);
        }
    }
}