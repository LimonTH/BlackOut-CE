package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.CompatUtils;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public class NoSlow extends Module {
    private static NoSlow INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgStrict = this.addGroup("Strict");

    private final Setting<Boolean> blocking = this.sgGeneral.booleanSetting("Sword Blocking", false, "Prevents the movement penalty while blocking with a sword.");
    private final Setting<Boolean> using = this.sgGeneral.booleanSetting("Item Usage", false, "Prevents the movement penalty while consuming food, potions, or using other items.");

    private final Setting<Boolean> strict = this.sgStrict.booleanSetting("NCP Bypass", false, "Synchronizes slot updates with item usage to bypass stricter Anti-Cheat checks.");
    private final Setting<Boolean> grim = this.sgStrict.booleanSetting("Grim Strategy", false, "Utilizes alternative slot switching logic specifically designed for the Grim Anti-Cheat.", this.strict::get);
    private final Setting<Boolean> single = this.sgStrict.booleanSetting("Single Sync", true, "Sends only a single synchronization packet upon item usage initiation.", this.strict::get);
    private final Setting<Integer> delay = this.sgStrict.intSetting("Sync Interval", 1, 1, 20, 1, "The tick delay between repeated synchronization packets.", () -> !this.single.get() && this.strict.get());

    private int timer = 0;

    public NoSlow() {
        super("No Slow", "Negates the movement speed reduction usually applied when using items or blocking.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static NoSlow getInstance() {
        return INSTANCE;
    }

    public static boolean shouldSlow() {
        if (BlackOut.mc.player == null) {
            return false;
        } else {
            boolean isBlocking = Aura.getInstance().isBlocking;
            if (getInstance().enabled) {
                if (isBlocking) {
                    return !getInstance().blocking.get();
                } else if (BlackOut.mc.player.isUsingItem()) {
                    InteractionHand activeHand = BlackOut.mc.player.getUsedItemHand();
                    ItemStack activeStack = activeHand == InteractionHand.MAIN_HAND ? 
                        Managers.PACKET.getStack() : 
                        BlackOut.mc.player.getOffhandItem();

                    return activeStack.getItem() instanceof SwordItem ? 
                        !getInstance().blocking.get() : 
                        !getInstance().using.get();
                } else {
                    return false;
                }
            } else {
                return isBlocking || BlackOut.mc.player.isUsingItem();
            }
        }
    }

    @Override
    public String getInfo() {
        return this.strict.get() ? "Strict" : "Normal";
    }

    public boolean shouldSendNoSlow(InteractionHand hand) {
        if (BlackOut.mc.player == null) {
            return false;
        } else if (!getInstance().enabled) {
            return false;
        } else if (Aura.getInstance().isBlocking) {
            return getInstance().blocking.get();
        } else if (BlackOut.mc.player.isUsingItem()) {
            ItemStack stack = InvUtils.getHandItem(hand);
            if (stack == null) {
                return false;
            } else {
                return stack.getItem() instanceof SwordItem ? getInstance().blocking.get() : getInstance().using.get();
            }
        } else {
            return false;
        }
    }

    @Event
    public void onSend(PacketEvent.Sent event) {
        if (event.packet instanceof ServerboundUseItemPacket packet && BlackOut.mc.player.isUsingItem()) {
            InteractionHand hand = BlackOut.mc.player.getUsedItemHand();
            if (hand == packet.getHand() && this.shouldSendNoSlow(hand) && this.strict.get()) {
                this.send(hand);
                this.timer = 0;
            }
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (this.strict.get()) {
            if (this.shouldSendNoSlow(BlackOut.mc.player.getUsedItemHand())) {
                if (++this.timer >= this.delay.get() && !this.single.get()) {
                    this.send(BlackOut.mc.player.getUsedItemHand());
                    this.timer = 0;
                }
            }
        }
    }

    private int getGrimSlot(int slot) {
        return slot > 7 ? 0 : slot + 1;
    }

    private void send(InteractionHand hand) {
        int currentSlot = Managers.PACKET.slot;
        if (this.grim.get()) {
            if (hand == InteractionHand.MAIN_HAND) {
                this.sendSequencedPostGrim(sequence -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, sequence, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch));
                return;
            }

            Managers.PACKET.sendPostPacket(new ServerboundSetCarriedItemPacket(this.getGrimSlot(currentSlot)));
        }

        Managers.PACKET.sendPostPacket(new ServerboundSetCarriedItemPacket(currentSlot));
    }
}
