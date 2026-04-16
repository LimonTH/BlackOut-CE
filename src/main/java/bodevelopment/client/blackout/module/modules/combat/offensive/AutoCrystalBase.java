package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.settings.SwingSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.List;
// TODO: ПРОВЕРКА НА ЖИДКОСТЬ НЕ РАБОТАЕТ, ЗАЕБАЛА
public class AutoCrystalBase extends ObsidianModule {
    private static AutoCrystalBase INSTANCE;

    public final SettingGroup sgPerformance = this.addGroup("Performance");
    public final SettingGroup sgAutoMine = this.addGroup("Auto Mine");

    private final Setting<Double> scoreImprove = this.sgGeneral.doubleSetting("Prioritize Score", 1.0, 0.0, 10.0, 0.5, "How much higher the damage must be to switch to a new base.");

    private final Setting<Integer> updateDelay = this.sgPerformance.intSetting("Update Delay", 2, 0, 20, 1, "How many ticks to wait between full re-calculations.");
    private final Setting<Double> searchRadius = this.sgPerformance.doubleSetting("Horizontal Radius", 4.0, 1.0, 6.0, 0.1, "Horizontal radius around target.");
    private final Setting<Integer> verticalRadius = this.sgPerformance.intSetting("Vertical Depth", 3, 1, 10, 1, "How many blocks to search up and down.");

    private final Setting<Boolean> autoMineToggle = this.sgAutoMine.booleanSetting("Enable Auto Mine", true, "Master switch: mines obstacles blocking base placement.");
    private final Setting<Boolean> minePacket = this.sgAutoMine.booleanSetting("Packet Mine", true, "Sends mining packets without client-side block removal.", this.autoMineToggle::get);
    private final Setting<Boolean> pauseEat = this.sgAutoMine.booleanSetting("Pause on Consume", false, "Stops mining while eating or drinking.", this.autoMineToggle::get);
    private final Setting<Boolean> pauseSword = this.sgAutoMine.booleanSetting("Sword Safety", false, "Disables mining while holding a sword.", this.autoMineToggle::get);
    private final Setting<List<Block>> ignore = this.sgAutoMine.blockListSetting("Exclusion List", "Blocks that will never be targeted for mining.", this.autoMineToggle::get);
    private final Setting<Boolean> switchReset = this.sgAutoMine.booleanSetting("Switch Reset", true, "Aborts mining progress if held item changes.", this.autoMineToggle::get);
    private final Setting<Boolean> rangeReset = this.sgAutoMine.booleanSetting("Range Reset", true, "Cancels mining if block moves out of reach.", this.autoMineToggle::get);
    private final Setting<Boolean> ncpProgress = this.sgAutoMine.booleanSetting("NCP Validation", true, "Calculates speed based on NCP thresholds.", this.autoMineToggle::get);
    private final Setting<RotationMode> rotationMode = this.sgAutoMine.enumSetting("Rotation Mode", RotationMode.Both, "When to rotate head towards block.", this.autoMineToggle::get);
    private final Setting<Boolean> preSwitch = this.sgAutoMine.booleanSetting("Predictive Switch", false, "Swaps to pickaxe slightly before break.", this.autoMineToggle::get);
    private final Setting<SwitchMode> pickaxeSwitch = this.sgAutoMine.enumSetting("Pickaxe Swap Mode", SwitchMode.InvSwitch, "Method to equip pickaxe.", this.autoMineToggle::get);
    private final Setting<Boolean> allowInventory = this.sgAutoMine.booleanSetting("Inventory Mining", false, "Allows tools in inventory.", () -> this.autoMineToggle.get() && this.pickaxeSwitch.get().inventory);
    private final Setting<Double> speed = this.sgAutoMine.doubleSetting("Mining Speed Multiplier", 1.0, 0.0, 2.0, 0.05, "Global multiplier for block breaking speed.", this.autoMineToggle::get);
    private final Setting<Boolean> onGroundSpoof = this.sgAutoMine.booleanSetting("Ground Spoofing", false, "Fakes 'on ground' status while airborne.", this.autoMineToggle::get);
    private final Setting<Boolean> onGroundCheck = this.sgAutoMine.booleanSetting("Ground Penalty Check", true, "Applies vanilla 5x slowdown if not on ground.", () -> this.autoMineToggle.get() && !this.onGroundSpoof.get());
    private final Setting<Boolean> effectCheck = this.sgAutoMine.booleanSetting("Status Effect Scaling", true, "Adjusts speed based on Haste/Fatigue.", this.autoMineToggle::get);
    private final Setting<Boolean> waterCheck = this.sgAutoMine.booleanSetting("Fluid Penalty Check", true, "Applies vanilla slowdown in water.", this.autoMineToggle::get);

