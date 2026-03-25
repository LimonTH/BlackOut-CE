package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.util.PlayerUtils;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class HitCrystal extends Module {
    private static HitCrystal INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Normal, "The method used to select End Crystals in the hotbar.");
    private final Setting<Integer> postPlace = this.sgGeneral.intSetting("Post-Obsidian Delay", 1, 0, 20, 1, "Ticks to wait after placing obsidian before attempting to place a crystal.");
    private final Setting<Integer> preCrystal = this.sgGeneral.intSetting("Pre-Crystal Delay", 1, 0, 20, 1, "Additional ticks to wait specifically before the crystal interaction.");
    private final Setting<Boolean> multiCrystal = this.sgGeneral.booleanSetting("Continuous Placement", true, "Allows placing multiple crystals on the same block without resetting.");
    private final Setting<Double> speed = this.sgGeneral.doubleSetting("Placement Speed", 10.0, 0.0, 20.0, 1.0, "The maximum number of crystal placements per second.", this.multiCrystal::get);
    private final Setting<Double> attackSpeed = this.sgGeneral.doubleSetting("Attack Speed", 20.0, 0.0, 20.0, 1.0, "The maximum number of crystal attacks per second.");
    private final Setting<Boolean> attack = this.sgGeneral.booleanSetting("Auto-Attack", true, "Automatically detonates the crystal after placement.");

    private int timer = -1;
    private BlockPos pos = null;
    private boolean placed = false;
    private boolean attacked = false;
    private double places = 0.0;
    private double attacks = 0.0;

    public HitCrystal() {
        super("Hit Crystal", "Optimizes the sequence of placing obsidian and immediately following up with an End Crystal for rapid damage.", SubCategory.LEGIT, true);
        INSTANCE = this;
    }

    public static HitCrystal getInstance() {
        return INSTANCE;
    }

    @Event
    public void onPlace(InteractBlockEvent event) {
        ItemStack stack = event.hand == InteractionHand.MAIN_HAND ? Managers.PACKET.getStack() : BlackOut.mc.player.getOffhandItem();
        if (stack.is(Items.OBSIDIAN)) {
            this.timer = 0;
            this.placed = false;
            this.attacked = false;
            this.pos = event.hitResult.getBlockPos().relative(event.hitResult.getDirection());
        }
    }

    public void onTick() {
        if (PlayerUtils.isInGame()) {
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
            if (BlackOut.mc.hitResult instanceof BlockHitResult hitResult) {
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
                if (BlackOut.mc.hitResult instanceof EntityHitResult entityHitResult) {
                    if (!this.multiCrystal.get() || !this.attacked) {
                        if (entityHitResult.getType() != HitResult.Type.MISS) {
                            if (entityHitResult.getEntity().blockPosition().equals(this.pos.above())) {
                                if (this.timer > this.postPlace.get() + this.preCrystal.get()) {
                                    BlackOut.mc.gameMode.attack(BlackOut.mc.player, entityHitResult.getEntity());
                                    BlackOut.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                                    this.clientSwing(SwingHand.RealHand, InteractionHand.MAIN_HAND);
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
        InteractionHand hand = null;
        if (Managers.PACKET.getStack().getItem() == Items.END_CRYSTAL) {
            hand = InteractionHand.MAIN_HAND;
        }

        FindResult result = this.switchMode.get().find(Items.END_CRYSTAL);
        boolean switched = false;
        if (this.timer >= this.postPlace.get()) {
            if (hand != null || result.wasFound() && (switched = this.switchMode.get().swap(result.slot()))) {
                if (this.timer >= this.postPlace.get() + this.preCrystal.get()) {
                    hand = hand == null ? InteractionHand.MAIN_HAND : hand;
                    InteractionResult actionResult = BlackOut.mc.gameMode.useItemOn(BlackOut.mc.player, hand, hitResult);
                    if (actionResult instanceof InteractionResult.Success success) {
                        if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                            BlackOut.mc.getConnection().send(new ServerboundSwingPacket(hand));
                        }
                        this.clientSwing(SwingHand.RealHand, hand);
                        this.placed = true;
                    }

                    if (switched) {
                        this.switchMode.get().swapBack();
                    }
                }
            }
        }
    }
}
