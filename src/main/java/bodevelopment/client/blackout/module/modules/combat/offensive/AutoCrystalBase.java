package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;

public class AutoCrystalBase extends ObsidianModule {
    public final SettingGroup sgPerformance = this.addGroup("Performance");

    private final Setting<Double> scoreImprove = this.sgGeneral.doubleSetting("Prioritize Score", 1.0, 0.0, 10.0, 0.5, "How much higher the damage must be to switch to a new base.");

    private final Setting<Integer> updateDelay = this.sgPerformance.intSetting("Update Delay", 2, 0, 20, 1, "How many ticks to wait between full re-calculations.");
    private final Setting<Double> searchRadius = this.sgPerformance.doubleSetting("Horizontal Radius", 4.0, 1.0, 6.0, 0.1, "Horizontal radius around target.");
    private final Setting<Integer> verticalRadius = this.sgPerformance.intSetting("Vertical Depth", 3, 1, 10, 1, "How many blocks to search up and down.");

    private PlayerEntity target = null;
    private BlockPos lastBestPos = null;
    private int internalTicks = 0;

    public AutoCrystalBase() {
        super("Auto Crystal Base", "Dynamic obsidian placement for crystals.", SubCategory.OFFENSIVE);
        this.attack.hide(false);
    }

    @Override
    protected void addInsideBlocks() {
        if (BlackOut.mc.world == null || BlackOut.mc.player == null) return;

        if (target == null || !target.isAlive() || BlackOut.mc.player.distanceTo(target) > 13.0) {
            this.target = BlackOut.mc.world.getPlayers().stream()
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
        BlockPos targetPos = target.getBlockPos();
        BlockPos currentBest = null;
        double maxScore = 0;

        double rH = searchRadius.get();
        int rV = verticalRadius.get();

        for (int x = (int)-rH; x <= rH; x++) {
            for (int z = (int)-rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;

                for (int y = -rV; y <= -1; y++) {
                    BlockPos pos = targetPos.add(x, y, z);

                    if (!isValidForBase(pos, ac)) continue;

                    double tDmg = getDmg(target, pos);
                    if (tDmg < ac.getMinPlace().get()) continue;

                    double sDmg = getDmg(BlackOut.mc.player, pos);
                    if (sDmg > ac.getMaxSelfPlace().get()) continue;

                    if (!isFriendSafe(pos, ac, tDmg)) continue;
                    if (ac.getCheckSelfPlacing().get() && (tDmg / Math.max(sDmg, 1.0)) < ac.getMinSelfRatio().get()) continue;

                    double score = tDmg + (pos.equals(lastBestPos) ? 1.5 : 0);

                    if (score > maxScore) {
                        maxScore = score;
                        currentBest = pos.toImmutable();
                    }
                }
            }
        }
        return currentBest;
    }

    private boolean isCurrentBaseBetter(BlockPos bestPos, AutoCrystal ac) {
        double bestScore = getDmg(target, bestPos);
        BlockPos targetPos = target.getBlockPos();

        double rH = searchRadius.get();
        int rV = verticalRadius.get();

        for (int x = (int)-rH; x <= rH; x++) {
            for (int z = (int)-rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;
                for (int y = -rV; y <= -1; y++) {
                    BlockPos pos = targetPos.add(x, y, z);

                    if (BlackOut.mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || BlackOut.mc.world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                        if (!ac.crystalBlock(pos.up()) || ac.intersects(pos.up())) continue;

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

        BlockPos crystalPos = pos.up();

        if (!BlackOut.mc.world.getBlockState(crystalPos).isAir() && !OLEPOSSUtils.replaceable(crystalPos)) {
            return false;
        }
        if (SettingUtils.oldCrystals() && !BlackOut.mc.world.getBlockState(crystalPos.up()).isAir()) {
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

    private boolean isFriendSafe(BlockPos pos, AutoCrystal ac, double targetDmg) {
        if (!ac.getCheckFriendPlacing().get()) return true;
        return BlackOut.mc.world.getPlayers().stream()
                .filter(p -> p != BlackOut.mc.player && Managers.FRIENDS.isFriend(p) && p.isAlive())
                .allMatch(friend -> {
                    double fDmg = getDmg(friend, pos);
                    return fDmg <= ac.getMaxFriendPlace().get()
                            && (targetDmg / Math.max(fDmg, 1.0)) >= ac.getMinFriendRatio().get();
                });
    }

    private double getDmg(PlayerEntity p, BlockPos pos) {
        return DamageUtils.crystalDamage(p, p.getBoundingBox(), pos.toCenterPos().add(0, 0.5, 0));
    }

    @Override protected boolean validForBlocking(Entity entity) { return false; }
    @Override protected double getCooldown() { return 0.1 * updateDelay.get(); }
}