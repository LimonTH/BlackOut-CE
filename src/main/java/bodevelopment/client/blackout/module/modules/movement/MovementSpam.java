package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class MovementSpam extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Integer> packets = this.sgGeneral.intSetting("Packet Multiplier", 1, 1, 10, 1, "The number of duplicate movement packets to send each tick.");

    private int packetsSent;

    public MovementSpam() {
        super("Movement Spam", "Redundantly transmits player movement packets to the server to manipulate network occupancy or trigger specific server-side behaviors.", SubCategory.MOVEMENT, true);
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Override
    public String getInfo() {
        return String.valueOf(this.packetsSent);
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (PlayerUtils.isInGame() && this.enabled) {
            Vec3 pos = Managers.PACKET.pos;

            for (int i = 0; i < this.packets.get(); i++) {
                this.sendPacket(
                        new ServerboundMovePlayerPacket.PosRot(
                                pos.x, pos.y, pos.z, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision));
            }
        }
    }

    @Event
    public void onSend(PacketEvent.Sent event) {
        if (event.packet instanceof ServerboundMovePlayerPacket) {
            this.packetsSent++;
        }
    }
}
