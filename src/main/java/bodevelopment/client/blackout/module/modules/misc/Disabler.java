package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class Disabler extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> grimMovement = this.sgGeneral.booleanSetting("Grim Movement", false, ".");
    private final Setting<Boolean> b1 = this.sgGeneral.booleanSetting("Bol1", true, ".", this.grimMovement::get);
    private final Setting<Boolean> b2 = this.sgGeneral.booleanSetting("Bol2", true, ".", this.grimMovement::get);
    private final Setting<Double> tridentDelay = this.sgGeneral.doubleSetting("Trident Delay", 0.5, 0.0, 1.0, 0.01, ".", this.grimMovement::get);
    private final Setting<SwitchMode> tridentSwitch = this.sgGeneral.enumSetting("Trident Switch", SwitchMode.Silent, ".", this.grimMovement::get);
    private final Setting<Boolean> vulcanOmni = this.sgGeneral.booleanSetting("Vulcan Omni Sprint", false, ".");
    private long prevRiptide = 0L;

    public Disabler() {
        super("Disabler", "Disables some parts of anticheats", SubCategory.MISC, true);
    }

    @Event
    public void onMove(MoveEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.vulcanOmni.get() && !BlackOut.mc.options.forwardKey.isPressed() && BlackOut.mc.player.isSprinting()) {
                this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                this.sendPacket(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }
    }

    @Event
    public void onTickPre(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.grimMovement.get()) {
                if (System.currentTimeMillis() - this.prevRiptide < this.tridentDelay.get() * 1000.0) {
                    return;
                }

                Hand hand = OLEPOSSUtils.getHand(Items.TRIDENT);
                if (hand == null) {
                    FindResult result = this.tridentSwitch.get().find(Items.TRIDENT);
                    if (!result.wasFound() || !this.tridentSwitch.get().swap(result.slot())) {
                        return;
                    }
                }

                if (this.b1.get()) {
                    this.sendPacket(new PlayerInteractItemC2SPacket(hand == null ? Hand.MAIN_HAND : hand, 0, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch));
                }

                if (this.b2.get()) {
                    this.releaseUseItem();
                }

                this.prevRiptide = System.currentTimeMillis();
                if (hand == null) {
                    this.tridentSwitch.get().swapBack();
                }
            }
        }
    }
}
