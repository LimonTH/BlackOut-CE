package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;

public class AutoCrystalBase extends ObsidianModule {
    private static AutoCrystalBase INSTANCE;

    public final SettingGroup sgPerformance = this.addGroup("Performance");

    private final Setting<Double> scoreImprove = this.sgGeneral.doubleSetting("Prioritize Score", 1.0, 0.0, 10.0, 0.5, "How much higher the damage must be to switch to a new base.");

    private final Setting<Integer> updateDelay = this.sgPerformance.intSetting("Update Delay", 2, 0, 20, 1, "How many ticks to wait between full re-calculations.");
    private final Setting<Double> searchRadius = this.sgPerformance.doubleSetting("Horizontal Radius", 4.0, 1.0, 6.0, 0.1, "Horizontal radius around target.");
    private final Setting<Integer> verticalRadius = this.sgPerformance.intSetting("Vertical Depth", 3, 1, 10, 1, "How many blocks to search up and down.");

    public Player target = null;
    public BlockPos bestBasePos = null;
    BlockPos lastBestPos = null;
    private int internalTicks = 0;
    private int cachedSurroundState = 0;
    private boolean cachedIsFar = false;

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
            bestBasePos = null;
            return;
        }

        if (internalTicks < updateDelay.get()) {
            internalTicks++;
            if (lastBestPos != null && bestBasePos == null) {
                if (!SettingUtils.inPlaceRange(lastBestPos)) {
                    lastBestPos = null;
                } else {
                    this.blockPlacements.add(lastBestPos);
                }
            }
            return;
        }
        internalTicks = 0;

        AutoCrystal ac = AutoCrystal.getInstance();
        if (ac == null || !ac.enabled) {
            bestBasePos = null;
            return;
        }

        BlockPos targetPos = target.blockPosition();
        cachedSurroundState = getSurroundState(targetPos);
        cachedIsFar = BlackOut.mc.player.distanceToSqr(target) >= 16.0;

        BlockPos bestPos = findOptimalBase(ac);

        if (bestPos != null) {
            if (isNearExistingBase(bestPos)) {
                lastBestPos = null;
                bestBasePos = null;
                return;
            }

            boolean baseClear = BlockUtils.replaceable(bestPos);
            boolean aboveClear = BlackOut.mc.level.getBlockState(bestPos.above()).isAir()
                    || BlockUtils.replaceable(bestPos.above());

            if (baseClear && aboveClear) {
                bestBasePos = null;
                lastBestPos = bestPos;
                if (SettingUtils.inPlaceRange(bestPos)) {
                    this.blockPlacements.add(bestPos);
                }
            } else {
                bestBasePos = bestPos;
                lastBestPos = bestPos;
            }
        } else {
            bestBasePos = null;
            lastBestPos = null;
        }
    }

    private BlockPos findOptimalBase(AutoCrystal ac) {
        BlockPos targetPos = target.blockPosition();
        BlockPos currentBest = null;
        double maxScore = -Double.MAX_VALUE;

        double rH = searchRadius.get();
        int rV = verticalRadius.get();
        double distToTargetSq = BlackOut.mc.player.distanceToSqr(target);
        double px = BlackOut.mc.player.getX();
        double py = BlackOut.mc.player.getY();
        double pz = BlackOut.mc.player.getZ();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = (int) -rH; x <= rH; x++) {
            for (int z = (int) -rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;

                for (int y = -1; y >= -rV; y--) {
                    mutablePos.set(targetPos.getX() + x, targetPos.getY() + y, targetPos.getZ() + z);

                    if (cachedIsFar) {
                        double dx = mutablePos.getX() + 0.5 - px;
                        double dy = mutablePos.getY() + 0.5 - py;
                        double dz = mutablePos.getZ() + 0.5 - pz;
                        if (dx * dx + dy * dy + dz * dz >= distToTargetSq) continue;
                    }
                    if (y == -1 && cachedSurroundState == 4) continue;

                    if (!isValidForBase(mutablePos, ac)) continue;

                    double tDmg = getSimulatedDmg(target, mutablePos);
                    if (tDmg <= 0.0 && BlockUtils.isLiquid(mutablePos.above())) {
                        tDmg = ac.getMinPlace().get() + 0.5;
                    }
                    if (tDmg < ac.getMinPlace().get()) continue;

                    double sDmg = getSimulatedDmg(BlackOut.mc.player, mutablePos);
                    if (sDmg > ac.getMaxSelfPlace().get()) continue;
                    if (!isFriendSafe(mutablePos, ac, tDmg)) continue;
                    if (ac.getCheckSelfPlacing().get() && (tDmg / Math.max(sDmg, 1.0)) < ac.getMinSelfRatio().get())
                        continue;

                    double score = tDmg + (mutablePos.equals(lastBestPos) ? scoreImprove.get() : 0);

                    if (cachedSurroundState > 0) {
                        if (y == -1) score += 5.0;
                    } else {
                        if (y <= -2) score += 5.0;
                    }

                    double dx2 = mutablePos.getX() + 0.5 - px;
                    double dy2 = mutablePos.getY() + 0.5 - py;
                    double dz2 = mutablePos.getZ() + 0.5 - pz;
                    score += Math.max(0, (25.0 - (dx2 * dx2 + dy2 * dy2 + dz2 * dz2)) * 0.1);

                    if (score > maxScore) {
                        maxScore = score;
                        currentBest = mutablePos.immutable();
                    }
                }
            }
        }
        return currentBest;
    }

    private boolean isValidForBase(BlockPos pos, AutoCrystal ac) {
        if (BlackOut.mc.level == null || BlackOut.mc.level.isOutsideBuildHeight(pos.getY())) return false;

        BlockPos crystalPos = pos.above();

        if (!BlockUtils.replaceable(pos)) return false;
        if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos))
            return false;

        if (!hasSupport(pos, true)) return false;

        if (ac.intersects(crystalPos)) return false;
        if (isBlockedByCrystal(pos)) return false;
        return ac.inAttackRangePlacing(crystalPos);
    }

    private boolean isNearExistingBase(BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos check = pos.offset(dx, 0, dz);
                if (BlackOut.mc.level.getBlockState(check).is(Blocks.OBSIDIAN)
                        || BlackOut.mc.level.getBlockState(check).is(Blocks.BEDROCK)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBlockedByCrystal(BlockPos pos) {
        return EntityUtils.intersects(Managers.POSITION.aabb().fromBlock(pos.getX(), pos.getY(), pos.getZ()), entity -> entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal);
    }

    private double getSimulatedDmg(Player p, BlockPos pos) {
        if (BlackOut.mc.level == null) return 0;

        BlockState oldBase = BlackOut.mc.level.getBlockState(pos);
        BlockPos crystalPos = pos.above();
        BlockState oldAbove = BlackOut.mc.level.getBlockState(crystalPos);

        if (!oldAbove.getFluidState().isEmpty()) return 0;

        boolean needsBase = !(oldBase.is(Blocks.OBSIDIAN) || oldBase.is(Blocks.BEDROCK));
        boolean needsAbove = !oldAbove.isAir();

        synchronized (BlackOut.mc.level) {
            if (needsBase) BlackOut.mc.level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 0);
            if (needsAbove) BlackOut.mc.level.setBlock(crystalPos, Blocks.AIR.defaultBlockState(), 0);

            double dmg = DamageUtils.crystalDamage(p, p.getBoundingBox(), Managers.POSITION.vec3().get(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5));

            if (needsAbove) BlackOut.mc.level.setBlock(crystalPos, oldAbove, 0);
            if (needsBase) BlackOut.mc.level.setBlock(pos, oldBase, 0);

            return dmg;
        }
    }

    private int getSurroundState(BlockPos targetPos) {
        int count = 0;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
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

    @Override
    protected boolean validForBlocking(Entity entity) {
        return false;
    }

    @Override
    protected double getCooldown() {
        return 0.1 * updateDelay.get();
    }
}
