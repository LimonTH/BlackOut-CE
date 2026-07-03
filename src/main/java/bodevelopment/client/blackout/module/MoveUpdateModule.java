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

package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.util.MovementPrediction;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.world.phys.Vec3;

public class MoveUpdateModule extends Module {
    private Mode type = Mode.Normal;

    public MoveUpdateModule(String name, String description, SubCategory category) {
        super(name, description, category, true);
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        this.type = Mode.values()[(SettingUtils.grimMovement() ? 2 : 0) + (SettingUtils.grimPackets() ? 1 : 0)];
        this.preTick();
    }

    @Event
    public void onMovePost(MoveEvent.Post event) {
        this.postMove();
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        this.postTick();
    }

    protected void preTick() {
        switch (this.type) {
            case Full:
                this.update(true, false);
                this.spoofedCalc();
                break;
            case Movement:
                this.spoofedCalc();
                break;
            case Packet:
                this.update(true, false);
        }
    }

    protected void postMove() {
        switch (this.type) {
            case Packet:
            case Normal:
                this.update(false, true);
        }
    }

    protected void postTick() {
        switch (this.type) {
            case Movement:
            case Normal:
                this.update(true, false);
        }
    }

    protected void update(boolean allowAction, boolean fakePos) {
    }

    private void spoofedCalc() {
        if (BlackOut.mc.player != null) {
            Vec3 pos = BlackOut.mc.player.position();
            BlackOut.mc.player.setPos(MovementPrediction.predict(BlackOut.mc.player));
            this.update(false, true);
            BlackOut.mc.player.setPos(pos);
        }
    }

    private enum Mode {
        Normal,
        Packet,
        Movement,
        Full
    }
}
