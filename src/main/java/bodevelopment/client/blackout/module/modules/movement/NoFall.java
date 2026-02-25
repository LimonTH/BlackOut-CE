package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class NoFall extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Bypass Mode", Mode.Packet, "The logic used to trick the server into resetting fall distance.");

    private float fallDist;
    private float lastFallDist = 0.0F;
    private boolean tg = false;
    private boolean grim = false;

    public NoFall() {
        super("No Fall", "Protects the player from taking fall damage by spoofing on-ground status or manipulating packet flow.", SubCategory.MOVEMENT, true);
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onMovePost(MoveEvent.PostSend event) {
        if (this.grim) {
            this.grim = false;
            if (!Managers.PACKET.isOnGround()) {
                Vec3d vec = Managers.PACKET.pos;
                this.sendPacket(
                        new PlayerMoveC2SPacket.Full(vec.x, vec.y + 1.0E-6, vec.z, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, false)
                );
                BlackOut.mc.player.fallDistance = 0.0F;
            }
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.player.fallDistance == 0.0F) {
                this.fallDist = 0.0F;
            }

            this.fallDist = this.fallDist + (BlackOut.mc.player.fallDistance - this.lastFallDist);
            this.lastFallDist = BlackOut.mc.player.fallDistance;
            switch (this.mode.get()) {
                case Packet:
                    if (this.fallDist > 2.0F) {
                        Managers.PACKET.spoofOG(true);
                        this.fallDist = 0.0F;
                    }
                    break;
                case LessDMG:
                    if (BlackOut.mc.player.fallDistance > 1.5 && this.tg) {
                        Managers.PACKET.spoofOG(true);
                        this.tg = false;
                    }

                    if (BlackOut.mc.player.isOnGround()) {
                        this.tg = true;
                    }
                    break;
                case GroundSpoof:
                    if (BlackOut.mc.player.fallDistance > 2.0F) {
                        Managers.PACKET.spoofOG(true);
                        this.fallDist = 0.0F;
                    }
                    break;
                case NoGround:
                    Managers.PACKET.spoofOG(false);
                    break;
                case Grim:
                    if (BlackOut.mc.player.fallDistance >= 3.0F) {
                        this.grim = true;
                    }
            }
        }
    }

    public enum Mode {
        Packet,
        GroundSpoof,
        NoGround,
        LessDMG,
        Grim
    }
}
