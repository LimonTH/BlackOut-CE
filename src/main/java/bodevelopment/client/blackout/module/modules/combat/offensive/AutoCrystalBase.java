package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.ObsidianModule;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.List;

public class AutoCrystalBase extends ObsidianModule {
    private static AutoCrystalBase INSTANCE;

    public final SettingGroup sgPerformance = this.addGroup("Performance");
    public final SettingGroup sgAutoMine = this.addGroup("Auto Mine");

    private final Setting<Double> scoreImprove = this.sgGeneral.doubleSetting("Prioritize Score", 1.0, 0.0, 10.0, 0.5, "How much higher the damage must be to switch to a new base.");

    private final Setting<Integer> updateDelay = this.sgPerformance.intSetting("Update Delay", 2, 0, 20, 1, "How many ticks to wait between full re-calculations.");
    private final Setting<Double> searchRadius = this.sgPerformance.doubleSetting("Horizontal Radius", 4.0, 1.0, 6.0, 0.1, "Horizontal radius around target.");
    private final Setting<Integer> verticalRadius = this.sgPerformance.intSetting("Vertical Depth", 3, 1, 10, 1, "How many blocks to search up and down.");

    private final Setting<Boolean> autoMineToggle = this.sgAutoMine.booleanSetting("Enable Auto Mine", true, "Master switch: mines obstacles blocking base placement.");
    private final Setting<Boolean> pauseEat = this.sgAutoMine.booleanSetting("Pause on Consume", false, "Stops mining while eating or drinking.", this.autoMineToggle::get);
    private final Setting<Boolean> pauseSword = this.sgAutoMine.booleanSetting("Sword Safety", false, "Disables mining while holding a sword.", this.autoMineToggle::get);
    private final Setting<List<Block>> ignore = this.sgAutoMine.blockListSetting("Exclusion List", "Blocks that will never be targeted for mining.", this.autoMineToggle::get);
    private final Setting<Boolean> ncpProgress = this.sgAutoMine.booleanSetting("NCP Validation", true, "Calculates speed based on NCP thresholds.", this.autoMineToggle::get);
    private final Setting<SwitchMode> pickaxeSwitch = this.sgAutoMine.enumSetting("Pickaxe Swap Mode", SwitchMode.InvSwitch, "Method to equip pickaxe.", this.autoMineToggle::get);
    private final Setting<Boolean> allowInventory = this.sgAutoMine.booleanSetting("Inventory Mining", false, "Allows tools in inventory.", () -> this.autoMineToggle.get() && this.pickaxeSwitch.get().inventory);
    private final Setting<Double> speed = this.sgAutoMine.doubleSetting("Mining Speed Multiplier", 1.0, 0.0, 2.0, 0.05, "Global multiplier for block breaking speed.", this.autoMineToggle::get);
    private final Setting<Boolean> onGroundSpoof = this.sgAutoMine.booleanSetting("Ground Spoofing", false, "Fakes 'on ground' status while airborne.", this.autoMineToggle::get);
    private final Setting<Boolean> onGroundCheck = this.sgAutoMine.booleanSetting("Ground Penalty Check", true, "Applies vanilla 5x slowdown if not on ground.", () -> this.autoMineToggle.get() && !this.onGroundSpoof.get());
    private final Setting<Boolean> effectCheck = this.sgAutoMine.booleanSetting("Status Effect Scaling", true, "Adjusts speed based on Haste/Fatigue.", this.autoMineToggle::get);
    private final Setting<Boolean> waterCheck = this.sgAutoMine.booleanSetting("Fluid Penalty Check", true, "Applies vanilla slowdown in water.", this.autoMineToggle::get);

    public Player target = null;
    BlockPos lastBestPos = null;
    private int internalTicks = 0;

    private int cachedSurroundState = 0;
    private boolean cachedIsFar = false;

    public BlockPos minePos = null;

