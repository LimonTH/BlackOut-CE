package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.mixin.accessors.AccessorEntityStatusC2SPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class FastEat extends Module {
    private static FastEat INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> antiStop = this.sgGeneral.booleanSetting("Anti Stop", false, "Doesn't allow you to stop eating.");
    private final Setting<Double> packets = this.sgGeneral.doubleSetting("Packets", 0.0, 0.0, 10.0, 1.0, ".");
    private double toSend = 0.0;

    public FastEat() {
        super("Fast Eat", "Eats golden apples faster.", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static boolean eating() {
        return INSTANCE != null && INSTANCE.enabled && INSTANCE.antiStop.get() && getHand() != null;
    }

    private static Hand getHand() {
        return BlackOut.mc.player != null && BlackOut.mc.world != null ? OLEPOSSUtils.getHand(OLEPOSSUtils::isGapple) : null;
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (getHand() != null) {
            if (event.packet instanceof PlayerActionC2SPacket packet && packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM && this.antiStop.get()) {
                event.setCancelled(true);
            }
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof EntityStatusS2CPacket packet
                && BlackOut.mc.player != null
                && ((AccessorEntityStatusC2SPacket) packet).getId() == BlackOut.mc.player.getId()) {
            Hand hand = getHand();
            if (hand != null && BlackOut.mc.options.useKey.isPressed()) {
                this.sendSequenced(s -> new PlayerInteractItemC2SPacket(hand, s, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch));
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        Hand hand = getHand();
        if (hand != null && BlackOut.mc.options.useKey.isPressed()) {
            this.sendSequenced(s -> new PlayerInteractItemC2SPacket(hand, s, Managers.ROTATION.nextYaw, Managers.ROTATION.nextPitch));

            for (this.toSend = this.toSend + this.packets.get(); this.toSend > 0.0; this.toSend--) {
                Vec3d pos = Managers.PACKET.pos;
                this.sendInstantly(
                        new PlayerMoveC2SPacket.Full(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                Managers.ROTATION.prevYaw,
                                Managers.ROTATION.prevPitch,
                                Managers.PACKET.isOnGround()
                        )
                );
            }
        }
    }
}
