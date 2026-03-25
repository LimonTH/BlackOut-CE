package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.InvUtils;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class InstantEat extends Module {
    private static InstantEat INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<PacketMode> packetMode = this.sgGeneral.enumSetting("Packet Mode", PacketMode.Full,
            "The type of movement packet to spam. 'Full' is the most reliable, while 'Rotation' or 'Position' might bypass specific checks.");
    private final Setting<Integer> packets = this.sgGeneral.intSetting("Packets", 32, 0, 50, 1,
            "How many packets to send in one tick. Since eating normally takes 32 ticks, sending 32 packets finishes the process instantly.");
    private final Setting<List<Item>> items = this.sgGeneral.itemListSetting("Items",
            "Which food items should be eaten instantly.", Items.GOLDEN_APPLE);
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent,
            "The method used to switch to the food item. Silent allows you to eat without stopping your current weapon use.");

    private final Predicate<ItemStack> predicate = itemStack -> this.items.get().contains(itemStack.getItem());
    private int packetsSent = 0;

    public InstantEat() {
        super("Instant Eat", "Instantly consumes food items in a single tick using packet saturation. (for v1.8)", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static InstantEat getInstance() {
        return INSTANCE;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.disable(this.doStuff());
    }

    private String doStuff() {
        InteractionHand hand = InvUtils.getHand(this.predicate);
        if (hand == null) {
            FindResult result = this.switchMode.get().find(this.predicate);
            if (!result.wasFound() || !this.switchMode.get().swapInstantly(result.slot())) {
                return "No item found";
            }
        }

        if (!BlackOut.mc.player.isUsingItem()) {
            this.useItemInstantly(hand);
        }

        this.packetsSent = 0;

        for (int i = 0; i < this.packets.get(); i++) {
            this.sendInstantly(this.packetMode.get().supplier.get());
            this.packetsSent++;
        }

        if (hand == null) {
            this.switchMode.get().swapBackInstantly();
        }

        return null;
    }

    public enum PacketMode {
        Full(
                () -> {
                    Vec3 pos = Managers.PACKET.pos;
                    return new ServerboundMovePlayerPacket.PosRot(
                            pos.x(), pos.y(), pos.z(), Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision
                    );
                }
        ),
        FullOffG(() -> {
            Vec3 pos = Managers.PACKET.pos;
            return new ServerboundMovePlayerPacket.PosRot(pos.x(), pos.y(), pos.z(), Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, false, BlackOut.mc.player.horizontalCollision);
        }),
        Rotation(
                () -> {
                    Vec3 pos = Managers.PACKET.pos;
                    return new ServerboundMovePlayerPacket.PosRot(
                            pos.x(),
                            pos.y(),
                            pos.z(),
                            Managers.ROTATION.prevYaw + ((InstantEat.getInstance().packetsSent & 1) == 0 ? 0.3759F : -0.2143F),
                            Managers.ROTATION.prevPitch,
                            Managers.PACKET.isOnGround(),
                            BlackOut.mc.player.horizontalCollision
                    );
                }
        ),
        DoubleRotation(() -> new ServerboundMovePlayerPacket.Rot(Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision)),
        Position(() -> {
            Vec3 pos = Managers.PACKET.pos;
            return new ServerboundMovePlayerPacket.Pos(pos.x(), pos.y(), pos.z(), Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision);
        }),
        Og(() -> new ServerboundMovePlayerPacket.StatusOnly(Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision));

        private final Supplier<ServerboundMovePlayerPacket> supplier;

        PacketMode(Supplier<ServerboundMovePlayerPacket> supplier) {
            this.supplier = supplier;
        }
    }
}
