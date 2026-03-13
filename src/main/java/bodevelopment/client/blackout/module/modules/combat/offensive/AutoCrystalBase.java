/*
 * Copyright (c) 2026.
 * Original module logic by RegalMagic.
 * Refined by Limon_TH.
 */

package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.*;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public class AutoCrystalBase extends ObsidianModule {
    public final SettingGroup sgPerformance = this.addGroup("Performance");

    private final Setting<Double> scoreImprove = this.sgGeneral.doubleSetting("Prioritize Score", 1.0, 0.0, 10.0, 0.5, "How much higher the damage must be to switch to a new base.");

    private final Setting<Integer> updateDelay = this.sgPerformance.intSetting("Update Delay", 2, 0, 20, 1, "How many ticks to wait between full re-calculations.");
    private final Setting<Double> searchRadius = this.sgPerformance.doubleSetting("Horizontal Radius", 4.0, 1.0, 6.0, 0.1, "Horizontal radius around target.");
    private final Setting<Integer> verticalRadius = this.sgPerformance.intSetting("Vertical Depth", 3, 1, 10, 1, "How many blocks to search up and down.");

    private Player target = null;
    private BlockPos lastBestPos = null;
    private int internalTicks = 0;

    public AutoCrystalBase() {
        super("Auto Crystal Base", "Dynamic obsidian placement for crystals.", SubCategory.OFFENSIVE);
        this.attack.hide(false);
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

                        double existingDmg = getDmg(target, pos);
                        if (existingDmg + scoreImprove.get() >= bestScore) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isValidForBase(BlockPos pos, AutoCrystal ac) {
        if (this.blockPlacements.contains(pos) || !SettingUtils.inPlaceRange(pos) || !OLEPOSSUtils.replaceable(pos)) {
            return false;
        }

        BlockPos crystalPos = pos.above();

        if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !OLEPOSSUtils.replaceable(crystalPos)) {
            return false;
        }
        if (SettingUtils.oldCrystals() && !BlackOut.mc.level.getBlockState(crystalPos.above()).isAir()) {
            return false;
        }
        if (ac.intersects(crystalPos)) {
            return false;
        }
        if (!ac.inAttackRangePlacing(crystalPos)) {
            return false;
        }

        return true;
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