/*
 * Copyright (c) 2026.
 * Original module logic by RegalMagic.
 * Refined by Limon_TH.
 */

package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.*;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AutoCrystalBase extends ObsidianModule {
    public final SettingGroup sgPerformance = this.addGroup("Performance");
    public final SettingGroup sgMine = this.addGroup("Smart Mine");

    private final Setting<Double> scoreImprove = this.sgGeneral.doubleSetting("Prioritize Score", 1.0, 0.0, 10.0, 0.5, "How much higher the damage must be to switch to a new base.");

    private final Setting<Integer> updateDelay = this.sgPerformance.intSetting("Update Delay", 2, 0, 20, 1, "How many ticks to wait between full re-calculations.");
    private final Setting<Double> searchRadius = this.sgPerformance.doubleSetting("Horizontal Radius", 4.0, 1.0, 6.0, 0.1, "Horizontal radius around target.");
    private final Setting<Integer> verticalRadius = this.sgPerformance.intSetting("Vertical Depth", 3, 1, 10, 1, "How many blocks to search up and down.");

    private final Setting<Boolean> smartMine = this.sgMine.booleanSetting("Smart Mine", false, "Break breakable blocks to create obsidian base spots for crystals.");
    private final Setting<SwitchMode> pickaxeSwitch = this.sgMine.enumSetting("Pickaxe Switch", SwitchMode.Silent, "Method to switch to a pickaxe when breaking blocks.", this.smartMine::get);
    private final Setting<Double> maxHardness = this.sgMine.doubleSetting("Max Hardness", 5.0, 0.5, 50.0, 0.5, "Maximum block hardness to attempt breaking.", this.smartMine::get);
    // --- What counts as a "valid existing base" that suppresses Smart Mine ---
    private final Setting<Boolean> checkExistingBases = this.sgMine.booleanSetting("Check Existing Bases", true,
            "Treat nearby obsidian/bedrock blocks as a valid base even if a crystal can't be placed there yet.", this.smartMine::get);
    private final Setting<Boolean> requireCrystalRange = this.sgMine.booleanSetting("Require Crystal Range", true,
            "Existing obsidian/bedrock only suppresses mining when the crystal spot above it is within attack range.",
            () -> this.smartMine.get() && this.checkExistingBases.get());
    private final Setting<Boolean> requireObsidianRange = this.sgMine.booleanSetting("Require Obsidian Range", true,
            "Air/replaceable spots only suppress mining when obsidian can actually be placed there (in place range).", this.smartMine::get);
    private final Setting<Double> minSuppressDmg = this.sgMine.doubleSetting("Min Suppress Damage", 6.0, 0.0, 36.0, 0.5,
            "Minimum crystal damage a base must provide to suppress Smart Mine activation.", this.smartMine::get);

    private Player target = null;
    private BlockPos lastBestPos = null;
    private int internalTicks = 0;

    // Smart mine state
    private BlockPos minePos = null;
    private double mineProgress = 0.0;
    private boolean mineStarted = false;
    private int mineSlot = -1;

    public AutoCrystalBase() {
        super("Auto Crystal Base", "Dynamic obsidian placement for crystals.", SubCategory.OFFENSIVE);
        this.attack.hide(false);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        abortMine();
    }

    @Event
    public void onTickMine(TickEvent.Pre event) {
        if (!PlayerUtils.isInGame() || !smartMine.get()) return;
        tickMining();
    }

    private void tickMining() {
        AutoCrystal ac = AutoCrystal.getInstance();
        if (ac == null || !ac.enabled || target == null) {
            abortMine();
            return;
        }

        // Only mine when there is truly no valid placement option (air/obsidian) near target
        if (hasValidPlacementOptions(ac)) {
            abortMine();
            return;
        }

        // Block already broken — find a new target
        if (minePos == null || BlockUtils.replaceable(minePos)) {
            mineStarted = false;
            mineProgress = 0.0;
            minePos = findMineTarget(ac);
            mineSlot = minePos == null ? -1 : findBestPickaxeSlot(minePos);
            return;
        }

        if (mineSlot < 0) return;

        Direction dir = SettingUtils.getPlaceOnDirection(minePos);
        if (dir == null) dir = Direction.UP;
        final Direction finalDir = dir;

        // Start mining
        if (!mineStarted) {
            if (SettingUtils.shouldRotate(RotationType.Mining)
                    && !rotation.rotateBlock(minePos, dir, minePos.getCenter(), RotationType.Mining, "base_mine")) {
                return;
            }
            sendSequenced(s -> new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, minePos, finalDir, s));
            mineStarted = true;
        }

        // Accumulate progress
        ItemStack bestStack = BlackOut.mc.player.getInventory().getItem(mineSlot);
        mineProgress += BlockUtils.getBlockBreakingDelta(minePos, bestStack);

        if (mineProgress >= 1.0) {
            finishMining(finalDir);
        }
    }

    private void finishMining(Direction dir) {
        if (BlockUtils.replaceable(minePos)) {
            resetMineState();
            return;
        }

        if (SettingUtils.shouldRotate(RotationType.Mining)
                && !rotation.rotateBlock(minePos, dir, minePos.getCenter(), RotationType.Mining, "base_mine")) {
            return;
        }

        boolean switched = pickaxeSwitch.get().swap(mineSlot);
        if (!switched && pickaxeSwitch.get() != SwitchMode.Disabled) return;

        final Direction finalDir = dir;
        final BlockPos finalPos = minePos;
        sendSequenced(s -> new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, finalPos, finalDir, s));

        if (switched) pickaxeSwitch.get().swapBack();

        BlackOut.mc.level.setBlockAndUpdate(finalPos, Blocks.AIR.defaultBlockState());
        Managers.BLOCK.set(finalPos, Blocks.AIR, true, true);

        if (SettingUtils.shouldRotate(RotationType.Mining)) rotation.end("base_mine");
        resetMineState();
    }

    private void abortMine() {
        if (mineStarted && minePos != null) {
            sendPacket(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, minePos, Direction.DOWN, 0));
        }
        resetMineState();
    }

    private void resetMineState() {
        minePos = null;
        mineProgress = 0.0;
        mineStarted = false;
        mineSlot = -1;
    }

    /**
     * Returns true if there is at least one base near the target that makes Smart Mine unnecessary.
     * What counts as "valid" is controlled by the Smart Mine settings:
     *  - checkExistingBases / requireCrystalRange control obsidian/bedrock evaluation
     *  - requireObsidianRange controls air-spot evaluation
     *  - minSuppressDmg is the damage threshold for suppression (independent of AutoCrystal's minPlace)
     */
    private boolean hasValidPlacementOptions(AutoCrystal ac) {
        if (target == null || BlackOut.mc.level == null) return false;
        BlockPos targetPos = target.blockPosition();
        double rH = searchRadius.get();
        int rV = verticalRadius.get();
        double minDmg = minSuppressDmg.get();

        for (int x = (int) -rH; x <= rH; x++) {
            for (int z = (int) -rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;
                for (int y = -rV; y <= -1; y++) {
                    BlockPos pos = targetPos.offset(x, y, z);
                    if (BlackOut.mc.level.isOutsideBuildHeight(pos.getY())) continue;

                    BlockState state = BlackOut.mc.level.getBlockState(pos);
                    boolean isExistingBase = state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
                    boolean isPlaceableAir = BlockUtils.replaceable(pos)
                            && (!requireObsidianRange.get() || SettingUtils.inPlaceRange(pos));

                    if (isExistingBase && checkExistingBases.get()) {
                        BlockPos crystalPos = pos.above();
                        if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos)) continue;
                        if (ac.intersects(crystalPos)) continue;
                        // Optionally require that crystal attack range is satisfied
                        if (requireCrystalRange.get() && !ac.inAttackRangePlacing(crystalPos)) continue;

                        double tDmg = getSimulatedDmg(target, pos);
                        if (tDmg < minDmg) continue;
                        double sDmg = getSimulatedDmg(BlackOut.mc.player, pos);
                        if (sDmg > ac.getMaxSelfPlace().get()) continue;
                        return true;
                    }

                    if (isPlaceableAir) {
                        BlockPos crystalPos = pos.above();
                        if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos)) continue;
                        if (ac.intersects(crystalPos)) continue;
                        if (!ac.inAttackRangePlacing(crystalPos)) continue;

                        double tDmg = getSimulatedDmg(target, pos);
                        if (tDmg < minDmg) continue;
                        double sDmg = getSimulatedDmg(BlackOut.mc.player, pos);
                        if (sDmg > ac.getMaxSelfPlace().get()) continue;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BlockPos findMineTarget(AutoCrystal ac) {
        if (target == null) return null;
        BlockPos targetPos = target.blockPosition();
        BlockPos best = null;
        double bestScore = 0;

        double rH = searchRadius.get();
        int rV = verticalRadius.get();

        for (int x = (int) -rH; x <= rH; x++) {
            for (int z = (int) -rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;
                for (int y = -rV; y <= -1; y++) {
                    BlockPos pos = targetPos.offset(x, y, z);
                    if (!isValidForMine(pos, ac)) continue;

                    double tDmg = getSimulatedDmg(target, pos);
                    if (tDmg < ac.getMinPlace().get()) continue;
                    double sDmg = getSimulatedDmg(BlackOut.mc.player, pos);
                    if (sDmg > ac.getMaxSelfPlace().get()) continue;

                    if (tDmg > bestScore) {
                        bestScore = tDmg;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private boolean isValidForMine(BlockPos pos, AutoCrystal ac) {
        if (BlackOut.mc.level == null || BlackOut.mc.level.isOutsideBuildHeight(pos.getY())) return false;

        BlockState state = BlackOut.mc.level.getBlockState(pos);

        // Only mine solid, non-replaceable blocks (replaceable = already handled by place logic)
        if (BlockUtils.replaceable(pos)) return false;
        // Already a valid crystal base — no need to mine
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) return false;
        // Unbreakable or too hard
        float hardness = state.getDestroySpeed(BlackOut.mc.level, pos);
        if (hardness < 0 || hardness > maxHardness.get()) return false;
        // Blast-resistant blocks (ancient debris etc)
        if (state.getBlock().getExplosionResistance() > 600.0f) return false;
        // Must be in mining range
        if (!SettingUtils.inMineRange(pos)) return false;

        // Crystal spot above must be free
        BlockPos crystalPos = pos.above();
        if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos)) return false;
        if (ac.intersects(crystalPos)) return false;
        if (!ac.inAttackRangePlacing(crystalPos)) return false;

        return true;
    }

    private int findBestPickaxeSlot(BlockPos pos) {
        FindResult result = InvUtils.findBest(
                pickaxeSwitch.get().hotbar,
                pickaxeSwitch.get().inventory,
                stack -> BlockUtils.getBlockBreakingDelta(pos, stack)
        );
        return result.wasFound() ? result.slot() : -1;
    }

    @Override
    protected void addInsideBlocks() {
        if (BlackOut.mc.level == null || BlackOut.mc.player == null) return;

        if (target == null || !target.isAlive() || BlackOut.mc.player.distanceTo(target) > 13.0) {
            this.target = BlackOut.mc.level.players().stream()
                    .filter(p -> p != BlackOut.mc.player && !Managers.FRIENDS.isFriend(p) && p.isAlive())
                    .filter(p -> BlackOut.mc.player.distanceTo(p) <= 12.0)
                    .min(Comparator.comparingDouble(p -> BlackOut.mc.player.distanceTo(p)))
                    .orElse(null);
        }
    }

    @Override
    protected void addPlacements() {
        if (this.target == null) {
            lastBestPos = null;
            return;
        }

        if (internalTicks < updateDelay.get()) {
            internalTicks++;
            if (lastBestPos != null) this.blockPlacements.add(lastBestPos);
            return;
        }
        internalTicks = 0;

        AutoCrystal ac = AutoCrystal.getInstance();
        if (ac == null || !ac.enabled) return;

        BlockPos bestPos = findOptimalBase(ac);

        if (bestPos != null) {
            if (!isCurrentBaseBetter(bestPos, ac)) {
                lastBestPos = bestPos;
                this.blockPlacements.add(bestPos);
            }
        }
    }

    private BlockPos findOptimalBase(AutoCrystal ac) {
        BlockPos targetPos = target.blockPosition();
        BlockPos currentBest = null;
        double maxScore = 0;

        double rH = searchRadius.get();
        int rV = verticalRadius.get();

        for (int x = (int)-rH; x <= rH; x++) {
            for (int z = (int)-rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;

                for (int y = -rV; y <= -1; y++) {
                    BlockPos pos = targetPos.offset(x, y, z);

                    if (!isValidForBase(pos, ac)) continue;

                    double tDmg = getSimulatedDmg(target, pos);

                    if (tDmg < ac.getMinPlace().get()) continue;

                    double sDmg = getSimulatedDmg(BlackOut.mc.player, pos);
                    if (sDmg > ac.getMaxSelfPlace().get()) continue;

                    if (!isFriendSafe(pos, ac, tDmg)) continue;
                    if (ac.getCheckSelfPlacing().get() && (tDmg / Math.max(sDmg, 1.0)) < ac.getMinSelfRatio().get()) continue;

                    double score = tDmg + (pos.equals(lastBestPos) ? 1.5 : 0);

                    if (score > maxScore) {
                        maxScore = score;
                        currentBest = pos.immutable();
                    }
                }
            }
        }
        return currentBest;
    }

    private boolean isCurrentBaseBetter(BlockPos bestPos, AutoCrystal ac) {
        double bestScore = getSimulatedDmg(target, bestPos);
        BlockPos targetPos = target.blockPosition();

        double rH = searchRadius.get();
        int rV = verticalRadius.get();

        for (int x = (int)-rH; x <= rH; x++) {
            for (int z = (int)-rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;
                for (int y = -rV; y <= -1; y++) {
                    BlockPos pos = targetPos.offset(x, y, z);

                    if (BlackOut.mc.level.getBlockState(pos).is(Blocks.OBSIDIAN) || BlackOut.mc.level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        if (!ac.crystalBlock(pos.above()) || ac.intersects(pos.above())) continue;
                        if (!ac.inAttackRangePlacing(pos.above())) continue;

                        double existingDmg = getDmg(target, pos);
                        if (existingDmg + scoreImprove.get() >= bestScore) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isValidForBase(BlockPos pos, AutoCrystal ac) {
        if (BlackOut.mc.level == null || BlackOut.mc.level.isOutsideBuildHeight(pos.getY())) return false;

        if (this.blockPlacements.contains(pos) || !SettingUtils.inPlaceRange(pos) || !BlockUtils.replaceable(pos)) {
            return false;
        }

        BlockPos crystalPos = pos.above();

        if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos)) {
            return false;
        }
        if (SettingUtils.oldCrystals() && !BlackOut.mc.level.getBlockState(crystalPos.above()).isAir()) {
            return false;
        }
        if (ac.intersects(crystalPos)) {
            return false;
        }
        return ac.inAttackRangePlacing(crystalPos);
    }

    private double getSimulatedDmg(Player p, BlockPos pos) {
        if (BlackOut.mc.level == null) return 0;

        net.minecraft.world.level.block.state.BlockState oldState = BlackOut.mc.level.getBlockState(pos);
        BlackOut.mc.level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 0);

        double dmg = DamageUtils.crystalDamage(p, p.getBoundingBox(), pos.getCenter().add(0, 0.5, 0));
        BlackOut.mc.level.setBlock(pos, oldState, 0);

        return dmg;
    }

    private boolean isFriendSafe(BlockPos pos, AutoCrystal ac, double targetDmg) {
        if (!ac.getCheckFriendPlacing().get()) return true;
        return BlackOut.mc.level.players().stream()
                .filter(p -> p != BlackOut.mc.player && Managers.FRIENDS.isFriend(p) && p.isAlive())
                .allMatch(friend -> {
                    double fDmg = getDmg(friend, pos);
                    return fDmg <= ac.getMaxFriendPlace().get()
                            && (targetDmg / Math.max(fDmg, 1.0)) >= ac.getMinFriendRatio().get();
                });
    }

    private double getDmg(Player p, BlockPos pos) {
        return DamageUtils.crystalDamage(p, p.getBoundingBox(), pos.getCenter().add(0, 0.5, 0));
    }

    @Override protected boolean validForBlocking(Entity entity) { return false; }
    @Override protected double getCooldown() { return 0.1 * updateDelay.get(); }
}