package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Items;

public class BowSpam extends Module {
    private static BowSpam INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> charge = this.sgGeneral.intSetting("Charge Duration", 3, 3, 20, 1, "The number of ticks to charge the bow before releasing the arrow.");
    public final Setting<Boolean> fast = this.sgGeneral.booleanSetting("Instant Draw", false, "Immediately begins drawing the next arrow after release to maximize projectile throughput.");

    public BowSpam() {
        super("Bow Spam", "Automatically releases bow tension at optimized intervals to maximize fire rate.", SubCategory.OFFENSIVE, true);
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
        if (PlayerUtils.isInGame()) {
            if (BlackOut.mc.player.getMainHandItem().is(Items.BOW)
                    && BlackOut.mc.player.getTicksUsingItem() >= this.charge.get()
                    && BlackOut.mc.options.keyUse.isDown()) {
                BlackOut.mc.gameMode.releaseUsingItem(BlackOut.mc.player);
                if (this.fast.get()) {
                    BlackOut.mc.gameMode.useItem(BlackOut.mc.player, InteractionHand.MAIN_HAND);
                }
            }
        }
    }
}
