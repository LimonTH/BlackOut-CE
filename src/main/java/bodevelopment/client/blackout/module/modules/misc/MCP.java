package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class MCP extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<SwitchMode> mode = this.sgGeneral.enumSetting("Swap Method", SwitchMode.Normal, "The mechanism used to switch to an Ender Pearl.");
    private final Setting<Boolean> swing = this.sgGeneral.booleanSetting("Swing Animation", false, "Plays a hand swing animation upon throwing the pearl.");
    private final Setting<SwingHand> swingHand = this.sgGeneral.enumSetting("Swing Hand", SwingHand.RealHand, "Determines which hand performs the swing animation.");

    public MCP() {
        super("MCP", "Middle Click Pearl; automatically throws an Ender Pearl upon clicking the middle mouse button.", SubCategory.MISC, true);
    }

    @Event
    public void mouseClick(MouseButtonEvent event) {
        if ((BlackOut.mc.player != null || BlackOut.mc.world != null) && BlackOut.mc.currentScreen == null) {
            if (event.button == 2) {
                Hand hand = OLEPOSSUtils.getHand(Items.ENDER_PEARL);
                FindResult result = this.mode.get().find(Items.ENDER_PEARL);
                if (result.wasFound() || hand != null) {
                    if (hand != null || this.mode.get().swap(result.slot())) {
                        this.useItem(hand);
                        if (this.swing.get()) {
                            this.clientSwing(this.swingHand.get(), hand);
                        }

                        if (hand == null) {
                            this.mode.get().swapBack();
                        }
                    }
                }
            }
        }
    }
}
