package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;

public class AutoLog extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> disable = this.sgGeneral.booleanSetting("Disable on Disconnect", true, "Automatically Disables the module when disconnecting");
    private final Setting<Double> health = this.sgGeneral.doubleSetting("Log Health", 16.0, 0.0, 36.0, 1.0, "At what health should we disconnect");
    private final Setting<Boolean> totems = this.sgGeneral.booleanSetting("Count Totems", true, "Counts Totems");
    private final Setting<Double> totemAmount = this.sgGeneral.doubleSetting("Totem Amount", 3.0, 0.0, 36.0, 1.0, "How many totems left to disconnect", this.totems::get);

    public AutoLog() {
        super("Auto Log", "Automatically logs off", SubCategory.MISC_COMBAT, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            int tots = InvUtils.count(true, true, stack -> stack.isOf(Items.TOTEM_OF_UNDYING));
            if (BlackOut.mc.player.getHealth() + BlackOut.mc.player.getAbsorptionAmount() <= this.health.get()) {
                if (this.totems.get() && tots > this.totemAmount.get()) {
                    return;
                }

                if (this.disable.get()) {
                    this.disable();
                }

                BlackOut.mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("AutoLog")));
            }
        }
    }
}