    public AutoCrystalBase() {
        super("Auto Crystal Base", "Dynamic obsidian placement and mining for crystals.", SubCategory.OFFENSIVE);
        INSTANCE = this;
        this.attack.hide(false);
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

        if (internalTicks < updateDelay.get()) {
            internalTicks++;
            if (lastBestPos != null && minePos == null) {
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
            minePos = null;
            return;
        }

        BlockPos targetPos = target.blockPosition();
        cachedSurroundState = getSurroundState(targetPos);
        cachedIsFar = BlackOut.mc.player.distanceToSqr(target) >= 16.0;

        BlockPos bestPos = findOptimalBase(ac);

        if (bestPos != null) {
            if (isNearExistingBase(bestPos)) {
                lastBestPos = null;
                minePos = null;
                return;
            }

            BlockPos obstacle = getObstacle(bestPos);

            if (obstacle != null && this.autoMineToggle.get()) {
                minePos = obstacle;
                lastBestPos = bestPos;
            } else {
                minePos = null;
                lastBestPos = bestPos;
                if (BlockUtils.replaceable(bestPos) && SettingUtils.inPlaceRange(bestPos)) {
                    this.blockPlacements.add(bestPos);
                }
            }
        } else {
            minePos = null;
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

        for (int x = (int) -rH; x <= rH; x++) {
            for (int z = (int) -rH; z <= rH; z++) {
                if (x * x + z * z > rH * rH) continue;

                for (int y = -1; y >= -rV; y--) {
                    BlockPos pos = targetPos.offset(x, y, z);

                    if (cachedIsFar && BlackOut.mc.player.distanceToSqr(pos.getCenter()) >= distToTargetSq) continue;
                    if (y == -1 && cachedSurroundState == 4) continue;

                    if (!isValidForBase(pos, ac)) continue;

                    double tDmg = getSimulatedDmg(target, pos);
                    if (tDmg <= 0.0 && BlockUtils.isLiquid(pos.above())) {
                        tDmg = ac.getMinPlace().get() + 0.5;
                    }
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

                    if (this.autoMineToggle.get()) {
                        BlockPos obstacle = getObstacle(pos);
                        if (obstacle != null) {
                            double bestDelta = getBestBreakDelta(obstacle);
                            if (bestDelta > 0.0) {
                                double estimatedTicks = Math.ceil(1.0 / bestDelta) / this.speed.get();
                                score -= estimatedTicks * 0.2;
                            }
                        }
                    }

                    if (score > maxScore) {
                        maxScore = score;
                        currentBest = pos.immutable();
                    }
                }
            }
        }
        return currentBest;
    }

    private double getBestBreakDelta(BlockPos pos) {
        int slot = findBestSlot(stack -> BlockUtils.getBlockBreakingDelta(
                pos, stack, this.effectCheck.get(), this.waterCheck.get(),
                this.onGroundCheck.get() && !this.onGroundSpoof.get())).slot();
        ItemStack bestStack = BlackOut.mc.player.getInventory().getItem(slot);
        return BlockUtils.getBlockBreakingDelta(pos, bestStack,
                this.effectCheck.get(), this.waterCheck.get(),
                this.onGroundCheck.get() && !this.onGroundSpoof.get());
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
                if (!hasSupport(pos, true)) return false;
            } else {
                return false;
            }
        } else {
            if (!BlockUtils.replaceable(pos)) return false;
            if (!BlackOut.mc.level.getBlockState(crystalPos).isAir() && !BlockUtils.replaceable(crystalPos)) return false;

            if (!hasSupport(pos, true)) return false;
        }

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
        return EntityUtils.intersects(BoxUtils.get(pos), entity -> entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal);
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

    private double getDmg(Player p, BlockPos pos) {
        return DamageUtils.crystalDamage(p, p.getBoundingBox(), pos.getCenter().add(0, 0.5, 0));
    }

    private FindResult findBestSlot(EpicInterface<ItemStack, Double> test) {
        return InvUtils.findBest(this.pickaxeSwitch.get().hotbar, this.allowInventory.get(), test);
    }

    @Override protected boolean validForBlocking(Entity entity) { return false; }
    @Override protected double getCooldown() { return 0.1 * updateDelay.get(); }
    public static AutoCrystalBase getInstance() { return INSTANCE; }
}
