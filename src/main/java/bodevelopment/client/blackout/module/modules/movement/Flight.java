package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.MovementUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Flight extends Module {
    private static int i = 0;
    private static int dmgFlyTicks = 0;
    private static int ticks = 0;
    private static boolean jumped = false;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Motion, ".", () -> true);
    private final Setting<Double> h = this.sgGeneral.doubleSetting("Horizontal", 0.5, 0.0, 10.0, 0.05, ".", () -> this.mode.get() == Mode.Motion);
    private final Setting<Double> v = this.sgGeneral.doubleSetting("Vertical", 0.5, 0.0, 10.0, 0.05, ".", () -> this.mode.get() == Mode.Motion);
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer", 1.0, 0.05, 10.0, 0.05, ".", () -> this.mode.get() == Mode.Motion);
    private final Setting<Boolean> antiKick = this.sgGeneral.booleanSetting("Anti Kick", true, ".", () -> this.mode.get() == Mode.Motion);
    public final Setting<Integer> delay = this.sgGeneral
            .intSetting("Anti Kick delay", 2, 0, 20, 1, ".", () -> this.mode.get() == Mode.Motion && this.antiKick.get());
    private final Setting<Double> verusSpeed = this.sgGeneral.doubleSetting("Verus Speed", 0.4, 0.0, 1.0, 0.01, ".", () -> this.mode.get() == Mode.Verus);
    private final Setting<Integer> verusTicks = this.sgGeneral.intSetting("Verus Ticks", 1, 0, 50, 1, ".", () -> this.mode.get() == Mode.Verus);
    private final Setting<FallMode> fallMode = this.sgGeneral.enumSetting("Fall Mode", FallMode.Smart, ".", () -> this.mode.get() == Mode.Verus);
    private final Setting<Double> verusBowSpeed = this.sgGeneral.doubleSetting("Bow Speed", 5.0, 0.0, 10.0, 0.1, ".", () -> this.mode.get() == Mode.VerusBow);
    private final Setting<Double> verusLimit = this.sgGeneral.doubleSetting("Tick Limit", 20.0, 0.0, 100.0, 1.0, ".", () -> this.mode.get() == Mode.VerusBow);
    private final Setting<Double> verusDMGSpeed = this.sgGeneral.doubleSetting("Damage Speed", 9.95, 0.0, 10.0, 0.05, ".", () -> this.mode.get() == Mode.VerusDMG);
    private final Setting<Double> verusDMGheight = this.sgGeneral.doubleSetting("DMG height", 3.05, 3.05, 10.0, 0.05, ".", () -> this.mode.get() == Mode.VerusDMG);
    private final Setting<Double> verusDMGLimit = this.sgGeneral.doubleSetting("Fly ticks", 20.0, 0.0, 100.0, 1.0, ".", () -> this.mode.get() == Mode.VerusDMG);
    private double startY = 0.0;
    private boolean changedTimer = false;
    private boolean damaged = false;

    public Flight() {
        super("Flight", "Flies", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onEnable() {
        ticks = 0;
        dmgFlyTicks = 0;
        jumped = false;
        this.damaged = false;
        this.startY = BlackOut.mc.player.getY();
        if (this.mode.get() == Mode.VerusBow) {
            Managers.NOTIFICATIONS.addNotification("Shoot yourself with a bow", this.getDisplayName(), 2.0, Notifications.Type.Info);
        }

        if (this.mode.get() == Mode.Verus) {
            Managers.NOTIFICATIONS.addNotification("Hold blocks in your hand to prevent flagging", this.getDisplayName(), 2.0, Notifications.Type.Info);
        }

        if (this.mode.get() == Mode.VerusDMG) {
            this.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(BlackOut.mc.player.getX(), BlackOut.mc.player.getY(), BlackOut.mc.player.getZ(), false)
            );
            this.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(
                            BlackOut.mc.player.getX(),
                            BlackOut.mc.player.getY() + this.verusDMGheight.get(),
                            BlackOut.mc.player.getZ(),
                            false
                    )
            );
            this.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(BlackOut.mc.player.getX(), BlackOut.mc.player.getY(), BlackOut.mc.player.getZ(), false)
            );
            this.sendPacket(
                    new PlayerMoveC2SPacket.PositionAndOnGround(BlackOut.mc.player.getX(), BlackOut.mc.player.getY(), BlackOut.mc.player.getZ(), true)
            );
        }
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (jumped) {
                ticks++;
            }

            if (this.enabled && this.timer.get() != 1.0 && this.mode.get() == Mode.Motion) {
                Timer.set(this.timer.get().floatValue());
                this.changedTimer = true;
            }

            if (this.mode.get() == Mode.VerusDMG && this.damaged) {
                this.changedTimer = true;
                dmgFlyTicks++;
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
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            i++;
            double y = 0.0;
            switch (this.mode.get()) {
                case Motion:
                    if (BlackOut.mc.options.jumpKey.isPressed()) {
                        y = this.v.get();
                    } else if (BlackOut.mc.options.sneakKey.isPressed() && !BlackOut.mc.player.isOnGround()) {
                        y = -this.v.get();
                    }

                    if (this.antiKick.get() && i > this.delay.get()) {
                        y = Math.min(y, -0.0315);
                        i = 0;
                    }

                    event.setY(this, y);
                    if (!Managers.ROTATION.move) {
                        return;
                    }

                    event.setXZ(
                            this, MovementUtils.xMovement(this.h.get(), Managers.ROTATION.moveYaw), MovementUtils.zMovement(this.h.get(), Managers.ROTATION.moveYaw)
                    );
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

                    BlockPos pos = BlackOut.mc.player.getBlockPos();
                    if (!BlackOut.mc.player.isOnGround()) {
                        this.placeBlock(Hand.MAIN_HAND, pos.toCenterPos(), Direction.UP, pos);
                    }

                    if (!Managers.ROTATION.move) {
                        return;
                    }

                    event.setXZ(
                            this,
                            MovementUtils.xMovement(this.verusSpeed.get(), Managers.ROTATION.moveYaw),
                            MovementUtils.zMovement(this.verusSpeed.get(), Managers.ROTATION.moveYaw)
                    );
                    break;
                case VerusBow:
                    if (BlackOut.mc.player.hurtTime > 0) {
                        if (!jumped) {
                            this.startY = BlackOut.mc.player.getY();
                        }

                        jumped = true;
                        event.setXZ(
                                this,
                                MovementUtils.xMovement(this.verusBowSpeed.get(), Managers.ROTATION.moveYaw),
                                MovementUtils.zMovement(this.verusBowSpeed.get(), Managers.ROTATION.moveYaw)
                        );
                        if (BlackOut.mc.player.getY() + event.originalMovement.y < this.startY) {
                            event.setY(this, this.startY - BlackOut.mc.player.getY());
                            Managers.PACKET.spoofOG(true);
                        }
                    }

                    if (ticks >= this.verusLimit.get()) {
                        event.setXZ(this, MovementUtils.xMovement(0.2873, Managers.ROTATION.moveYaw), MovementUtils.zMovement(0.2873, Managers.ROTATION.moveYaw));
                        Managers.NOTIFICATIONS.addNotification("Reached tick limit", this.getDisplayName(), 2.0, Notifications.Type.Info);
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
                    event.setXZ(
                            this,
                            MovementUtils.xMovement(this.verusDMGSpeed.get(), Managers.ROTATION.moveYaw),
                            MovementUtils.zMovement(this.verusDMGSpeed.get(), Managers.ROTATION.moveYaw)
                    );
                    Timer.set(0.1F);
                    if (dmgFlyTicks > this.verusDMGLimit.get()) {
                        event.setXZ(this, MovementUtils.xMovement(0.2873, Managers.ROTATION.moveYaw), MovementUtils.zMovement(0.2873, Managers.ROTATION.moveYaw));
                        Timer.reset();
                        this.disable();
                    }
            }
        }
    }

    private void jump() {
        BlackOut.mc.player.jump();
    }

    private boolean jumping() {
        return BlackOut.mc.options.jumpKey.isPressed();
    }

    private boolean sneaking() {
        return BlackOut.mc.options.sneakKey.isPressed();
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
