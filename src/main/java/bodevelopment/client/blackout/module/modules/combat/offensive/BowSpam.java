package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class BowSpam extends Module {
    private static BowSpam INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Integer> charge = this.sgGeneral.intSetting("Charge", 3, 3, 20, 1, "How long to charge until releasing");
    public final Setting<Boolean> fast = this.sgGeneral.booleanSetting("Instant", false, "Instantly restarts using after stopping. Might not lose charge.");

    public BowSpam() {
        super("Bow Spam", "Automatically releases arrows", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static BowSpam getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return String.valueOf(InvUtils.count(true, true, stack -> stack.getItem() instanceof ArrowItem));
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.player.getMainHandStack().isOf(Items.BOW)
                    && BlackOut.mc.player.getItemUseTime() >= this.charge.get()
                    && BlackOut.mc.options.useKey.isPressed()) {
                BlackOut.mc.interactionManager.stopUsingItem(BlackOut.mc.player);
                if (this.fast.get()) {
                    BlackOut.mc.interactionManager.interactItem(BlackOut.mc.player, Hand.MAIN_HAND);
                }
            }
        }
    }
}
