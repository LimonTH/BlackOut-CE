package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.movement.Blink;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class FastProjectile extends Module {
    private static FastProjectile INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> posRot = this.sgGeneral.booleanSetting("Sync Rotation", true, "Includes rotation data in the spoofed movement packets.");
    private final Setting<Boolean> blink = this.sgGeneral.booleanSetting("Blink Integration", false, "Buffers packets and releases them simultaneously to bypass strict velocity checks.");
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Blink Speed", 1.0, 1.0, 10.0, 0.1, "The speed multiplier for packet release when using Blink mode.", this.blink::get);
    private final Setting<Integer> charge = this.sgGeneral.intSetting("Power Level", 10, 0, 100, 1, "The number of movement packets to spoof per projectile throw to increase its velocity.");

    public int ticksLeft = 0;
    private double throwYaw = 0.0;
    private boolean down = false;
    private boolean enabledBlink = false;
    private int toSend = 0;
    private double x;
    private double y;
    private double z;
    private boolean ignore = false;
    private boolean throwIgnore = false;
    private Packet<?> throwPacket = null;

    public FastProjectile() {
        super("Fast Projectile", "Increases the velocity and distance of projectiles like arrows and ender pearls by spoofing rapid movement packets.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static FastProjectile getInstance() {
        return INSTANCE;
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && !this.ignore && this.ticksLeft > 0) {
            event.setCancelled(true);
        }

        if (event.packet instanceof PlayerInteractItemC2SPacket packet) {
            if (this.throwIgnore || !OLEPOSSUtils.getItem(packet.getHand()).isOf(Items.ENDER_PEARL)) {
                return;
            }

            this.move(event);
        }

        if (event.packet instanceof PlayerActionC2SPacket packet && packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
            if (this.throwIgnore || !(BlackOut.mc.player.getActiveItem().getItem() instanceof BowItem)) {
                return;
            }

            this.move(event);
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (--this.ticksLeft <= 0) {
            if (this.enabledBlink) {
                Blink.getInstance().disable();
                BlackOut.mc.getNetworkHandler().getConnection().flush();
                this.enabledBlink = false;
                this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                this.throwIgnore = true;
                if (this.throwPacket != null) {
                    this.sendPacket(this.throwPacket);
                }

                this.throwIgnore = false;
            }
        } else {
            this.toSend = (int) (this.toSend + this.timer.get());
            event.set(this, 0.0, 0.0, 0.0);

            while (this.toSend > 0) {
                this.chargeBlink();
                this.toSend--;
            }
        }
    }

    private void chargeBlink() {
        if (this.down = !this.down) {
            this.x = this.x + Math.cos(this.throwYaw) * 1.0E-5;
            this.z = this.z + Math.sin(this.throwYaw) * 1.0E-5;
            this.send(this.x, this.y + 1.0E-13, this.z, true);
        } else {
            this.send(this.x, this.y + 2.0E-13, this.z, false);
            this.ticksLeft--;
        }
    }

    private void move(PacketEvent.Send event) {
        double yaw = Math.toRadians(Managers.ROTATION.prevYaw + 90.0F);
        this.x = BlackOut.mc.player.getX();
        this.y = BlackOut.mc.player.getY();
        this.z = BlackOut.mc.player.getZ();
        if (this.blink.get()) {
            this.down = false;
            this.enabledBlink = true;
            this.throwYaw = yaw;
            this.ticksLeft = this.charge.get();
            this.throwPacket = event.packet;
            Blink.getInstance().enable();
            event.setCancelled(true);
        }

        this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        if (!this.blink.get()) {
            for (int i = 0; i < this.charge.get(); i++) {
                this.x = this.x + Math.cos(yaw) * 1.0E-5;
                this.z = this.z + Math.sin(yaw) * 1.0E-5;
                this.send(this.x, this.y + 1.0E-13, this.z, true);
                this.send(this.x, this.y + 2.0E-13, this.z, false);
            }

            this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
    }

    private void send(double x, double y, double z, boolean og) {
        this.ignore = true;
        if (this.posRot.get()) {
            this.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, og));
        } else {
            this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, og));
        }

        this.ignore = false;
    }
}
