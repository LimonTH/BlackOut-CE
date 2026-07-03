/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AutoCrystalBase extends ObsidianModule {
    private static AutoCrystalBase INSTANCE;

    public final SettingGroup sgPerformance = this.addGroup("Performance");

    private final Setting<Double> scoreImprove = this.sgGeneral.doubleSetting("Prioritize Score", 1.0, 0.0, 10.0, 0.5, "How much higher the damage must be to switch to a new base.");
    private final Setting<Integer> updateDelay = this.sgPerformance.intSetting("Update Delay", 2, 0, 20, 1, "How many ticks to wait between full re-calculations.");
    private final Setting<Double> searchRadius = this.sgPerformance.doubleSetting("Horizontal Radius", 4.0, 1.0, 6.0, 0.1, "Horizontal radius around target.");
    private final Setting<Integer> verticalRadius = this.sgPerformance.intSetting("Vertical Depth", 3, 1, 10, 1, "How many blocks to search up and down.");
    private final Setting<Double> mineScorePenalty = this.sgPerformance.doubleSetting("Mine Score Penalty", 3.0, 0.0, 15.0, 0.5, "Score reduction for positions requiring block mining to offset delay.");

    public Player target = null;
    public BlockPos bestBasePos = null;
    public BlockPos miningTarget = null;
    BlockPos lastBestPos = null;
    private int internalTicks = 0;
    private double lastFindScore = 0.0;

    public AutoCrystalBase() {
        super("Auto Crystal Base", "Dynamic obsidian placement and mining for crystals.", SubCategory.OFFENSIVE);
        INSTANCE = this;
        this.attack.hide(false);
    }

    public static AutoCrystalBase getInstance() {
        return INSTANCE;
    }

    @Override
    protected void addInsideBlocks() {
        if (BlackOut.mc.level == null || BlackOut.mc.player == null) return;

        int idx = Managers.POSITION.findClosest(e -> e instanceof Player p 
            && p != BlackOut.mc.player 
            && !Managers.FRIENDS.isFriend(p) 
            && p.isAlive() 
            && BlackOut.mc.player.distanceTo(p) <= 12.0);
        
        this.target = idx >= 0 ? (Player) Managers.POSITION.entity(idx) : null;
    }

    @Override
    protected void addPlacements() {
        if (this.target == null) {
            lastBestPos = null;
            bestBasePos = null;
            miningTarget = null;
            return;
        }

        if (internalTicks < updateDelay.get()) {
            internalTicks++;
            if (lastBestPos != null && bestBasePos == null) {
                if (!SettingUtils.inPlaceRange(lastBestPos)) lastBestPos = null;
                else this.blockPlacements.add(lastBestPos);
            }
            return;
        }
        internalTicks = 0;

        AutoCrystal ac = AutoCrystal.getInstance();
        if (ac == null || !ac.enabled) { bestBasePos = null; miningTarget = null; return; }

        BlockPos targetPos = target.blockPosition();
        int surroundState = getSurroundState(targetPos);
        boolean isFar = BlackOut.mc.player.distanceToSqr(target) >= 16.0;
        double px = BlackOut.mc.player.getX(), py = BlackOut.mc.player.getY(), pz = BlackOut.mc.player.getZ();

        // Pass 1
        BlockPos bestFree = findBestPos(ac, targetPos, surroundState, isFar, px, py, pz, false);
        double freeScore = lastFindScore;

        // Pass 2
        BlockPos bestMine = findBestPos(ac, targetPos, surroundState, isFar, px, py, pz, true);
        double mineScore = lastFindScore;

        BlockPos bestPos;
        boolean requiresMining;
        if (bestMine != null && (bestFree == null || mineScore > freeScore)) {
            bestPos = bestMine;
            requiresMining = true;
        } else {
            bestPos = bestFree;
            requiresMining = false;
        }

        if (bestPos != null) {
            BlockState baseState = BlackOut.mc.level.getBlockState(bestPos);
            boolean baseClear = BlockUtils.replaceable(bestPos) || !baseState.getFluidState().isEmpty();
            BlockState aboveState = BlackOut.mc.level.getBlockState(bestPos.above());
            boolean aboveClear = (aboveState.isAir() || BlockUtils.replaceable(bestPos.above())) && aboveState.getFluidState().isEmpty();

            if (requiresMining) {
                BlockPos abv = bestPos.above();
                boolean baseNeedsMining = !baseClear && !baseState.is(Blocks.OBSIDIAN) && !baseState.is(Blocks.BEDROCK) && BlockUtils.mineable(bestPos);
                boolean aboveNeedsMining = !aboveClear && BlockUtils.mineable(abv);

                if (aboveNeedsMining) {
                    miningTarget = abv;
                    bestBasePos = bestPos;
                    lastBestPos = bestPos;
                    if (baseNeedsMining) miningTarget = bestPos;
                } else if (baseNeedsMining) {
                    miningTarget = bestPos;
                    bestBasePos = bestPos;
                    lastBestPos = bestPos;
                } else {
                    miningTarget = null;
                    bestBasePos = null;
                    lastBestPos = bestPos;
                    if (baseClear && SettingUtils.inPlaceRange(bestPos)) {
                        this.blockPlacements.add(bestPos);
                    }
                }
            } else if (aboveClear) {
                miningTarget = null;
                bestBasePos = null;
                lastBestPos = bestPos;
                if (baseClear && SettingUtils.inPlaceRange(bestPos)) {
                    this.blockPlacements.add(bestPos);
                }
            } else {
                miningTarget = null;
                bestBasePos = bestPos;
                lastBestPos = bestPos;
            }
        } else {
            bestBasePos = null;
            miningTarget = null;
            lastBestPos = null;
        }
    }

    private BlockPos findBestPos(AutoCrystal ac, BlockPos targetPos, int surroundState, boolean isFar, double px, double py, double pz) {
        return findBestPos(ac, targetPos, surroundState, isFar, px, py, pz, false);
    }

    private BlockPos findBestPos(AutoCrystal ac, BlockPos targetPos, int surroundState, boolean isFar, double px, double py, double pz, boolean allowMineable) {
        double rH = searchRadius.get();
        int rV = verticalRadius.get();
        BlockPos best = null;
        double maxScore = -Double.MAX_VALUE;
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        for (int x = (int) -rH; x <= rH; x++) {
            for (int z = (int) -rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;
                for (int y = -1; y >= -rV; y--) {
                    mut.set(targetPos.getX() + x, targetPos.getY() + y, targetPos.getZ() + z);

                    if (isFar) {
                        double dx = mut.getX() + 0.5 - px, dy = mut.getY() + 0.5 - py, dz = mut.getZ() + 0.5 - pz;
                        if (dx * dx + dy * dy + dz * dz >= BlackOut.mc.player.distanceToSqr(target)) continue;
                    }
                    if (y == -1 && surroundState == 4) continue;
                    if (!isValid(mut, ac, allowMineable)) continue;

                    double tDmg = simDamage(target, mut);
                    if (tDmg <= 0.0 && BlockUtils.isLiquid(mut.above())) tDmg = ac.getMinPlace().get() + 0.5;
                    if (tDmg < ac.getMinPlace().get()) continue;

                    double sDmg = simDamage(BlackOut.mc.player, mut);
                    if (sDmg > ac.getMaxSelfPlace().get()) continue;
                    if (!friendSafe(mut, ac, tDmg)) continue;
                    if (ac.getCheckSelfPlacing().get() && (tDmg / Math.max(sDmg, 1.0)) < ac.getMinSelfRatio().get()) continue;

                    double score = tDmg + (mut.equals(lastBestPos) ? scoreImprove.get() : 0);
                    if (surroundState > 0) { if (y == -1) score += 5.0; }
                    else { if (y <= -2) score += 5.0; }

                    double dx2 = mut.getX() + 0.5 - px, dy2 = mut.getY() + 0.5 - py, dz2 = mut.getZ() + 0.5 - pz;
                    score += Math.max(0, (25.0 - (dx2 * dx2 + dy2 * dy2 + dz2 * dz2)) * 0.1);

                    if (allowMineable) score -= mineScorePenalty.get();

                    if (score > maxScore) { maxScore = score; best = mut.immutable(); }
                }
            }
        }
        lastFindScore = maxScore;
        return best;
    }

    private boolean isValid(BlockPos pos, AutoCrystal ac) {
        return isValid(pos, ac, false);
    }

    private boolean isValid(BlockPos pos, AutoCrystal ac, boolean allowMineable) {
        if (BlackOut.mc.level == null || BlackOut.mc.level.isOutsideBuildHeight(pos.getY())) return false;

        BlockState baseState = BlackOut.mc.level.getBlockState(pos);
        boolean existing = baseState.is(Blocks.OBSIDIAN) || baseState.is(Blocks.BEDROCK);
        boolean baseOk = existing || BlockUtils.replaceable(pos) || !baseState.getFluidState().isEmpty();
        if (!baseOk) {
            if (!allowMineable || !BlockUtils.mineable(pos)) return false;
        }

        BlockPos up = pos.above();
        BlockState upState = BlackOut.mc.level.getBlockState(up);
        boolean aboveOk = (upState.isAir() || BlockUtils.replaceable(up)) && upState.getFluidState().isEmpty();
        if (!aboveOk) {
            if (!allowMineable || !BlockUtils.mineable(up)) return false;
        }

        if (!existing && baseState.getFluidState().isEmpty() && !hasSupport(pos, true)) {
            if (!allowMineable || !BlockUtils.mineable(pos) || !hasSupport(pos, true)) return false;
        }
        if (ac.intersects(up)) return false;

        return ac.inAttackRangePlacing(up);
    }

    private double simDamage(Player p, BlockPos pos) {
        if (BlackOut.mc.level == null) return 0;
        BlockState oldBase = BlackOut.mc.level.getBlockState(pos);
        BlockPos up = pos.above();
        BlockState oldUp = BlackOut.mc.level.getBlockState(up);
        if (!oldUp.getFluidState().isEmpty()) return 0;

        boolean needBase = !(oldBase.is(Blocks.OBSIDIAN) || oldBase.is(Blocks.BEDROCK));
        boolean needUp = !oldUp.isAir();

        synchronized (BlackOut.mc.level) {
            if (needBase) BlackOut.mc.level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 0);
            if (needUp) BlackOut.mc.level.setBlock(up, Blocks.AIR.defaultBlockState(), 0);

            double dmg = DamageUtils.crystalDamage(p, p.getBoundingBox(), Managers.POSITION.vec3().get(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5));

            if (needUp) BlackOut.mc.level.setBlock(up, oldUp, 0);
            if (needBase) BlackOut.mc.level.setBlock(pos, oldBase, 0);
            return dmg;
        }
    }

    private int getSurroundState(BlockPos targetPos) {
        int n = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Block b = BlackOut.mc.level.getBlockState(targetPos.relative(dir)).getBlock();
            if (b == Blocks.OBSIDIAN || b == Blocks.BEDROCK || b == Blocks.ENDER_CHEST || b == Blocks.RESPAWN_ANCHOR || b == Blocks.CRYING_OBSIDIAN) n++;
        }
        return n;
    }

    private boolean friendSafe(BlockPos pos, AutoCrystal ac, double tDmg) {
        if (!ac.getCheckFriendPlacing().get()) return true;
        return BlackOut.mc.level.players().stream()
                .filter(p -> p != BlackOut.mc.player && Managers.FRIENDS.isFriend(p) && p.isAlive())
                .allMatch(f -> { double fd = simDamage(f, pos); return fd <= ac.getMaxFriendPlace().get() && (tDmg / Math.max(fd, 1.0)) >= ac.getMinFriendRatio().get(); });
    }

    @Override
    protected BlockPos findSupport(BlockPos pos) {
        if (!BlockUtils.replaceable(pos)) return null;

        BlockPos immediate = super.findSupport(pos);

        int maxH = (int) Math.ceil(searchRadius.get());
        int maxV = verticalRadius.get();

        double px = BlackOut.mc.player.getX();
        double py = BlackOut.mc.player.getY();
        double pz = BlackOut.mc.player.getZ();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction dir : Direction.values()) {
            int maxDist = dir.getAxis() == Direction.Axis.Y ? maxV : maxH;
            if (maxDist < 2) continue;

            int solidDist = -1;
            for (int dist = 2; dist <= maxDist; dist++) {
                BlockPos probe = pos.relative(dir, dist);
                BlockState probeState = BlackOut.mc.level.getBlockState(probe);
                boolean isSolid = probeState.getFluidState().isEmpty() && !BlockUtils.replaceable(probeState);
                if (isSolid) {
                    solidDist = dist;
                    break;
                }
            }
            if (solidDist == -1) continue;

            BlockPos supportPos = null;
            for (int back = solidDist - 1; back >= 1; back--) {
                BlockPos candidate = pos.relative(dir, back);
                if (this.blockPlacements.contains(candidate)
                        || this.insideBlocks.contains(candidate)
                        || EntityUtils.intersects(BoxUtils.get(candidate), entity -> entity instanceof Player && !entity.isSpectator())) {
                    continue;
                }
                if (!SettingUtils.getPlaceData(candidate, true).valid()) {
                    continue;
                }
                if (!SettingUtils.inPlaceRange(candidate)) {
                    continue;
                }
                supportPos = candidate;
                break;
            }
            if (supportPos == null) continue;

            double dX = (supportPos.getX() + 0.5) - px;
            double dY = (supportPos.getY() + 0.5) - py;
            double dZ = (supportPos.getZ() + 0.5) - pz;
            double dist = dX * dX + dY * dY + dZ * dZ;

            if (dir == Direction.DOWN || dir == Direction.UP) {
                dist += 16.0;
            }

            if (dist < bestDist) {
                bestDist = dist;
                best = supportPos;
            }
        }

        if (best != null) {
            BlockState posState = BlackOut.mc.level.getBlockState(pos);
            boolean isFluid = !posState.getFluidState().isEmpty();
            if (isFluid) return best;

            if (immediate != null) {
                double immX = (immediate.getX() + 0.5) - px;
                double immY = (immediate.getY() + 0.5) - py;
                double immZ = (immediate.getZ() + 0.5) - pz;
                double immDist = immX * immX + immY * immY + immZ * immZ;
                if (immDist < bestDist - 1.0) return immediate;
            }
            return best;
        }

        return immediate;
    }

    @Override protected boolean validForBlocking(Entity entity) { return false; }
    @Override protected double getCooldown() { return 0.1 * updateDelay.get(); }
}
