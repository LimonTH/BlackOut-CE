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
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class Disabler extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> grimMovement = this.sgGeneral.booleanSetting("Grim Movement", false, "Attempts to desynchronize the Grim AntiCheat movement processor using Trident Riptide interactions.");
    private final Setting<Boolean> b1 = this.sgGeneral.booleanSetting("Interact Packet", true, "Sends an item interaction packet before releasing the trident.", this.grimMovement::get);
    private final Setting<Boolean> b2 = this.sgGeneral.booleanSetting("Release Packet", true, "Sends a stop-use packet to finalize the trident desync.", this.grimMovement::get);
    private final Setting<Double> tridentDelay = this.sgGeneral.doubleSetting("Interaction Delay", 0.5, 0.0, 1.0, 0.01, "The cooldown in seconds between simulated Riptide uses.", this.grimMovement::get);
    private final Setting<SwitchMode> tridentSwitch = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "The inventory swap method used to access the Trident.", this.grimMovement::get);
    private final Setting<Boolean> vulcanOmni = this.sgGeneral.booleanSetting("Vulcan Omni Sprint", false, "Spoofs sprinting packets to allow multi-directional sprinting on Vulcan AntiCheat.");

    private long prevRiptide = 0L;

    public Disabler() {
        super("Disabler", "Exploits specific AntiCheat vulnerabilities to bypass movement restrictions or combat checks.", SubCategory.MISC, true);
    }

    @Event
    public void onMove(MoveEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            if (this.vulcanOmni.get() && !BlackOut.mc.options.keyUp.isDown() && BlackOut.mc.player.isSprinting()) {
                this.sendPacket(new ServerboundPlayerCommandPacket(BlackOut.mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                this.sendPacket(new ServerboundPlayerCommandPacket(BlackOut.mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
        }
    }

    @Event
    public void onTickPre(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            if (this.grimMovement.get()) {
                if (System.currentTimeMillis() - this.prevRiptide < this.tridentDelay.get() * 1000.0) {
                    return;
                }

                InteractionHand hand = OLEPOSSUtils.getHand(Items.TRIDENT);
                if (hand == null) {
                    FindResult result = this.tridentSwitch.get().find(Items.TRIDENT);
                    if (!result.wasFound() || !this.tridentSwitch.get().swap(result.slot())) {
                        return;
                    }
                }

                if (this.b1.get()) {
                    this.sendPacket(new ServerboundUseItemPacket(hand == null ? InteractionHand.MAIN_HAND : hand, 0, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch));
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
