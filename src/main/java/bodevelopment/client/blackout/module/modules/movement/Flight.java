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

package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.MovementController;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;

public class Flight extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Flight Mode", Mode.Motion, "The bypass logic used to maintain altitude.");
    private final Setting<Double> h = this.sgGeneral.doubleSetting("Horizontal Speed", 0.5, 0.0, 10.0, 0.05, "The lateral travel velocity.", () -> this.mode.get() == Mode.Motion);
    private final Setting<Double> v = this.sgGeneral.doubleSetting("Vertical Speed", 0.5, 0.0, 10.0, 0.05, "The ascent and descent velocity.", () -> this.mode.get() == Mode.Motion);
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer Multiplier", 1.0, 0.05, 10.0, 0.05, "Speeds up the flight by increasing the client-side game speed.", () -> this.mode.get() == Mode.Motion);
    private final Setting<Boolean> antiKick = this.sgGeneral.booleanSetting("Anti-Kick", true, "Periodically lowers the player slightly to prevent being kicked by the server's flight check.", () -> this.mode.get() == Mode.Motion);
    public final Setting<Integer> delay = this.sgGeneral.intSetting("Anti-Kick Interval", 2, 0, 20, 1, "The frequency of the anti-kick downward movement (in ticks).", () -> this.mode.get() == Mode.Motion && this.antiKick.get());
    private final Setting<Double> verusSpeed = this.sgGeneral.doubleSetting("Verus Velocity", 0.4, 0.0, 1.0, 0.01, "Movement speed for the Verus anti-cheat bypass.", () -> this.mode.get() == Mode.Verus);
    private final Setting<Integer> verusTicks = this.sgGeneral.intSetting("Jump Interval", 1, 0, 50, 1, "The tick delay between automated jumps in Verus mode.", () -> this.mode.get() == Mode.Verus);
    private final Setting<FallMode> fallMode = this.sgGeneral.enumSetting("Descent Logic", FallMode.Smart, "The method used to calculate downward motion when falling in Verus mode.", () -> this.mode.get() == Mode.Verus);
    private final Setting<Double> verusBowSpeed = this.sgGeneral.doubleSetting("Launch Speed", 5.0, 0.0, 10.0, 0.1, "The initial speed boost gained after taking bow damage.", () -> this.mode.get() == Mode.VerusBow);
    private final Setting<Double> verusLimit = this.sgGeneral.doubleSetting("Effect Duration", 20.0, 0.0, 100.0, 1.0, "How long the bow-boosted flight lasts in ticks.", () -> this.mode.get() == Mode.VerusBow);
    private final Setting<Double> verusDMGSpeed = this.sgGeneral.doubleSetting("Combat Speed", 9.95, 0.0, 10.0, 0.05, "The travel speed after taking self-damage.", () -> this.mode.get() == Mode.VerusDMG);
    private final Setting<Double> verusDMGheight = this.sgGeneral.doubleSetting("Packet Height", 3.05, 3.05, 10.0, 0.05, "The simulated height used to trigger self-damage via packets.", () -> this.mode.get() == Mode.VerusDMG);
    private final Setting<Double> verusDMGLimit = this.sgGeneral.doubleSetting("Sustain Ticks", 20.0, 0.0, 100.0, 1.0, "The duration of the damage-based flight.", () -> this.mode.get() == Mode.VerusDMG);

    /**
     * Vanilla anti-kick offset: the minimal downward velocity to satisfy the server's movement check.
     */
    private static final double ANTI_KICK_OFFSET = -0.0315;
    /**
     * Vanilla walking speed constant (blocks/tick).
     */
    private static final double VANILLA_WALK_SPEED = 0.2873;
    private double startY = 0.0;
    private boolean changedTimer = false;
    private boolean damaged = false;
    private int i = 0;
    private int dmgFlyTicks = 0;
    private int ticks = 0;
    private boolean jumped = false;
    private int verusDmgPhase = -1;

    public Flight() {
        super("Flight", "Overrides gravity and air friction to allow the player to travel through the air freely.", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onEnable() {
        ticks = 0;
        dmgFlyTicks = 0;
        jumped = false;
        this.damaged = false;
        this.verusDmgPhase = -1;
        this.startY = BlackOut.mc.player.getY();
        if (this.mode.get() == Mode.VerusBow) {
            Managers.NOTIFICATIONS.addNotification("Shoot yourself with a bow", this.getDisplayName(), 2.0, NotificationsSettings.Type.Info);
        }

        if (this.mode.get() == Mode.Verus) {
            Managers.NOTIFICATIONS.addNotification("Hold blocks in your hand to prevent flagging", this.getDisplayName(), 2.0, NotificationsSettings.Type.Info);
        }

        if (this.mode.get() == Mode.VerusDMG) {
            // Phase 0 sends the first packet immediately; remaining phases spread across ticks
            this.verusDmgPhase = 0;
        }
    }

    /**
     * Spreads the 4 VerusDMG position packets across 4 ticks to avoid Grim's
     * multi-position-per-tick detection. One packet is sent per tick via onTick.
     */
    private void tickVerusDmgPhase() {
        if (this.verusDmgPhase < 0 || this.verusDmgPhase > 3) return;
        double px = BlackOut.mc.player.getX();
        double py = BlackOut.mc.player.getY();
        double pz = BlackOut.mc.player.getZ();
        boolean hCol = BlackOut.mc.player.horizontalCollision;

        switch (this.verusDmgPhase) {
            case 0 -> this.sendPacket(
                    new ServerboundMovePlayerPacket.Pos(px, py, pz, false, hCol));
            case 1 -> this.sendPacket(
                    new ServerboundMovePlayerPacket.Pos(px, py + this.verusDMGheight.get(), pz, false, hCol));
            case 2 -> this.sendPacket(
                    new ServerboundMovePlayerPacket.Pos(px, py, pz, false, hCol));
            case 3 -> this.sendPacket(
                    new ServerboundMovePlayerPacket.Pos(px, py, pz, true, hCol));
        }
        this.verusDmgPhase++;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (jumped) {
                ticks++;
            }

            if (this.enabled && this.timer.get() != 1.0 && this.mode.get() == Mode.Motion) {
                Timer.set(this.timer.get().floatValue());
                this.changedTimer = true;
            }

            if (this.mode.get() == Mode.VerusDMG) {
                this.tickVerusDmgPhase();
                if (this.damaged) {
                    this.changedTimer = true;
                    dmgFlyTicks++;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.changedTimer) {
            Timer.reset();
            this.changedTimer = false;
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            i++;
            double y = 0.0;
            switch (this.mode.get()) {
                case Motion:
                    if (BlackOut.mc.options.keyJump.isDown()) {
                        y = this.v.get();
                    } else if (BlackOut.mc.options.keyShift.isDown() && !BlackOut.mc.player.onGround()) {
                        y = -this.v.get();
                    }

                    if (this.antiKick.get()) {
                        double adjustedY = MovementController.applyAntiKick(i, this.delay.get(), y);
                        if (adjustedY != y) {
                            y = adjustedY;
                            i = 0;
                        }
                    }

                    event.setY(this, y);
                    if (!Managers.ROTATION.move) {
                        return;
                    }

                    MovementController.applyHorizontalMotion(event, this, this.h.get());
                    break;
                case Verus:
                    if (BlackOut.mc.player.getY() == this.startY) {
                        if (this.jumping()) {
                            this.jump();
                            this.startY++;
                        } else if (this.sneaking()) {
                            this.jump();
                            this.startY--;
                        } else if (++ticks > this.verusTicks.get()) {
                            this.jump();
                        } else {
                            event.setY(this, 0.0);
                        }
                    } else {
                        ticks = 0;
                        if (event.originalMovement.y < 0.0 && BlackOut.mc.player.getY() > this.startY && !this.jumping() && !this.sneaking()) {
                            switch (this.fallMode.get()) {
                                case Slow:
                                    event.setY(this, -0.1);
                                    break;
                                case VerySlow:
                                    event.setY(this, -0.001);
                                    break;
                                case Smart:
                                    event.setY(this, -0.3 * Math.pow(Math.abs(BlackOut.mc.player.getY() - this.startY), 2.0));
                            }
                        }

                        if (BlackOut.mc.player.getY() + event.movement.y <= this.startY) {
                            event.setY(this, this.startY - BlackOut.mc.player.getY());
                            Managers.PACKET.spoofOG(true);
                        }
                    }

                    BlockPos pos = BlackOut.mc.player.blockPosition();
                    if (!BlackOut.mc.player.onGround()) {
                        this.placeBlock(InteractionHand.MAIN_HAND, pos.getCenter(), Direction.UP, pos);
                    }

                    if (!Managers.ROTATION.move) {
                        return;
                    }

                    MovementController.applyHorizontalMotion(event, this, this.verusSpeed.get());
                    break;
                case VerusBow:
                    if (BlackOut.mc.player.hurtTime > 0) {
                        if (!jumped) {
                            this.startY = BlackOut.mc.player.getY();
                        }

                        jumped = true;
                        MovementController.applyHorizontalMotion(event, this, this.verusBowSpeed.get());
                        if (BlackOut.mc.player.getY() + event.originalMovement.y < this.startY) {
                            event.setY(this, this.startY - BlackOut.mc.player.getY());
                            Managers.PACKET.spoofOG(true);
                        }
                    }

                    if (ticks >= this.verusLimit.get()) {
                        MovementController.applyHorizontalMotion(event, this, MovementController.getVanillaWalkSpeed());
                        Managers.NOTIFICATIONS.addNotification("Reached tick limit", this.getDisplayName(), 2.0, NotificationsSettings.Type.Info);
                        this.toggle();
                    }
                    break;
                case VerusDMG:
                    if (BlackOut.mc.player.hurtTime > 0) {
                        this.damaged = true;
                    }

                    if (!this.damaged) {
                        return;
                    }

                    event.setY(this, 0.0);
                    MovementController.applyHorizontalMotion(event, this, this.verusDMGSpeed.get());
                    Timer.set(0.1F);
                    if (dmgFlyTicks > this.verusDMGLimit.get()) {
                        MovementController.applyHorizontalMotion(event, this, MovementController.getVanillaWalkSpeed());
                        Timer.reset();
                        this.disable();
                    }
            }
        }
    }

    private void jump() {
        BlackOut.mc.player.jumpFromGround();
    }

    private boolean jumping() {
        return BlackOut.mc.options.keyJump.isDown();
    }

    private boolean sneaking() {
        return BlackOut.mc.options.keyShift.isDown();
    }

    public enum FallMode {
        Vanilla,
        Slow,
        VerySlow,
        Smart
    }

    public enum Mode {
        Motion,
        Verus,
        VerusBow,
        VerusDMG
    }
}
