package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.settings.SwingSettings;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoMine;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class Nuker extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgSpeed = this.addGroup("Speed");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final Setting<Boolean> pauseEat = this.sgGeneral.booleanSetting("Pause Eat", false, "Pauses when eating");
    private final Setting<Boolean> packet = this.sgGeneral.booleanSetting("Packet", true, "Doesn't remove the block client side.");
    private final Setting<SwitchMode> pickaxeSwitch = this.sgGeneral
            .enumSetting("Pickaxe Switch", SwitchMode.InvSwitch, "Method of switching. InvSwitch is used in most clients.");
    private final Setting<Boolean> allowInventory = this.sgGeneral.booleanSetting("Allow Inventory", false, ".", () -> this.pickaxeSwitch.get().inventory);
    private final Setting<Boolean> resetOnSwitch = this.sgGeneral.booleanSetting("Reset On Switch", true, "Resets mining when switched held item.");
    private final Setting<Boolean> ncpProgress = this.sgGeneral.booleanSetting("NCP Progress", true, "Uses ncp mining progress checks.");
    private final Setting<List<Block>> blocks = this.sgGeneral.blockListSetting("Blocks", ".");
    private final Setting<Boolean> down = this.sgGeneral.booleanSetting("Down", false, "Allows mining down.");
    private final Setting<Boolean> creative = this.sgSpeed.booleanSetting("Creative", false, ".");
    private final Setting<Double> speed = this.sgSpeed.doubleSetting("Speed", 1.0, 0.0, 2.0, 0.05, "Vanilla speed multiplier.", () -> !this.creative.get());
    private final Setting<Boolean> onGroundCheck = this.sgSpeed.booleanSetting("On Ground Check", true, "Mines 5x slower when not on ground.", () -> !this.creative.get());
    private final Setting<Boolean> effectCheck = this.sgSpeed
            .booleanSetting("Effect Check", true, "Modifies mining speed depending on haste and mining fatigue.", () -> !this.creative.get());
    private final Setting<Boolean> waterCheck = this.sgSpeed.booleanSetting("Water Check", true, "Mines 5x slower while submerged in water.", () -> !this.creative.get());
    private final Setting<Integer> maxInstants = this.sgSpeed.intSetting("Max Instants", 5, 1, 20, 1, "Maximum amount if instant mines per tick.");
    private final Setting<Boolean> mineStartSwing = this.sgRender.booleanSetting("Mine Start Swing", false, "Renders swing animation when starting to mine.");
    private final Setting<Boolean> mineEndSwing = this.sgRender.booleanSetting("Mine End Swing", false, "Renders swing animation when ending mining.");
    private final Setting<SwingHand> mineHand = this.sgRender
            .enumSetting("Mine Hand", SwingHand.RealHand, "Which hand should be swung.", () -> this.mineStartSwing.get() || this.mineEndSwing.get());
    private final Setting<Boolean> animationColor = this.sgRender.booleanSetting("Animation Color", true, "Changes color smoothly.");
    private final Setting<AutoMine.AnimationMode> animationMode = this.sgRender.enumSetting("Animation Mode", AutoMine.AnimationMode.Full, ".");
    private final Setting<Double> animationExponent = this.sgRender.doubleSetting("Animation Exponent", 1.0, 0.0, 10.0, 0.1, ".");
    private final Setting<RenderShape> renderShape = this.sgRender.enumSetting("Render Shape", RenderShape.Full, "Which parts should be rendered.");
    private final Setting<BlackOutColor> lineStartColor = this.sgRender.colorSetting("Line Start Color", new BlackOutColor(255, 0, 0, 0), ".");
    private final Setting<BlackOutColor> sideStartColor = this.sgRender.colorSetting("Side Start Color", new BlackOutColor(255, 0, 0, 0), ".");
    private final Setting<BlackOutColor> lineEndColor = this.sgRender.colorSetting("Line End Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> sideEndColor = this.sgRender.colorSetting("Side End Color", new BlackOutColor(255, 0, 0, 50), ".");
    private final List<Pair<BlockPos, Double>> instants = new ArrayList<>();
    public boolean started = false;
    private BlockPos minePos = null;
    private BlockPos prevPos = null;
    private boolean startedThisTick = false;
    private double progress = 0.0;
    private int minedFor = 0;
    private double prevProgress = 0.0;
    private double currentProgress = 0.0;
    private boolean shouldRestart = false;
    private BlockPos ended = BlockPos.ORIGIN;

    public Nuker() {
        super("Nuker", "Breaks blocks.", SubCategory.MISC, true);
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (this.resetOnSwitch.get() && event.packet instanceof UpdateSelectedSlotC2SPacket) {
            this.shouldRestart = true;
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (!this.started) {
            this.prevProgress = 0.0;
            this.currentProgress = 0.0;
        }

        this.updateRender(event.tickDelta);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.startedThisTick = false;
            if (this.shouldUpdatePos()) {
                this.minePos = this.findPos();
            }

            if (this.minePos != null && (!SettingUtils.inMineRange(this.minePos) || SettingUtils.getPlaceOnDirection(this.minePos) == null)) {
                this.started = false;
                this.minePos = null;
            }

            if (this.instants.isEmpty()) {
                this.updateStartOrAbort(this.minePos);
            } else {
                this.instants.forEach(pair -> this.updateStartOrAbort(pair.getLeft()));
            }

            this.updateMining();
            this.prevProgress = this.currentProgress;
            this.currentProgress = this.getProgress();
        }
    }

    private boolean shouldUpdatePos() {
        return this.minePos == null || !this.blocks.get().contains(BlackOut.mc.world.getBlockState(this.minePos).getBlock());
    }

    private BlockPos findPos() {
        this.instants.clear();
        BlockPos middle = BlockPos.ofFloored(BlackOut.mc.player.getEyePos());
        int rad = (int) Math.ceil(SettingUtils.maxMineRange());
        BlockPos bestPos = null;
        double bestDist = -1.0;
        double bestDelta = 0.0;
        int feetY = (int) Math.round(BlackOut.mc.player.getY());

        for (int x = -rad; x <= rad; x++) {
            for (int y = -rad; y <= rad; y++) {
                for (int z = -rad; z <= rad; z++) {
                    BlockPos pos = middle.add(x, y, z);
                    if (this.down.get() || pos.getY() >= feetY) {
                        BlockState state = BlackOut.mc.world.getBlockState(pos);
                        if (this.blocks.get().contains(state.getBlock())) {
                            double delta = this.getBestDelta(pos);
                            boolean isInstant = this.creative.get() || delta >= 1.0;
                            if (!(delta < bestDelta) || isInstant) {
                                double dist = BlackOut.mc.player.getEyePos().distanceTo(pos.toCenterPos());
                                if ((!(dist > bestDist) || !(bestDist > 0.0) || isInstant)
                                        && SettingUtils.getPlaceOnDirection(pos) != null
                                        && SettingUtils.inMineRange(pos)) {
                                    if (isInstant) {
                                        if (this.instants.size() < this.maxInstants.get()) {
                                            this.instants.add(new Pair<>(pos, dist));
                                        } else {
                                            for (int i = 0; i < this.instants.size(); i++) {
                                                Pair<BlockPos, Double> pair = this.instants.get(i);
                                                if (!(pair.getRight() < dist)) {
                                                    this.instants.remove(i);
                                                    this.instants.add(new Pair<>(pos, dist));
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    bestPos = pos;
                                    bestDist = delta > bestDelta ? -1.0 : dist;
                                    bestDelta = delta;
                                }
                            }
                        }
                    }
                }
            }
        }

        return this.instants.size() > 0 ? this.instants.get(0).getLeft() : bestPos;
    }

    private double getProgress() {
        if (this.minePos == null) {
            return 0.0;
        } else {
            ItemStack itemStack = this.findBestSlot(
                            stack -> BlockUtils.getBlockBreakingDelta(
                                    stack, BlackOut.mc.world.getBlockState(this.minePos), this.minePos, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get()
                            )
                    )
                    .stack();
            return this.ncpProgress.get()
                    ? this.minedFor
                    / (
                    1.0
                            / BlockUtils.getBlockBreakingDelta(
                            itemStack, BlackOut.mc.world.getBlockState(this.minePos), this.minePos, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get()
                    )
            )
                    : this.progress;
        }
    }

    private double getBestDelta(BlockPos pos) {
        double best = 0.0;

        for (int i = this.pickaxeSwitch.get().hotbar ? 0 : 9;
             i < (this.pickaxeSwitch.get().inventory && this.allowInventory.get() ? BlackOut.mc.player.getInventory().size() : 9);
             i++
        ) {
            ItemStack stack = BlackOut.mc.player.getInventory().getStack(i);
            best = Math.max(
                    best,
                    BlockUtils.getBlockBreakingDelta(
                            stack, BlackOut.mc.world.getBlockState(pos), pos, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get()
                    )
            );
        }

        return best;
    }

    private void updateStartOrAbort(BlockPos pos) {
        if (this.shouldRestart) {
            if (pos != null) {
                this.abort(pos);
            }

            this.shouldRestart = false;
            this.started = false;
        }

        if (pos == null) {
            if (this.prevPos != null && !this.prevPos.equals(this.ended)) {
                this.abort(this.prevPos);
            }
        } else {
            if (!pos.equals(this.prevPos)) {
                this.started = false;
            }

            if (!this.started && !this.paused()) {
                Direction dir = SettingUtils.getPlaceOnDirection(pos);
                if (!SettingUtils.startMineRot() || this.rotateBlock(pos, dir, pos.toCenterPos(), RotationType.Mining, "mining")) {
                    this.start(pos);
                }
            }
        }

        this.prevPos = pos;
    }

    private boolean paused() {
        return this.pauseEat.get() && BlackOut.mc.player.isUsingItem();
    }

    private void start(BlockPos pos) {
        Direction dir = SettingUtils.getPlaceOnDirection(pos);
        if (dir != null) {
            if (this.prevPos != null && !this.prevPos.equals(pos) && !this.prevPos.equals(this.ended)) {
                this.abort(this.prevPos);
            }

            boolean holding = this.creative.get()
                    || BlockUtils.getBlockBreakingDelta(pos, Managers.PACKET.getStack(), this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get())
                    >= 1.0;
            FindResult result = this.findBestSlot(
                    stack -> BlockUtils.getBlockBreakingDelta(pos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get())
            );
            boolean canInstant = this.creative.get()
                    || BlockUtils.getBlockBreakingDelta(pos, result.stack(), this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get()) >= 1.0;
            if (!holding && !canInstant) {
                this.currentProgress = 0.0;
                this.prevProgress = 0.0;
                this.started = true;
                this.startedThisTick = true;
                this.progress = 0.0;
                this.minedFor = 0;
            } else {
                this.minePos = null;
                this.ended = pos;
                if (!this.packet.get()) {
                    BlackOut.mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }

                Managers.BLOCK.set(pos, Blocks.AIR, true, true);
            }

            if (canInstant && !holding) {
                this.pickaxeSwitch.get().swap(result.slot());
            }

            this.sendSequenced(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, dir, s));
            SettingUtils.mineSwing(SwingSettings.MiningSwingState.Start);
            if (canInstant && !holding) {
                this.pickaxeSwitch.get().swapBack();
            }

            this.end("mining");
            if (this.mineStartSwing.get()) {
                this.clientSwing(this.mineHand.get(), Hand.MAIN_HAND);
            }
        }
    }

    private void updateMining() {
        if (this.minePos != null && !this.startedThisTick) {
            boolean holding = this.itemMinedCheck(Managers.PACKET.getStack());
            int slot = this.findBestSlot(
                            stack -> BlockUtils.getBlockBreakingDelta(this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get())
                    )
                    .slot();
            ItemStack bestStack = holding ? Managers.PACKET.getStack() : BlackOut.mc.player.getInventory().getStack(slot);
            if (this.ncpProgress.get()) {
                this.minedFor++;
            } else {
                this.progress = this.progress
                        + BlockUtils.getBlockBreakingDelta(this.minePos, bestStack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get());
            }

            if (this.itemMinedCheck(bestStack)) {
                this.mineEndUpdate(holding, slot);
            } else if (this.almostMined(bestStack) && SettingUtils.endMineRot()) {
                this.preRotate();
            }
        }
    }

    private void mineEndUpdate(boolean holding, int slot) {
        if (!this.paused()) {
            this.endMining(holding, slot);
        }
    }

    private void endMining(boolean holding, int slot) {
        if (!(this.getBlock(this.minePos) instanceof AirBlock)) {
            Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
            if (dir != null) {
                if (!SettingUtils.endMineRot() || this.rotateBlock(this.minePos, dir, this.minePos.toCenterPos(), RotationType.Mining, "mining")) {
                    boolean switched = false;
                    if (holding || (switched = this.pickaxeSwitch.get().swap(slot))) {
                        this.ended = this.minePos;
                        this.sendSequenced(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, this.minePos, dir, s));
                        SettingUtils.mineSwing(SwingSettings.MiningSwingState.End);
                        if (this.mineEndSwing.get()) {
                            this.clientSwing(this.mineHand.get(), Hand.MAIN_HAND);
                        }

                        if (!this.packet.get()) {
                            BlackOut.mc.world.setBlockState(this.minePos, Blocks.AIR.getDefaultState());
                        }

                        Managers.BLOCK.set(this.minePos, Blocks.AIR, true, true);
                        Managers.ENTITY.addSpawning(this.minePos);
                        this.started = false;
                        this.minePos = null;
                        this.end("mining");
                        if (switched) {
                            this.pickaxeSwitch.get().swapBack();
                        }
                    }
                }
            }
        }
    }

    private void preRotate() {
        if (!(this.getBlock(this.minePos) instanceof AirBlock)) {
            if (SettingUtils.inMineRange(this.minePos)) {
                Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
                if (dir != null) {
                    this.rotateBlock(this.minePos, dir, this.minePos.toCenterPos(), RotationType.Mining, "mining");
                }
            }
        }
    }

    private Block getBlock(BlockPos pos) {
        return Managers.BLOCK.blockState(pos).getBlock();
    }

    private FindResult findBestSlot(EpicInterface<ItemStack, Double> test) {
        return InvUtils.findBest(this.pickaxeSwitch.get().hotbar, this.pickaxeSwitch.get().inventory && this.allowInventory.get(), test);
    }

    private boolean itemMinedCheck(ItemStack stack) {
        return this.ncpProgress.get()
                ? this.minedFor * this.speed.get()
                >= Math.ceil(1.0 / BlockUtils.getBlockBreakingDelta(this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get()))
                : this.progress * this.speed.get() >= 1.0;
    }

    private boolean almostMined(ItemStack stack) {
        if (this.getBlock(this.minePos) instanceof AirBlock) {
            return false;
        } else if (!SettingUtils.inMineRange(this.minePos)) {
            return false;
        } else if (SettingUtils.getPlaceOnDirection(this.minePos) == null) {
            return false;
        } else {
            return this.ncpProgress.get()
                    ? this.minedFor + 2
                    >= Math.ceil(
                    1.0 / BlockUtils.getBlockBreakingDelta(this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get())
            )
                    : this.progress >= 0.9;
        }
    }

    private void updateRender(float tickDelta) {
        double p = this.currentProgress;
        if (this.minePos != null && this.prevProgress < p && p < Double.POSITIVE_INFINITY) {
            p = MathHelper.lerp(tickDelta, this.prevProgress, p);
            p = MathHelper.clamp(p, 0.0, 1.0);
            p = 1.0 - Math.pow(1.0 - p, this.animationExponent.get());
            p = Math.min(p / 2.0, 0.5);
            BlackOutColor sideColor = this.getSideColor(p * 2.0);
            BlackOutColor lineColor = this.getLineColor(p * 2.0);
            Box box = this.getBox(p, this.animationMode.get());
            if (box != null) {
                Render3DUtils.box(box, sideColor, lineColor, this.renderShape.get());
            }
        }
    }

    private Box getBox(double p, AutoMine.AnimationMode mode) {
        double up = 0.5;
        double down = 0.5;
        double sides = 0.5;
        switch (mode) {
            case Full:
                up = p;
                down = p;
                sides = p;
                break;
            case Up:
                down = p * 2.0 - 0.5;
                break;
            case Down:
                up = p * 2.0 - 0.5;
                break;
            case Double:
                double p2 = p * 2.0 - 0.5;
                up = p2;
                down = p2;
                sides = p2;
        }

        return this.getBox(sides, up, down);
    }

    private void abort(BlockPos pos) {
        this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN, 0));
        this.started = false;
    }

    private Box getBox(double sides, double up, double down) {
        VoxelShape shape = BlackOut.mc.world.getBlockState(this.minePos).getOutlineShape(BlackOut.mc.world, this.minePos);
        if (shape.isEmpty()) {
            return null;
        } else {
            Box from = shape.getBoundingBox();
            Vec3d middle = BoxUtils.middle(from);
            Vec3d scale = new Vec3d(from.getLengthX(), from.getLengthY(), from.getLengthZ());
            return this.fromScale(middle, scale, sides, up, down).offset(this.minePos);
        }
    }

    private Box fromScale(Vec3d m, Vec3d s, double sides, double up, double down) {
        return new Box(
                m.x - sides * s.x,
                m.y - down * s.y,
                m.z - sides * s.z,
                m.x + sides * s.x,
                m.y + up * s.y,
                m.z + sides * s.z
        );
    }

    private BlackOutColor getSideColor(double p) {
        if (this.animationColor.get()) {
            return this.sideStartColor.get().lerp(p, this.sideEndColor.get());
        } else {
            return p > 0.9 ? this.sideEndColor.get() : this.sideStartColor.get();
        }
    }

    private BlackOutColor getLineColor(double p) {
        if (this.animationColor.get()) {
            return this.lineStartColor.get().lerp(p, this.lineEndColor.get());
        } else {
            return p > 0.9 ? this.lineEndColor.get() : this.lineStartColor.get();
        }
    }
}