    private Player target = null;
    BlockPos lastBestPos = null;
    private int internalTicks = 0;

    private int cachedSurroundState = 0;
    private boolean cachedIsFar = false;

    public BlockPos minePos = null;
    public boolean started = false;
    private double progress = 0.0;
    private int minedFor = 0;
    private boolean shouldRestart = false;
    private boolean startedThisTick = false;

    public AutoCrystalBase() {
        super("Auto Crystal Base", "Dynamic obsidian placement and mining for crystals.", SubCategory.OFFENSIVE);
        INSTANCE = this;
        this.attack.hide(false);
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (this.switchReset.get() && event.packet instanceof ServerboundSetCarriedItemPacket) {
            this.shouldRestart = true;
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (!PlayerUtils.isInGame()) return;
        this.startedThisTick = false;

        if (this.shouldRestart) {
            if (this.minePos != null) this.abort(this.minePos);
            this.shouldRestart = false;
            this.started = false;
        }
        if (this.minePos != null) {
            if (!BlackOut.mc.level.getFluidState(this.minePos).isEmpty() ||
                    !BlackOut.mc.level.getFluidState(this.minePos.above()).isEmpty()) {

                this.abort(this.minePos);
                this.minePos = null;
                this.started = false;
                return;
            }
            if (this.autoMineToggle.get()) {
                if (this.rangeReset.get() && !SettingUtils.inMineRange(this.minePos)) {
                    this.abort(this.minePos);
                    this.minePos = null;
                    this.lastBestPos = null;
                    this.started = false;
                    return;
                }
                if (!this.started && !this.paused()) {
                    Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
                    if (!this.shouldRotateStart() || this.rotation.rotateBlock(this.minePos, dir, this.minePos.getCenter(), RotationType.Mining, "mining")) {
                        this.start(this.minePos);
                    }
                }
                this.updateMining();
            }
        }
    }

    @Override
    protected void addInsideBlocks() {
        if (BlackOut.mc.level == null || BlackOut.mc.player == null) return;

        this.target = BlackOut.mc.level.players().stream()
                .filter(p -> p != BlackOut.mc.player && !Managers.FRIENDS.isFriend(p) && p.isAlive())
                .filter(p -> BlackOut.mc.player.distanceTo(p) <= 12.0)
                .min(Comparator.comparingDouble(p -> BlackOut.mc.player.distanceTo(p)))
                .orElse(null);
    }

    @Override
    protected void addPlacements() {
        if (this.target == null) {
            lastBestPos = null;
            minePos = null;
            return;
        }

        AutoCrystal ac = AutoCrystal.getInstance();
        if (ac == null || !ac.enabled) return;

        if (internalTicks < updateDelay.get()) {
            internalTicks++;
            if (lastBestPos != null && minePos == null) {
                if (SettingUtils.inPlaceRange(lastBestPos)) {
                    this.blockPlacements.add(lastBestPos);
                } else {
                    lastBestPos = null;
                }
            }
            return;
        }
        internalTicks = 0;

        BlockPos targetPos = target.blockPosition();
        cachedSurroundState = getSurroundState(targetPos);
        cachedIsFar = BlackOut.mc.player.distanceToSqr(target) >= 16.0;

        BlockPos bestPos = findOptimalBase(ac);

        if (bestPos != null) {
            if (isCurrentBaseBetter(bestPos, ac)) {
                if (minePos != null) {
                    abort(minePos);
                    minePos = null;
                }
                return;
            }

            BlockPos obstacle = getObstacle(bestPos);

            if (obstacle != null && this.autoMineToggle.get()) {
                if (!obstacle.equals(minePos)) {
                    if (minePos != null) abort(minePos);
                    minePos = obstacle;
                    lastBestPos = bestPos;
                    started = false;
                }
            } else {
                if (minePos != null) {
                    abort(minePos);
                    minePos = null;
                }
                lastBestPos = bestPos;

                if (BlockUtils.replaceable(bestPos) && SettingUtils.inPlaceRange(bestPos)) {
                    this.blockPlacements.add(bestPos);
                }
            }
        } else {
            if (minePos != null) {
                abort(minePos);
                minePos = null;
            }
            lastBestPos = null;
        }
    }

    private BlockPos findOptimalBase(AutoCrystal ac) {
        BlockPos targetPos = target.blockPosition();
        BlockPos currentBest = null;
        double maxScore = 0;

        double rH = searchRadius.get();
        int rV = verticalRadius.get();
        double distToTargetSq = BlackOut.mc.player.distanceToSqr(target);

        for (int x = (int) -rH; x <= rH; x++) {
            for (int z = (int) -rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;

                for (int y = -1; y >= -rV; y--) {
                    BlockPos pos = targetPos.offset(x, y, z);

                    if (cachedIsFar && BlackOut.mc.player.distanceToSqr(pos.getCenter()) >= distToTargetSq) continue;
                    if (y == -1 && cachedSurroundState == 4) continue;

                    if (!isValidForBase(pos, ac)) continue;

                    double tDmg = getSimulatedDmg(target, pos);
                    if (tDmg < ac.getMinPlace().get()) continue;

                    double sDmg = getSimulatedDmg(BlackOut.mc.player, pos);
                    if (sDmg > ac.getMaxSelfPlace().get()) continue;
                    if (!isFriendSafe(pos, ac, tDmg)) continue;
                    if (ac.getCheckSelfPlacing().get() && (tDmg / Math.max(sDmg, 1.0)) < ac.getMinSelfRatio().get()) continue;

                    double score = tDmg + (pos.equals(lastBestPos) ? scoreImprove.get() : 0);

                    if (cachedSurroundState > 0) {
                        if (y == -1) score += 5.0;
                    } else {
                        if (y <= -2) score += 5.0;
                    }

                    double distSqToPos = BlackOut.mc.player.distanceToSqr(pos.getCenter());
                    score += Math.max(0, (25.0 - distSqToPos) * 0.1);

                    if (score > maxScore) {
                        maxScore = score;
                        currentBest = pos.immutable();
                    }
                }
            }
        }
        return currentBest;
    }


    private BlockPos getObstacle(BlockPos pos) {
        BlockPos crystalPos = pos.above();
        if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos)) {
            return crystalPos;
        }
        if (!BlackOut.mc.level.getBlockState(pos).is(Blocks.OBSIDIAN) && !BlackOut.mc.level.getBlockState(pos).is(Blocks.BEDROCK)) {
            if (!BlockUtils.replaceable(pos)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isValidForBase(BlockPos pos, AutoCrystal ac) {
        if (BlackOut.mc.level == null || BlackOut.mc.level.isOutsideBuildHeight(pos.getY())) return false;

        BlockPos crystalPos = pos.above();

        if (BlockUtils.isLiquid(crystalPos)) return false;
        if (BlockUtils.isLiquid(pos)) return false;

        if (this.autoMineToggle.get()) {
            BlockPos obstacle = getObstacle(pos);
            if (obstacle != null) {
                Block obstacleBlock = BlackOut.mc.level.getBlockState(obstacle).getBlock();

                if (obstacleBlock == Blocks.OBSIDIAN || obstacleBlock == Blocks.BEDROCK || obstacleBlock == Blocks.ENDER_CHEST) {
                    return false;
                }
                if (this.ignore.get().contains(obstacleBlock) || !BlockUtils.mineable(obstacle)) {
                    return false;
                }
                if (!SettingUtils.inMineRange(obstacle)) return false;
                if (SettingUtils.getPlaceOnDirection(obstacle) == null) return false;
            } else if (BlockUtils.replaceable(pos)) {
                PlaceData placeData = SettingUtils.getPlaceData(pos);
                if (!placeData.valid() || !SettingUtils.inPlaceRange(placeData.pos())) return false;
            } else {
                return false;
            }
        } else {
            if (!BlockUtils.replaceable(pos)) return false;
            if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos)) return false;

            PlaceData placeData = SettingUtils.getPlaceData(pos);
            if (!placeData.valid() || !SettingUtils.inPlaceRange(placeData.pos())) return false;
        }

        if (ac.intersects(crystalPos)) return false;
        return ac.inAttackRangePlacing(crystalPos);
    }

