package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.HoleUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector2i;

public class FastFall extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> onlyHole = this.sgGeneral.booleanSetting("Only Above Holes", false, "Limits the increased fall speed to when the player is directly positioned over a hole.");
    private final Setting<Boolean> jumpHole = this.sgGeneral.booleanSetting("Hole Forcing", false, "Forces the player into a hole even if they are currently in the upward phase of a jump.");
    private final Setting<Double> fallSpeed = this.sgGeneral.doubleSetting("Vertical Velocity", 1.0, 0.0, 10.0, 0.1, "The downward speed applied in blocks per second.");
    private final Setting<Boolean> rbDisable = this.sgGeneral.booleanSetting("Anti-Rubberband", true, "Temporarily suspends the module if a server-side position setback (rubberband) is detected.");

    private boolean jumping = false;
    private Vector2i jumpPos = new Vector2i(0, 0);
    private boolean rubberbanded = false;
    private long rbTime = 0L;

    public FastFall() {
        super("Fast Fall", "Increases downward acceleration to reach the ground or enter defensive holes more rapidly.", SubCategory.MOVEMENT, true);
    }

    @Override
    public boolean shouldSkipListeners() {
        return !this.enabled && !HoleSnap.getInstance().shouldFastFall();
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (event.movement.y > 0.2) {
            this.jumping = true;
            this.jumpPos.set(BlackOut.mc.player.getBlockX(), BlackOut.mc.player.getBlockZ());
        } else {
            if (event.movement.y < 0.0) {
                this.jumpPos = new Vector2i(-69420, -69420);
            }

            boolean onGround = OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(0.0, -0.04, 0.0));
            if (onGround) {
                if (this.rbTime <= 0L) {
                    this.rbTime = System.currentTimeMillis();
                }

                if (System.currentTimeMillis() - this.rbTime > 100L) {
                    this.rubberbanded = false;
                    this.rbTime = 0L;
                }
            }

            if (!this.rubberbanded) {
                if (BlackOut.mc.player.getBlockX() != this.jumpPos.x || BlackOut.mc.player.getBlockZ() != this.jumpPos.y) {
                    boolean holeCheck = this.aboveHole();
                    if (!this.onlyHole.get() || holeCheck) {
                        if (!this.jumping || holeCheck && this.jumpHole.get()) {
                            if (!onGround) {
                                if (!OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(0.0, -0.6, 0.0))) {
                                    this.fall(event, holeCheck);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.player.isOnGround()) {
            this.jumping = false;
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && this.rbDisable.get()) {
            this.rubberbanded = true;
        }
    }

    private void fall(MoveEvent.Pre event, boolean holeCheck) {
        event.setY(this, -this.fallSpeed.get());
        if (holeCheck) {
            event.setXZ(this, 0.0, 0.0);
        }
    }

    private boolean aboveHole() {
        for (double offset = 0.0; offset < 7.0; offset += 0.1) {
            if (OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(0.0, -offset, 0.0))) {
                return HoleUtils.inHole(BlockPos.ofFloored(BlackOut.mc.player.getPos().add(0.0, -offset + 0.12, 0.0)));
            }
        }

        return false;
    }
}
