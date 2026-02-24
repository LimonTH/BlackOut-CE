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
    public final Setting<SwitchMode> mode = this.sgGeneral.enumSetting("Mode", SwitchMode.Normal, "How to switch.");
    private final Setting<Boolean> swing = this.sgGeneral.booleanSetting("Swing", false, "Renders swing animation when placing throwing a peal");
    private final Setting<SwingHand> swingHand = this.sgGeneral.enumSetting("Swing Hand", SwingHand.RealHand, "Which hand should be swung.");

    public MCP() {
        super("MCP", "Throws a pearl", SubCategory.MISC, true);
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
