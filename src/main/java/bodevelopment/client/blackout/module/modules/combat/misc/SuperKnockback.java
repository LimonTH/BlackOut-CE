package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class SuperKnockback extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> check = this.sgGeneral.booleanSetting("Move Check", true, "Checks if you are moving to prevent sprinting in place.");

    public SuperKnockback() {
        super("Super Knockback", "Tries to give more KB", SubCategory.MISC_COMBAT, true);
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (BlackOut.mc.player != null) {
            if (!this.check.get() || BlackOut.mc.player.getVelocity().getX() != 0.0 && BlackOut.mc.player.getVelocity().getZ() != 0.0) {
                if (event.packet instanceof AccessorInteractEntityC2SPacket packet
                        && packet.getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK
                        && BlackOut.mc.world.getEntityById(packet.getId()) instanceof LivingEntity) {
                    if (!BlackOut.mc.player.isSprinting()) {
                        this.start();
                    }

                    this.stop();
                    this.start();
                }
            }
        }
    }

    private void stop() {
        this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
    }

    private void start() {
        this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
    }
}