    private boolean isCurrentBaseBetter(BlockPos bestPos, AutoCrystal ac) {
        double bestScore = getSimulatedDmg(target, bestPos);

        BlockPos targetPos = target.blockPosition();
        double rH = searchRadius.get();
        int rV = verticalRadius.get();

        for (int x = (int) -rH; x <= rH; x++) {
            for (int z = (int) -rH; z <= rH; z++) {
                for (int y = -1; y >= -rV; y--) {
                    BlockPos pos = targetPos.offset(x, y, z);

                    if (BlackOut.mc.level.getBlockState(pos).is(Blocks.OBSIDIAN) || BlackOut.mc.level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        if (BlockUtils.isLiquid(pos.above()) || BlockUtils.isLiquid(pos)) continue;
                        if (!ac.crystalBlock(pos.above()) || ac.intersects(pos.above())) continue;

                        double existingDmg = getDmg(target, pos);
                        if (existingDmg < ac.getMinPlace().get()) continue;

                        double sDmg = getDmg(BlackOut.mc.player, pos);
                        if (sDmg > ac.getMaxSelfPlace().get()) continue;

                        double score = existingDmg;

                        int relY = pos.getY() - targetPos.getY();
                        if (cachedSurroundState > 0 && relY == -1) score += 5.0;
                        else if (cachedSurroundState == 0 && relY <= -2) score += 5.0;

                        double simulatedBestScore = bestScore;
                        int bestRelY = bestPos.getY() - targetPos.getY();
                        if (cachedSurroundState > 0 && bestRelY == -1) simulatedBestScore += 5.0;
                        else if (cachedSurroundState == 0 && bestRelY <= -2) simulatedBestScore += 5.0;

                        if (score + scoreImprove.get() >= simulatedBestScore) {
                            lastBestPos = pos;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private double getSimulatedDmg(Player p, BlockPos pos) {
        if (BlackOut.mc.level == null) return 0;

        BlockState oldBase = BlackOut.mc.level.getBlockState(pos);
        BlockPos crystalPos = pos.above();
        BlockState oldAbove = BlackOut.mc.level.getBlockState(crystalPos);

        if (!oldAbove.getFluidState().isEmpty()) return 0;

        boolean needsBase = !(oldBase.is(Blocks.OBSIDIAN) || oldBase.is(Blocks.BEDROCK));
        boolean needsAbove = !oldAbove.isAir();

        if (needsBase) BlackOut.mc.level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 0);
        if (needsAbove) BlackOut.mc.level.setBlock(crystalPos, Blocks.AIR.defaultBlockState(), 0);

        double dmg = DamageUtils.crystalDamage(p, p.getBoundingBox(), pos.getCenter().add(0, 0.5, 0));

        if (needsAbove) BlackOut.mc.level.setBlock(crystalPos, oldAbove, 0);
        if (needsBase) BlackOut.mc.level.setBlock(pos, oldBase, 0);

        return dmg;
    }

    private int getSurroundState(BlockPos targetPos) {
        int count = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Block b = BlackOut.mc.level.getBlockState(targetPos.relative(dir)).getBlock();
            if (b == Blocks.OBSIDIAN || b == Blocks.BEDROCK || b == Blocks.ENDER_CHEST
                    || b == Blocks.RESPAWN_ANCHOR || b == Blocks.CRYING_OBSIDIAN) {
                count++;
            }
        }
        return count;
    }

    private boolean isFriendSafe(BlockPos pos, AutoCrystal ac, double targetDmg) {
        if (!ac.getCheckFriendPlacing().get()) return true;
        return BlackOut.mc.level.players().stream()
                .filter(p -> p != BlackOut.mc.player && Managers.FRIENDS.isFriend(p) && p.isAlive())
                .allMatch(friend -> {
                    double fDmg = getSimulatedDmg(friend, pos);
                    return fDmg <= ac.getMaxFriendPlace().get()
                            && (targetDmg / Math.max(fDmg, 1.0)) >= ac.getMinFriendRatio().get();
                });
    }

    private double getDmg(Player p, BlockPos pos) {
        return DamageUtils.crystalDamage(p, p.getBoundingBox(), pos.getCenter().add(0, 0.5, 0));
    }

    private void updateMining() {
        if (this.minePos != null && !this.startedThisTick) {
            boolean holding = this.itemMinedCheck(Managers.PACKET.getStack());
            int slot = this.findBestSlot(stack -> BlockUtils.getBlockBreakingDelta(
                    this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(),
                    this.onGroundCheck.get() && !this.onGroundSpoof.get())).slot();
            ItemStack bestStack = holding ? Managers.PACKET.getStack() : BlackOut.mc.player.getInventory().getItem(slot);

            if (this.ncpProgress.get()) {
                this.minedFor++;
            } else {
                this.progress += BlockUtils.getBlockBreakingDelta(this.minePos, bestStack,
                        this.effectCheck.get(), this.waterCheck.get(),
                        this.onGroundCheck.get() && !this.onGroundSpoof.get());
            }

            if (this.minedCheck(bestStack)) {
                this.endMining(holding, slot);
            } else if (this.almostMined(bestStack) && this.shouldRotateEnd()) {
                this.preRotate();
            }
        }
    }

    private void start(BlockPos pos) {
        Direction dir = SettingUtils.getPlaceOnDirection(pos);
        if (dir != null) {
            this.started = true;
            this.startedThisTick = true;
            this.progress = 0.0;
            this.minedFor = 0;

            if (this.preSwitch.get()) {
                int slot = this.findBestSlot(stack -> BlockUtils.getBlockBreakingDelta(this.minePos, stack,
                        this.effectCheck.get(), this.waterCheck.get(),
                        this.onGroundCheck.get() && !this.onGroundSpoof.get())).slot();
                this.pickaxeSwitch.get().swap(slot);
            }

            this.sendSequenced(s -> new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir, s));
            SwingSettings.getInstance().mineSwing(SwingSettings.MiningSwingState.Start);
            this.rotation.end("mining");

            if (this.preSwitch.get()) {
                this.pickaxeSwitch.get().swapBack();
            }
        }
    }

    private void endMining(boolean holding, int slot) {
        if (!(BlackOut.mc.level.getBlockState(this.minePos).getBlock() instanceof AirBlock)) {
            if (SettingUtils.inMineRange(this.minePos)) {
                Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
                if (dir != null) {
                    if (!this.shouldRotateEnd() || this.rotation.rotateBlock(this.minePos, dir,
                            this.minePos.getCenter(), RotationType.Mining, "mining")) {
                        boolean switched = false;
                        if (holding || (switched = this.pickaxeSwitch.get().swap(slot))) {
                            this.sendSequenced(s -> new ServerboundPlayerActionPacket(
                                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, this.minePos, dir, s));
                            SwingSettings.getInstance().mineSwing(SwingSettings.MiningSwingState.End);

                            BlackOut.mc.level.setBlockAndUpdate(this.minePos, Blocks.AIR.defaultBlockState());

                            Managers.BLOCK.set(this.minePos, Blocks.AIR, true, true);
                            Managers.ENTITY.addSpawning(this.minePos);

                            this.started = false;
                            this.minePos = null;
                            this.rotation.end("mining");

                            if (switched) {
                                this.pickaxeSwitch.get().swapBack();
                            }
                        }
                    }
                }
            }
        }
    }

    private void abort(BlockPos pos) {
        this.sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN, 0));
        this.started = false;
    }

    private void preRotate() {
        if (!(BlackOut.mc.level.getBlockState(this.minePos).getBlock() instanceof AirBlock)) {
            if (SettingUtils.inMineRange(this.minePos)) {
                Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
                if (dir != null && this.shouldRotateEnd()) {
                    this.rotation.rotateBlock(this.minePos, dir, this.minePos.getCenter(), RotationType.Mining, "mining");
                }
            }
        }
    }

    private FindResult findBestSlot(EpicInterface<ItemStack, Double> test) {
        return InvUtils.findBest(this.pickaxeSwitch.get().hotbar, this.allowInventory.get(), test);
    }

    private boolean itemMinedCheck(ItemStack stack) {
        if (!this.ncpProgress.get()) {
            return this.progress * this.speed.get() >= 1.0;
        } else {
            return this.minedFor * this.speed.get() >= Math.ceil(1.0 / BlockUtils.getBlockBreakingDelta(
                    this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(),
                    this.onGroundCheck.get() || this.onGroundSpoof.get()));
        }
    }

    private boolean minedCheck(ItemStack stack) {
        if (this.itemMinedCheck(stack)) {
            return true;
        } else {
            if (this.onGroundSpoof.get()) Managers.PACKET.spoofOG(true);
            return false;
        }
    }

    private boolean almostMined(ItemStack stack) {
        if (BlackOut.mc.level.getBlockState(this.minePos).getBlock() instanceof AirBlock) return false;
        if (!SettingUtils.inMineRange(this.minePos)) return false;
        if (SettingUtils.getPlaceOnDirection(this.minePos) == null) return false;

        if (!this.ncpProgress.get()) {
            return this.progress >= 0.9;
        } else {
            return this.minedFor + 2 >= Math.ceil(1.0 / BlockUtils.getBlockBreakingDelta(
                    this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(),
                    this.onGroundCheck.get() || this.onGroundSpoof.get()));
        }
    }

    private boolean paused() {
        return (this.pauseEat.get() && BlackOut.mc.player.isUsingItem())
                || (this.pauseSword.get() && BlackOut.mc.player.getMainHandItem().getItem() instanceof SwordItem);
    }

    private boolean shouldRotateStart() {
        RotationMode mode = rotationMode.get();
        return mode == RotationMode.StartOnly || mode == RotationMode.Both || SettingUtils.shouldRotate(RotationType.Mining);
    }

    private boolean shouldRotateEnd() {
        RotationMode mode = rotationMode.get();
        return mode == RotationMode.EndOnly || mode == RotationMode.Both || SettingUtils.shouldRotate(RotationType.Mining);
    }

    @Override protected boolean validForBlocking(Entity entity) { return false; }
    @Override protected double getCooldown() { return 0.1 * updateDelay.get(); }
    public static AutoCrystalBase getInstance() { return INSTANCE; }

    public enum RotationMode {
        None, StartOnly, EndOnly, Both
    }
}
