package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.InteractBlockEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class HitCrystal extends Module {
    private static HitCrystal INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Normal, "Method of switching.");
    private final Setting<Integer> postPlace = this.sgGeneral.intSetting("Post Place Ticks", 1, 0, 20, 1, ".");
    private final Setting<Integer> preCrystal = this.sgGeneral.intSetting("Pre Crystal Ticks", 1, 0, 20, 1, ".");
    private final Setting<Boolean> multiCrystal = this.sgGeneral.booleanSetting("Multi Crystal", true, ".");
    private final Setting<Double> speed = this.sgGeneral.doubleSetting("Speed", 10.0, 0.0, 20.0, 1.0, ".", this.multiCrystal::get);
    private final Setting<Double> attackSpeed = this.sgGeneral.doubleSetting("Attack Speed", 20.0, 0.0, 20.0, 1.0, ".");
    private final Setting<Boolean> attack = this.sgGeneral.booleanSetting("Attack", true, ".");
    private int timer = -1;
    private BlockPos pos = null;
    private boolean placed = false;
    private boolean attacked = false;
    private double places = 0.0;
    private double attacks = 0.0;

    public HitCrystal() {
        super("Hit Crystal", "Places a crystal on top of obsidian.", SubCategory.LEGIT, true);
        INSTANCE = this;
    }

    public static HitCrystal getInstance() {
        return INSTANCE;
    }

    @Event
    public void onPlace(InteractBlockEvent event) {
        ItemStack stack = event.hand == Hand.MAIN_HAND ? Managers.PACKET.getStack() : BlackOut.mc.player.getOffHandStack();
        if (stack.isOf(Items.OBSIDIAN)) {
            this.timer = 0;
            this.placed = false;
            this.attacked = false;
            this.pos = event.hitResult.getBlockPos().offset(event.hitResult.getSide());
        }
    }

    public void onTick() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.places = this.places + this.speed.get() / 20.0;
            this.attacks = this.attacks + this.attackSpeed.get() / 20.0;
            if (this.timer >= 0) {
                if (++this.timer <= this.postPlace.get() + this.preCrystal.get() + 10) {
                    this.updateAttacking();
                    this.updatePlacing();
                } else {
                    this.timer = -1;
                }
            }

            this.places = Math.min(this.places, 1.0);
            this.attacks = Math.min(this.attacks, 1.0);
        }
    }

    private void updatePlacing() {
        if (this.pos != null) {
            if (BlackOut.mc.crosshairTarget instanceof BlockHitResult hitResult) {
                if (this.multiCrystal.get() || !this.placed) {
                    if (hitResult.getType() != HitResult.Type.MISS) {
                        if (hitResult.getBlockPos().equals(this.pos)) {
                            while (this.places > 0.0) {
                                this.placeCrystal(hitResult);
                                this.places--;
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateAttacking() {
        if (this.attack.get()) {
            if (this.pos != null) {
                if (BlackOut.mc.crosshairTarget instanceof EntityHitResult entityHitResult) {
                    if (!this.multiCrystal.get() || !this.attacked) {
                        if (entityHitResult.getType() != HitResult.Type.MISS) {
                            if (entityHitResult.getEntity().getBlockPos().equals(this.pos.up())) {
                                if (this.timer > this.postPlace.get() + this.preCrystal.get()) {
                                    BlackOut.mc.interactionManager.attackEntity(BlackOut.mc.player, entityHitResult.getEntity());
                                    BlackOut.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    this.clientSwing(SwingHand.RealHand, Hand.MAIN_HAND);
                                    this.attacked = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeCrystal(BlockHitResult hitResult) {
        Hand hand = null;
        if (Managers.PACKET.getStack().getItem() == Items.END_CRYSTAL) {
            hand = Hand.MAIN_HAND;
        }

        FindResult result = this.switchMode.get().find(Items.END_CRYSTAL);
        boolean switched = false;
        if (this.timer >= this.postPlace.get()) {
            if (hand != null || result.wasFound() && (switched = this.switchMode.get().swap(result.slot()))) {
                if (this.timer >= this.postPlace.get() + this.preCrystal.get()) {
                    hand = hand == null ? Hand.MAIN_HAND : hand;
                    ActionResult actionResult = BlackOut.mc.interactionManager.interactBlock(BlackOut.mc.player, hand, hitResult);
                    if (actionResult.shouldSwingHand()) {
                        BlackOut.mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
                    }

                    this.clientSwing(SwingHand.RealHand, hand);
                    this.placed = true;
                    if (switched) {
                        this.switchMode.get().swapBack();
                    }
                }
            }
        }
    }
}
