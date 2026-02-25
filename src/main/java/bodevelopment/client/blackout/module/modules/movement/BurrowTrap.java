package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.EntityUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class BurrowTrap extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Filter Blocks", "The blocks allowed to be used for the burrowing process.", Blocks.OBSIDIAN);
    public final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Enable Timer", true, "Utilizes a temporary game speed increase to accelerate the burrowing sequence.");
    public final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer Speed", 2.0, 0.0, 5.0, 0.05, "The factor by which the game tick rate is increased.", this.useTimer::get);
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Strategy", SwitchMode.Silent, "The method used to swap to the burrow block. Silent provides the best compatibility with anti-cheat.");
    private final Setting<Boolean> packet = this.sgGeneral.booleanSetting("Packet Placement", false, "Uses packet-based logic to place the block, potentially bypassing certain interaction checks.");
    private final Setting<Boolean> instantRotation = this.sgGeneral.booleanSetting("Instant Rotate", true, "Forces the player's rotation to the placement target immediately within a single tick.");

    private final double[] offsets = new double[]{0.42, 0.3332, 0.2468};
    private BlockPos pos = null;
    private int progress = 0;
    private boolean placed = false;

    public BurrowTrap() {
        super("Burrow Trap", "Quickly clips the player into a block at their current feet position to prevent being pushed or damaged.", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onEnable() {
        this.pos = BlackOut.mc.player.getBlockPos();
        this.progress = -1;
        this.placed = false;
    }

    @Override
    public void onDisable() {
        if (this.useTimer.get()) {
            Timer.reset();
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (this.progress >= 0) {
            if (this.progress < 3) {
                double offset = this.offsets[this.progress];
                this.progress++;
                event.set(this, 0.0, offset, 0.0);
            } else {
                this.progress++;
                event.set(this, 0.0, 0.0, 0.0);
                if (this.progress > 3) {
                    this.disable("success");
                }
            }
        }
    }

    @Event
    public void onRender(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.pos != null) {
            if (!EntityUtils.intersects(BoxUtils.get(this.pos), entity -> !(entity instanceof ItemEntity) && entity != BlackOut.mc.player)) {
                PlaceData data = SettingUtils.getPlaceData(this.pos);
                if (data.valid()) {
                    Hand hand = OLEPOSSUtils.getHand(this::valid);
                    boolean switched = false;
                    FindResult result = this.switchMode.get().find(this::valid);
                    if (hand != null || result.wasFound()) {
                        if (this.progress == -1) {
                            this.progress = 0;
                        }

                        if (this.useTimer.get()) {
                            Timer.set(this.timer.get().floatValue());
                        }

                        if (!this.placed) {
                            if (!SettingUtils.shouldRotate(RotationType.BlockPlace)
                                    || this.rotateBlock(data, RotationType.BlockPlace.withInstant(this.instantRotation.get()), "placing")) {
                                if (!EntityUtils.intersects(BoxUtils.get(this.pos), entity -> entity == BlackOut.mc.player)) {
                                    if (hand != null || (switched = this.switchMode.get().swap(result.slot()))) {
                                        this.placeBlock(hand, data.pos().toCenterPos(), data.dir(), data.pos());
                                        this.placed = true;
                                        if (!this.packet.get()) {
                                            this.setBlock(this.pos, result.stack().getItem());
                                        }

                                        if (switched) {
                                            this.switchMode.get().swapBack();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setBlock(BlockPos pos, Item item) {
        if (item instanceof BlockItem block) {
            Managers.PACKET
                    .addToQueue(
                            handler -> {
                                BlackOut.mc.world.setBlockState(pos, block.getBlock().getDefaultState());
                                BlackOut.mc
                                        .world
                                        .playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
                            }
                    );
        }
    }

    private boolean valid(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && this.blocks.get().contains(block.getBlock());
    }
}
