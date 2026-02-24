package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;

public class NoSlow extends Module {
    private static NoSlow INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgStrict = this.addGroup("Strict");
    private final Setting<Boolean> blocking = this.sgGeneral.booleanSetting("Blocking", false, ".");
    private final Setting<Boolean> using = this.sgGeneral.booleanSetting("Using", false, ".");
    private final Setting<Boolean> strict = this.sgStrict.booleanSetting("Strict", false, "Sends switch packets to bypass NCP noslow checks.");
    private final Setting<Boolean> grim = this.sgStrict
            .booleanSetting("Grim", false, "Switches to a different slot instead of sending switch packet to the current one.", this.strict::get);
    private final Setting<Boolean> single = this.sgStrict
            .booleanSetting("Single Packet", true, "Only sends 1 switch packet after starting to eat. Works on most servers that require this module.", this.strict::get);
    private final Setting<Integer> delay = this.sgStrict
            .intSetting("Delay", 1, 1, 20, 1, "Tick delay between switch packets.", () -> !this.single.get() && this.strict.get());
    private int timer = 0;

    public NoSlow() {
        super("No Slow", "Prevents slowing down.", SubCategory.MOVEMENT, true);
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
                    return Managers.PACKET.getStack().getItem() instanceof SwordItem ? !getInstance().blocking.get() : !getInstance().using.get();
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

    public boolean shouldSendNoSlow(Hand hand) {
        if (BlackOut.mc.player == null) {
            return false;
        } else if (!getInstance().enabled) {
            return false;
        } else if (Aura.getInstance().isBlocking) {
            return getInstance().blocking.get();
        } else if (BlackOut.mc.player.isUsingItem()) {
            ItemStack stack = OLEPOSSUtils.getItem(hand);
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
        if (event.packet instanceof PlayerInteractItemC2SPacket packet && BlackOut.mc.player.isUsingItem()) {
            Hand hand = BlackOut.mc.player.getActiveHand();
            if (hand == packet.getHand() && this.shouldSendNoSlow(hand) && this.strict.get()) {
                this.send(hand);
                this.timer = 0;
            }
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (this.strict.get()) {
            if (this.shouldSendNoSlow(BlackOut.mc.player.getActiveHand())) {
                if (++this.timer >= this.delay.get() && !this.single.get()) {
                    this.send(BlackOut.mc.player.getActiveHand());
                    this.timer = 0;
                }
            }
        }
    }

    private int getGrimSlot(int slot) {
        return slot > 7 ? 0 : slot + 1;
    }

    private void send(Hand hand) {
        int currentSlot = Managers.PACKET.slot;
        if (this.grim.get()) {
            if (hand == Hand.MAIN_HAND) {
                this.sendSequencedPostGrim(sequence -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, sequence, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch));
                return;
            }

            Managers.PACKET.sendPostPacket(new UpdateSelectedSlotC2SPacket(this.getGrimSlot(currentSlot)));
        }

        Managers.PACKET.sendPostPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
    }
}
