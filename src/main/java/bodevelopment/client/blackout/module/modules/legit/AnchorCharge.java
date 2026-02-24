package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class AnchorCharge extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<SwitchMode> glowstoneSwitch = this.sgGeneral.enumSetting("Glowstone Switch", SwitchMode.Normal, "Method of switching.");
    private final Setting<Boolean> fullCharge = this.sgGeneral.booleanSetting("Full Charge", false, "Uses 4 glowstone.");
    private final Setting<Boolean> allowOffhand = this.sgGeneral.booleanSetting("Allow Offhand", true, "Blows up the anchor with offhand. Not possible in vanilla.");
    private final Setting<SwitchMode> explodeSwitch = this.sgGeneral.enumSetting("Explode Switch", SwitchMode.Normal, "Method of switching.");
    private final Setting<Double> speed = this.sgGeneral.doubleSetting("Speed", 4.0, 0.0, 20.0, 0.1, "Actions per second.");
    private final Setting<Boolean> onlyOwn = this.sgGeneral.booleanSetting("Only Own", true, ".");
    private final Setting<Double> ownTime = this.sgGeneral.doubleSetting("Own Time", 2.0, 0.0, 10.0, 0.1, ".");
    private final TimerList<BlockPos> own = new TimerList<>(false);
    private final TimerMap<BlockPos, Integer> charges = new TimerMap<>(false);
    private final Map<BlockPos, BlockState> realStates = new ConcurrentHashMap<>();
    private final Predicate<ItemStack> emptyPredicate = stack -> {
        if (stack != null && !stack.isEmpty()) {
            Item item = stack.getItem();
            return item instanceof ToolItem;
        } else {
            return true;
        }
    };
    private double actions = 0.0;
    private int prevAnchor = -1;

    public AnchorCharge() {
        super("Anchor Charge", "Automatically charges and explodes anchors.", SubCategory.LEGIT, true);
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.actions = this.actions + event.frameTime * this.speed.get();
        this.actions = Math.min(this.actions, 1.0);
        this.realStates.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            if (this.charges.containsKey(pos)) {
                return false;
            } else {
                BlackOut.mc.world.setBlockState(pos, entry.getValue());
                return true;
            }
        });
    }

    @Event
    public void onState(BlockStateEvent event) {
        this.charges.update();
        if (this.charges.containsKey(event.pos)) {
            if (event.state.getBlock() == Blocks.RESPAWN_ANCHOR) {
                int c = event.state.get(Properties.CHARGES);
                if (c < this.charges.get(event.pos)) {
                    event.setCancelled(true);
                }
            } else if (this.charges.get(event.pos) != -1) {
                event.setCancelled(true);
            }

            this.realStates.remove(event.pos);
            this.realStates.put(event.pos, event.state);
        }
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            BlockPos pos = packet.getBlockHitResult().getBlockPos().offset(packet.getBlockHitResult().getSide());
            ItemStack holdingStack = packet.getHand() == Hand.MAIN_HAND ? Managers.PACKET.getStack() : BlackOut.mc.player.getOffHandStack();
            if (holdingStack.getItem() == Items.RESPAWN_ANCHOR) {
                this.own.add(pos, this.ownTime.get());
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.own.update();
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (Managers.PACKET.getStack().getItem() == Items.RESPAWN_ANCHOR) {
                this.prevAnchor = Managers.PACKET.slot;
            }

            if (BlackOut.mc.crosshairTarget instanceof BlockHitResult hitResult) {
                if (hitResult.getType() != HitResult.Type.MISS) {
                    BlockState state = BlackOut.mc.world.getBlockState(hitResult.getBlockPos());
                    if (state.getBlock() == Blocks.RESPAWN_ANCHOR) {
                        if (!this.onlyOwn.get() || this.own.contains(hitResult.getBlockPos())) {
                            if (!(this.actions <= 0.0)) {
                                if (!this.fullCharge.get() && state.get(Properties.CHARGES) > 0) {
                                    this.explode(hitResult);
                                } else {
                                    this.charge(hitResult);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void charge(BlockHitResult blockHitResult) {
        Hand hand = null;
        if (Managers.PACKET.getStack().getItem() == Items.GLOWSTONE) {
            hand = Hand.MAIN_HAND;
        }

        if (this.allowOffhand.get() && BlackOut.mc.player.getOffHandStack().getItem() == Items.GLOWSTONE) {
            hand = Hand.OFF_HAND;
        }

        FindResult result = this.glowstoneSwitch.get().find(Items.GLOWSTONE);
        boolean switched = false;
        if (hand != null || result.wasFound() && (switched = this.glowstoneSwitch.get().swap(result.slot()))) {
            hand = hand == null ? Hand.MAIN_HAND : hand;
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState state = BlackOut.mc.world.getBlockState(pos);
            if (state.get(Properties.CHARGES) < 4) {
                int c = state.get(Properties.CHARGES) + 1;
                this.charges.add(pos, c, 0.3);
                BlackOut.mc.world.setBlockState(pos, state.with(Properties.CHARGES, c));
            } else {
                this.charges.add(pos, -1, 0.3);
                BlackOut.mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }

            BlackOut.mc.interactionManager.interactBlock(BlackOut.mc.player, hand, blockHitResult);
            BlackOut.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            this.clientSwing(SwingHand.RealHand, hand);
            this.actions--;
            if (switched) {
                this.glowstoneSwitch.get().swapBack();
            }
        }
    }

    private void explode(BlockHitResult blockHitResult) {
        Hand hand = null;
        FindResult result = InvUtils.findNullable(this.explodeSwitch.get().hotbar, false, this.emptyPredicate);
        boolean switched = false;
        if (this.prevAnchor > -1
                && BlackOut.mc.player.getInventory().getStack(this.prevAnchor).getItem() == Items.RESPAWN_ANCHOR
                && this.explodeSwitch.get().hotbar) {
            if (!(switched = this.explodeSwitch.get().swap(this.prevAnchor))) {
                return;
            }
        } else {
            if (this.emptyPredicate.test(Managers.PACKET.getStack())) {
                hand = Hand.MAIN_HAND;
            }

            if (this.allowOffhand.get() && this.emptyPredicate.test(Managers.PACKET.getStack())) {
                hand = Hand.OFF_HAND;
            }

            if (hand == null && (!result.wasFound() || !(switched = this.explodeSwitch.get().swap(result.slot())))) {
                return;
            }
        }

        BlockPos pos = blockHitResult.getBlockPos();
        this.charges.add(pos, -1, 0.3);
        BlackOut.mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
        hand = hand == null ? Hand.MAIN_HAND : hand;
        BlackOut.mc.interactionManager.interactBlock(BlackOut.mc.player, hand, blockHitResult);
        this.clientSwing(SwingHand.RealHand, hand);
        this.actions--;
        if (switched) {
            this.explodeSwitch.get().swapBack();
        }
    }
}
