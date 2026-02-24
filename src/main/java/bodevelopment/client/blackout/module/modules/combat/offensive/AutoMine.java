package bodevelopment.client.blackout.module.modules.combat.offensive;

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
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.settings.SwingSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class AutoMine extends Module {
    private static AutoMine INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgSwitch = this.addGroup("Switch");
    private final SettingGroup sgSpeed = this.addGroup("Speed");
    private final SettingGroup sgCrystals = this.addGroup("Crystals");
    private final SettingGroup sgCev = this.addGroup("Cev");
    private final SettingGroup sgTrapCev = this.addGroup("Trap Cev");
    private final SettingGroup sgSurroundCev = this.addGroup("Surround Cev");
    private final SettingGroup sgAntiSurround = this.addGroup("Anti Surround");
    private final SettingGroup sgAntiBurrow = this.addGroup("Anti Burrow");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Boolean> pauseEat = this.sgGeneral.booleanSetting("Pause on Consume", false, "Stops mining operations while eating or drinking.");
    private final Setting<Boolean> pauseEatPlacing = this.sgGeneral.booleanSetting("Pause Placement on Consume", false, "Prevents crystal placement while eating.");
    private final Setting<Boolean> pauseSword = this.sgGeneral.booleanSetting("Sword Safety", false, "Disables mining while holding a sword to prevent accidental durability loss.");
    private final Setting<Boolean> packet = this.sgGeneral.booleanSetting("Packet Mine", true, "Sends mining packets without client-side block removal to prevent desync.");
    private final Setting<Boolean> autoMine = this.sgGeneral.booleanSetting("Auto-Selection", true, "Automatically identifies and targets the most optimal block for mining.");
    private final Setting<Boolean> manualMine = this.sgGeneral.booleanSetting("Manual Selection", true, "Allows the user to manually select a target block by clicking it.");
    private final Setting<Boolean> manualInstant = this.sgGeneral.booleanSetting("Manual Instant-Mine", false, "Enables instant-mining logic for manually selected blocks.", this.manualMine::get);
    private final Setting<Boolean> manualRemine = this.sgGeneral.booleanSetting("Manual Persistence", false, "Automatically restarts mining on the manual block after it is broken.", () -> this.manualMine.get() && !this.manualInstant.get());
    private final Setting<Boolean> fastRemine = this.sgGeneral.booleanSetting("Accelerated Remine", false, "Predicts mining progress based on the last block break timing for faster cycles.", () -> this.manualMine.get() && !this.manualInstant.get() && this.manualRemine.get());
    private final Setting<Boolean> manualRangeReset = this.sgGeneral.booleanSetting("Range Reset", true, "Cancels manual mining if the target block moves out of reach.", this.manualMine::get);
    private final Setting<Boolean> resetOnSwitch = this.sgGeneral.booleanSetting("Switch Reset", true, "Aborts mining progress if the player changes their held item.");
    private final Setting<Boolean> ncpProgress = this.sgGeneral.booleanSetting("NCP Validation", true, "Calculates mining speed based on NoCheatPlus-compatible progress thresholds.");
    private final Setting<Boolean> damageSync = this.sgGeneral.booleanSetting("Damage Synchronization", false, "Delays block breaks to synchronize with the target's invulnerability frames.");
    private final Setting<Integer> syncPredict = this.sgGeneral.intSetting("Sync Prediction Ticks", 0, 0, 10, 1, "The number of ticks to look ahead for damage synchronization.", this.damageSync::get);
    private final Setting<Integer> syncLength = this.sgGeneral.intSetting("Sync Window", 2, 0, 10, 1, "The duration of the synchronization window in ticks.", this.damageSync::get);
    private final Setting<Boolean> useMineBind = this.sgGeneral.booleanSetting("Manual Trigger", false, "Requires a specific keypress to execute the final block break.");
    private final Setting<KeyBind> mineBind = this.sgGeneral.keySetting("Mining Hotkey", "Keybind used for manual block breaking.", this.useMineBind::get);
    private final Setting<List<Block>> ignore = this.sgGeneral.blockListSetting("Exclusion List", "Blocks that will never be automatically targeted for mining.");

    private final Setting<Boolean> preSwitch = this.sgSwitch.booleanSetting("Predictive Switch", false, "Swaps to the pickaxe slightly before the block is ready to break.");
    private final Setting<SwitchMode> pickaxeSwitch = this.sgSwitch.enumSetting("Pickaxe Swap Mode", SwitchMode.InvSwitch, "The method used to equip the pickaxe for mining.");
    private final Setting<Boolean> allowInventory = this.sgSwitch.booleanSetting("Inventory Mining", false, "Allows using tools located in the inventory rather than just the hotbar.", () -> this.pickaxeSwitch.get().inventory);
    private final Setting<SwitchMode> crystalSwitch = this.sgSwitch.enumSetting("Crystal Swap Mode", SwitchMode.InvSwitch, "The method used to equip crystals for offensive mining.");

    private final Setting<Double> speed = this.sgSpeed.doubleSetting("Mining Speed Multiplier", 1.0, 0.0, 2.0, 0.05, "Global multiplier for block breaking speed.");
    private final Setting<Boolean> onGroundSpoof = this.sgSpeed.booleanSetting("Ground Spoofing", false, "Fakes the 'on ground' status to maintain mining speed while airborne.");
    private final Setting<Boolean> onGroundCheck = this.sgSpeed.booleanSetting("Ground Penalty Check", true, "Applies the vanilla 5x mining slowdown if not standing on solid ground.", () -> !this.onGroundSpoof.get());
    private final Setting<Boolean> effectCheck = this.sgSpeed.booleanSetting("Status Effect Scaling", true, "Adjusts mining speed based on Haste and Mining Fatigue effects.");
    private final Setting<Boolean> waterCheck = this.sgSpeed.booleanSetting("Fluid Penalty Check", true, "Applies the vanilla 5x mining slowdown while submerged in water.");

    private final Setting<Double> placeSpeed = this.sgCrystals.doubleSetting("Crystal Placement Rate", 2.0, 0.0, 20.0, 0.1, "The frequency of crystal placements per second during mining.");
    private final Setting<Double> attackSpeed = this.sgCrystals.doubleSetting("Crystal Attack Rate", 2.0, 0.0, 20.0, 0.1, "The frequency of crystal detonations per second.");
    private final Setting<Double> attackTime = this.sgCrystals.doubleSetting("Detonation Window", 2.0, 0.0, 10.0, 0.1, "The duration in seconds to keep attacking a placed crystal.");

    private final Setting<Priority> cevPriority = this.sgCev.enumSetting("CEV Priority", Priority.Normal, "Priority level for Crystal-End-Vantage (Top-down) attacks.");
    private final Setting<Boolean> cevDamageCheck = this.sgCev.booleanSetting("CEV Safety Check", true, "Ensures CEV placement deals damage to the target and not the player.", () -> this.cevPriority.get() != Priority.Disabled);
    private final Setting<Double> minCevDamage = this.sgCev.doubleSetting("CEV Minimum Damage", 6.0, 0.0, 20.0, 0.5, "Required damage to target for CEV initiation.", () -> this.cevPriority.get() != Priority.Disabled && this.cevDamageCheck.get());
    private final Setting<Double> maxCevDamage = this.sgCev.doubleSetting("CEV Max Self-Damage", 10.0, 0.0, 20.0, 0.5, "Maximum allowed damage to the player during CEV.", () -> this.cevPriority.get() != Priority.Disabled && this.cevDamageCheck.get());
    private final Setting<Boolean> instantCev = this.sgCev.booleanSetting("CEV Instant-Mine", false, "Sends minimal packets for maximum speed during CEV sequences.", () -> this.cevPriority.get() != Priority.Disabled);
    private final Setting<Boolean> antiAntiCev = this.sgCev.booleanSetting("High-Speed CEV", false, "Attempts to place crystals and mine simultaneously for maximum bypass.", () -> this.cevPriority.get() != Priority.Disabled);

    private final Setting<Priority> trapCevPriority = this.sgTrapCev.enumSetting("Trap-CEV Priority", Priority.Normal, "Priority level for mining out head-level trap blocks.");
    private final Setting<Boolean> trapCevDamageCheck = this.sgTrapCev.booleanSetting("Trap-CEV Safety", true, "Enforces damage thresholds for Trap-CEV placements.", () -> this.trapCevPriority.get() != Priority.Disabled);
    private final Setting<Double> minTrapCevDamage = this.sgTrapCev.doubleSetting("Trap-CEV Min Damage", 6.0, 0.0, 20.0, 0.5, "Required target damage for Trap-CEV.", () -> this.trapCevPriority.get() != Priority.Disabled && this.trapCevDamageCheck.get());
    private final Setting<Double> maxTrapCevDamage = this.sgTrapCev.doubleSetting("Trap-CEV Max Self-Damage", 10.0, 0.0, 20.0, 0.5, "Max allowed self-damage for Trap-CEV.", () -> this.trapCevPriority.get() != Priority.Disabled && this.trapCevDamageCheck.get());
    private final Setting<Boolean> instantTrapCev = this.sgTrapCev.booleanSetting("Trap-CEV Instant-Mine", false, "Optimizes packet throughput for Trap-CEV mining.", () -> this.trapCevPriority.get() != Priority.Disabled);
    private final Setting<Boolean> antiAntiTrapCev = this.sgTrapCev.booleanSetting("Trap-CEV Bypass", false, "Synchronized placement and mining for trap blocks.", () -> this.trapCevPriority.get() != Priority.Disabled);

    private final Setting<Priority> surroundCevPriority = this.sgSurroundCev.enumSetting("Surround-CEV Priority", Priority.Normal, "Priority for targeting the opponent's feet-level surround blocks.");
    private final Setting<Boolean> surroundCevDamageCheck = this.sgSurroundCev.booleanSetting("Surround-CEV Safety", true, "Enforces damage thresholds for Surround-CEV.", () -> this.surroundCevPriority.get() != Priority.Disabled);
    private final Setting<Double> minSurroundCevDamage = this.sgSurroundCev.doubleSetting("Surround-CEV Min Damage", 6.0, 0.0, 20.0, 0.5, "Required target damage for Surround-CEV.", () -> this.surroundCevPriority.get() != Priority.Disabled && this.surroundCevDamageCheck.get());
    private final Setting<Double> maxSurroundCevDamage = this.sgSurroundCev.doubleSetting("Surround-CEV Max Self-Damage", 10.0, 0.0, 20.0, 0.5, "Max allowed self-damage for Surround-CEV.", () -> this.surroundCevPriority.get() != Priority.Disabled && this.surroundCevDamageCheck.get());
    private final Setting<Boolean> instantSurroundCev = this.sgSurroundCev.booleanSetting("Surround-CEV Instant-Mine", false, "Optimizes packet throughput for surround mining.", () -> this.surroundCevPriority.get() != Priority.Disabled);
    private final Setting<Boolean> antiAntiSurroundCev = this.sgSurroundCev.booleanSetting("Surround-CEV Bypass", false, "Synchronized placement and mining for feet-level blocks.", () -> this.surroundCevPriority.get() != Priority.Disabled);
    private final Setting<Boolean> acceptCollide = this.sgSurroundCev.booleanSetting("Collision Tolerance", false, "Allows placement of crystals that collide with the target block rather than sitting on top.", () -> this.surroundCevPriority.get() != Priority.Disabled);

    private final Setting<Priority> autoCityPriority = this.sgAntiSurround.enumSetting("Auto-City Priority", Priority.Normal, "Priority for mining blocks adjacent to the target to facilitate crystal hits.");
    private final Setting<Boolean> autoCityDamageCheck = this.sgAntiSurround.booleanSetting("City Safety Check", true, "Enforces damage thresholds for City placements.", () -> this.autoCityPriority.get() != Priority.Disabled);
    private final Setting<Double> minAutoCityDamage = this.sgAntiSurround.doubleSetting("City Minimum Damage", 6.0, 0.0, 20.0, 0.5, "Required target damage for City mining.", () -> this.autoCityPriority.get() != Priority.Disabled && this.autoCityDamageCheck.get());
    private final Setting<Double> maxAutoCityDamage = this.sgAntiSurround.doubleSetting("City Max Self-Damage", 10.0, 0.0, 20.0, 0.5, "Max allowed self-damage for City mining.", () -> this.autoCityPriority.get() != Priority.Disabled && this.autoCityDamageCheck.get());
    private final Setting<Boolean> instantAutoCity = this.sgAntiSurround.booleanSetting("City Instant-Mine", false, "Optimizes packet throughput for City mining.", () -> this.autoCityPriority.get() != Priority.Disabled);
    private final Setting<Boolean> placeCrystal = this.sgAntiSurround.booleanSetting("City Placement", true, "Automatically places a crystal after a successful city-mine.", () -> this.autoCityPriority.get() != Priority.Disabled);
    private final Setting<Boolean> attackCrystal = this.sgAntiSurround.booleanSetting("City Detonation", false, "Automatically attacks the crystal placed during city mining.", () -> this.autoCityPriority.get() != Priority.Disabled);

    private final Setting<Priority> antiBurrowPriority = this.sgAntiBurrow.enumSetting("Anti-Burrow Priority", Priority.Normal, "Priority for mining blocks inside the target's feet (burrowed blocks).");

    private final Setting<Boolean> mineStartSwing = this.sgRender.booleanSetting("Initiation Animation", false, "Shows a hand swing when starting a mining operation.");
    private final Setting<Boolean> mineEndSwing = this.sgRender.booleanSetting("Completion Animation", false, "Shows a hand swing when the block is broken.");
    private final Setting<SwingHand> mineHand = this.sgRender.enumSetting("Mining Arm", SwingHand.RealHand, "The arm used for mining animations.", () -> this.mineStartSwing.get() || this.mineEndSwing.get());
    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Placement Animation", false, "Shows a hand swing when placing a crystal.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Placement Arm", SwingHand.RealHand, "The arm used for placement animations.", this.placeSwing::get);
    private final Setting<Boolean> attackSwing = this.sgRender.booleanSetting("Detonation Animation", false, "Shows a hand swing when attacking a crystal.");
    private final Setting<SwingHand> attackHand = this.sgRender.enumSetting("Detonation Arm", SwingHand.RealHand, "The arm used for attack animations.", this.attackSwing::get);
    private final Setting<Boolean> animationColor = this.sgRender.booleanSetting("Color Interpolation", true, "Smoothly transitions block highlight colors based on progress.");
    private final Setting<AnimationMode> animationMode = this.sgRender.enumSetting("Visual Growth Mode", AnimationMode.Full, "The expansion logic for the highlight box.");
    private final Setting<Double> animationExponent = this.sgRender.doubleSetting("Animation Sensitivity", 1.0, 0.0, 10.0, 0.1, "Curvature of the progress animation.");
    private final Setting<RenderShape> renderShape = this.sgRender.enumSetting("Box Style", RenderShape.Full, "The geometry style of the block highlight.");
    private final Setting<BlackOutColor> lineStartColor = this.sgRender.colorSetting("Outline Start Color", new BlackOutColor(255, 0, 0, 0), "Initial color of the box outline.");
    private final BlackOutColor sideStartColorValue = new BlackOutColor(255, 0, 0, 0); // TODO: ???
    private final Setting<BlackOutColor> sideStartColor = this.sgRender.colorSetting("Fill Start Color", sideStartColorValue, "Initial color of the box faces.");
    private final Setting<BlackOutColor> lineEndColor = this.sgRender.colorSetting("Outline End Color", new BlackOutColor(255, 0, 0, 255), "Final color of the box outline at 100% progress.");
    private final Setting<BlackOutColor> sideEndColor = this.sgRender.colorSetting("Fill End Color", new BlackOutColor(255, 0, 0, 50), "Final color of the box faces at 100% progress.");
    private final Setting<RenderShape> instaRenderShape = this.sgRender.enumSetting("Instant Box Style", RenderShape.Full, "Visual style for instant-mined blocks.");
    private final Setting<BlackOutColor> instaLineColor = this.sgRender.colorSetting("Instant Outline Color", new BlackOutColor(255, 0, 0, 255), "Outline color for instant mining.");
    private final Setting<BlackOutColor> instaSideColor = this.sgRender.colorSetting("Instant Fill Color", new BlackOutColor(255, 0, 0, 50), "Fill color for instant mining.");

    private final TimerList<BlockPos> crystals = new TimerList<>(false);
    private final List<PlayerEntity> enemies = new ArrayList<>();
    public BlockPos minePos = null;
    public BlockPos crystalPos = null;
    public MineType mineType = null;
    public boolean started = false;
    private BlockPos prevPos = null;
    private PlayerEntity target = null;
    private double progress = 0.0;
    private int minedFor = 0;
    private double prevProgress = 0.0;
    private double currentProgress = 0.0;
    private BlockState prevState = Blocks.AIR.getDefaultState();
    private boolean startedThisTick = false;
    private long lastPlace = 0L;
    private long lastAttack = 0L;
    private BlockPos prevMined = null;
    private boolean shouldRestart = false;

    public AutoMine() {
        super("Auto Mine", "Automatically mines enemies' surround blocks to abuse them with crystals.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static AutoMine getInstance() {
        return INSTANCE;
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (this.resetOnSwitch.get() && event.packet instanceof UpdateSelectedSlotC2SPacket) {
            this.shouldRestart = true;
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.updateRender(event.tickDelta);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.crystals.update();
            this.enemies.clear();
            BlackOut.mc.world.getPlayers().forEach(player -> {
                if (!(BlackOut.mc.player.distanceTo(player) > 10.0F)) {
                    if (player != BlackOut.mc.player) {
                        if (!Managers.FRIENDS.isFriend(player)) {
                            this.enemies.add(player);
                        }
                    }
                }
            });
            this.startedThisTick = false;
            this.updatePos();
            if (this.minePos != null && this.mineType == MineType.Manual) {
                if (this.manualRangeReset.get() && !SettingUtils.inMineRange(this.minePos)) {
                    this.prevMined = null;
                    this.started = false;
                    this.minePos = null;
                } else {
                    BlockState state = BlackOut.mc.world.getBlockState(this.minePos);
                    if (!(state.getBlock() instanceof AirBlock)) {
                        this.prevState = state;
                    }
                }
            }

            this.updateStartOrAbort();
            this.updateMining();
            this.prevProgress = this.currentProgress;
            this.currentProgress = this.getProgress();
            this.updateAttacking();
        }
    }

    private void updateStartOrAbort() {
        if (this.shouldRestart) {
            if (this.minePos != null) {
                this.abort(this.minePos);
            }

            this.shouldRestart = false;
            this.started = false;
        }

        if (this.minePos == null) {
            if (this.prevPos != null) {
                this.abort(this.prevPos);
            }
        } else {
            if (!this.minePos.equals(this.prevPos)) {
                this.started = false;
            }

            if (!this.started && !this.paused(false)) {
                Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
                if (!SettingUtils.startMineRot() || this.rotateBlock(this.minePos, dir, this.getMineStartRotationVec(dir), RotationType.Mining, "mining")) {
                    this.start(this.minePos, false);
                }
            }
        }

        this.prevPos = this.minePos;
    }

    private boolean paused(boolean placing) {
        return (placing ? this.pauseEatPlacing : this.pauseEat).get() && BlackOut.mc.player.isUsingItem() || this.pauseSword.get() && BlackOut.mc.player.getMainHandStack().getItem() instanceof SwordItem;
    }

    private void updateRender(float tickDelta) {
        double p = this.currentProgress;
        if (this.prevMined != null) {
            Render3DUtils.box(BoxUtils.get(this.prevMined), this.instaSideColor.get(), this.instaLineColor.get(), this.instaRenderShape.get());
        } else if (this.minePos != null && this.started && this.prevProgress < p && p < Double.POSITIVE_INFINITY) {
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

    private Box getBox(double p, AnimationMode mode) {
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

    private Box getBox(double sides, double up, double down) {
        BlockState state = this.ncpState();
        VoxelShape shape = state.getOutlineShape(BlackOut.mc.world, this.minePos);
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

    private double getProgress() {
        if (this.minePos == null) {
            return 0.0;
        } else {
            ItemStack bestStack = this.findBestSlot(
                            stack -> BlockUtils.getBlockBreakingDelta(
                                    stack, this.ncpState(), minePos, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get() && !this.onGroundSpoof.get()
                            )
                    )
                    .stack();
            return !this.ncpProgress.get()
                    ? this.progress
                    : this.minedFor
                    / (
                    1.0
                            / BlockUtils.getBlockBreakingDelta(
                            bestStack, this.ncpState(), minePos, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get() && !this.onGroundSpoof.get()
                    )
            );
        }
    }

    private BlockState ncpState() {
        return this.mineType == MineType.Manual && this.manualRemine.get() && this.fastRemine.get() && !this.shouldInstant()
                ? this.prevState
                : BlackOut.mc.world.getBlockState(this.minePos);
    }

    private void updatePos() {
        if (this.minePos == null || this.mineType != MineType.Manual) {
            Target target = this.getTarget();
            this.minePos = target.pos;
            this.crystalPos = target.crystal;
            this.mineType = target.type;
            this.target = target.target;
        }
    }

    private Target getTarget() {
        Target target = null;
        if (this.autoMine.get()) {
            target = this.targetCheck(target, this.getCev(), this.cevPriority);
            target = this.targetCheck(target, this.getTrapCev(), this.trapCevPriority);
            target = this.targetCheck(target, this.getSurroundCev(), this.surroundCevPriority);
            target = this.targetCheck(target, this.getAutoCity(), this.autoCityPriority);
            target = this.targetCheck(target, this.getAntiBurrow(), this.antiBurrowPriority);
        }

        return target == null ? new Target(null, null, null, 0, null) : target;
    }

    private int getPriority(Target target) {
        return target == null ? 0 : target.priority;
    }

    private Target getCev() {
        BlockPos best = null;
        PlayerEntity bestPlayer = null;
        double bestDist = 1000.0;

        for (PlayerEntity player : this.enemies) {
            BlockPos pos = new BlockPos(player.getBlockX(), (int) Math.ceil(player.getBoundingBox().maxY), player.getBlockZ());
            if (!this.invalidCev(pos, player, this.minCevDamage, this.maxCevDamage, this.cevDamageCheck)) {
                if (pos.equals(this.minePos)) {
                    return new Target(pos, pos.up(), MineType.Cev, this.cevPriority.get().priority, player);
                }

                if (!this.ignored(pos)) {
                    double distance = this.getDist(pos);
                    if (!(distance >= bestDist)) {
                        best = pos;
                        bestDist = distance;
                        bestPlayer = player;
                    }
                }
            }
        }

        return best == null ? null : new Target(best, best.up(), MineType.Cev, this.cevPriority.get().priority, bestPlayer);
    }

    private Target getTrapCev() {
        BlockPos best = null;
        PlayerEntity bestPlayer = null;
        double bestDist = 1000.0;

        for (PlayerEntity player : this.enemies) {
            BlockPos eyePos = new BlockPos(player.getBlockX(), (int) Math.ceil(player.getBoundingBox().maxY) - 1, player.getBlockZ());

            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos pos = eyePos.offset(dir);
                if (!this.invalidCev(pos, player, this.minTrapCevDamage, this.maxTrapCevDamage, this.trapCevDamageCheck)) {
                    if (pos.equals(this.minePos)) {
                        return new Target(pos, pos.up(), MineType.TrapCev, this.trapCevPriority.get().priority, player);
                    }

                    if (!this.ignored(pos)) {
                        double distance = this.getDist(pos);
                        if (!(distance >= bestDist)) {
                            best = pos;
                            bestDist = distance;
                            bestPlayer = player;
                        }
                    }
                }
            }
        }

        return best == null ? null : new Target(best, best.up(), MineType.TrapCev, this.trapCevPriority.get().priority, bestPlayer);
    }

    private Target getSurroundCev() {
        BlockPos best = null;
        PlayerEntity bestPlayer = null;
        double bestDist = 1000.0;

        for (PlayerEntity player : this.enemies) {
            BlockPos feetPos = new BlockPos(player.getBlockX(), (int) Math.round(player.getY()), player.getBlockZ());

            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos pos = feetPos.offset(dir);
                if (!this.invalidCev(pos, player, this.minSurroundCevDamage, this.maxSurroundCevDamage, this.surroundCevDamageCheck)) {
                    if (pos.equals(this.minePos)) {
                        return new Target(pos, pos.up(), MineType.SurroundCev, this.surroundCevPriority.get().priority, player);
                    }

                    if (!this.ignored(pos)) {
                        double distance = this.getDist(pos);
                        if (!(distance >= bestDist)) {
                            best = pos;
                            bestDist = distance;
                            bestPlayer = player;
                        }
                    }
                }
            }
        }

        return best == null
                ? null
                : new Target(best, best.up(), MineType.SurroundCev, this.surroundCevPriority.get().priority, bestPlayer);
    }

    private Target getAutoCity() {
        BlockPos best = null;
        BlockPos bestCrystal = null;
        PlayerEntity bestPlayer = null;
        double bestDist = 1000.0;

        for (PlayerEntity player : this.enemies) {
            BlockPos feetPos = new BlockPos(player.getBlockX(), (int) Math.round(player.getY()), player.getBlockZ());

            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos pos = feetPos.offset(dir);
                BlockPos crystal = pos.offset(dir);
                if (SettingUtils.getPlaceOnDirection(pos) != null
                        && SettingUtils.getPlaceOnDirection(crystal.down()) != null
                        && this.crystalBlock(crystal.down(), false)
                        && SettingUtils.inMineRange(pos)
                        && SettingUtils.inInteractRange(crystal.down())
                        && SettingUtils.inAttackRange(OLEPOSSUtils.getCrystalBox(crystal))) {
                    if (this.isInstant(pos)) {
                        return new Target(pos, crystal, MineType.AutoCity, this.autoCityPriority.get().priority, player);
                    }

                    if (!this.ignored(pos) && BlockUtils.mineable(pos)) {
                        if (this.autoCityDamageCheck.get()) {
                            Vec3d crystalFeet = new Vec3d(crystal.getX() + 0.5, crystal.getY(), crystal.getZ() + 0.5);
                            if (DamageUtils.crystalDamage(player, player.getBoundingBox(), crystalFeet, pos) < this.minAutoCityDamage.get()
                                    || DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), crystalFeet, pos)
                                    > this.maxAutoCityDamage.get()) {
                                continue;
                            }
                        }

                        double distance = this.getDist(pos);
                        if (!(distance >= bestDist)) {
                            best = pos;
                            bestCrystal = crystal;
                            bestDist = distance;
                            bestPlayer = player;
                        }
                    }
                }
            }
        }

        return best == null ? null : new Target(best, bestCrystal, MineType.AutoCity, this.autoCityPriority.get().priority, bestPlayer);
    }

    private Target getAntiBurrow() {
        BlockPos best = null;
        PlayerEntity bestPlayer = null;
        double bestDist = 1000.0;

        for (PlayerEntity player : this.enemies) {
            BlockPos pos = new BlockPos(player.getBlockX(), (int) Math.round(player.getY()), player.getBlockZ());
            if (SettingUtils.getPlaceOnDirection(pos) != null && SettingUtils.inMineRange(pos)) {
                if (this.isInstant(pos)) {
                    return new Target(pos, null, MineType.AntiBurrow, this.antiBurrowPriority.get().priority, player);
                }

                if (!this.ignored(pos) && BlockUtils.mineable(pos)) {
                    double distance = this.getDist(pos);
                    if (!(distance >= bestDist)) {
                        best = pos;
                        bestDist = distance;
                        bestPlayer = player;
                    }
                }
            }
        }

        return best == null ? null : new Target(best, null, MineType.AntiBurrow, this.antiBurrowPriority.get().priority, bestPlayer);
    }

    private boolean invalidCev(BlockPos pos, PlayerEntity player, Setting<Double> minDmg, Setting<Double> maxDmg, Setting<Boolean> dmgCheck) {
        if (!this.crystalBlock(pos, true)) {
            return true;
        } else if (SettingUtils.getPlaceOnDirection(pos) == null) {
            return true;
        } else if (!SettingUtils.inMineRange(pos)
                || !SettingUtils.inInteractRange(pos)
                || !SettingUtils.inAttackRange(OLEPOSSUtils.getCrystalBox(pos.up()))) {
            return true;
        } else if (this.isInstant(pos)) {
            return false;
        } else if (EntityUtils.intersects(
                BoxUtils.crystalSpawnBox(pos.up()),
                entity -> !(entity instanceof EndCrystalEntity) && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ExperienceBottleEntity)
        )) {
            return true;
        } else {
            Vec3d crystalFeet = new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            if (dmgCheck.get()) {
                return DamageUtils.crystalDamage(player, player.getBoundingBox(), crystalFeet, pos) < minDmg.get() || DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), crystalFeet, pos) > maxDmg.get();
            } else {
                return false;
            }
        }
    }

    private boolean ignored(BlockPos pos) {
        return this.ignore.get().contains(BlackOut.mc.world.getBlockState(pos).getBlock());
    }

    private boolean isInstant(BlockPos pos) {
        return pos.equals(this.prevMined);
    }

    private double getDist(BlockPos pos) {
        return BlackOut.mc.player.getEyePos().distanceTo(pos.toCenterPos());
    }

    private boolean crystalBlock(BlockPos pos, boolean cev) {
        Block bottom = this.getBlock(pos);
        if (SettingUtils.oldCrystals() && !(this.getBlock(pos.up()) instanceof AirBlock)) {
            return false;
        } else if (cev && bottom == Blocks.BEDROCK) {
            return false;
        } else if (!(this.getBlock(pos.up()) instanceof AirBlock)) {
            return false;
        } else {
            return this.isInstant(pos) || bottom == Blocks.OBSIDIAN || bottom == Blocks.BEDROCK;
        }
    }

    private Block getBlock(BlockPos pos) {
        return Managers.BLOCK.blockState(pos).getBlock();
    }

    private Target targetCheck(Target target, Target newTarget, Setting<Priority> prioritySetting) {
        int priority = prioritySetting.get().priority;
        return priority >= 0 && newTarget != null && priority >= this.getPriority(target) ? newTarget : target;
    }

    private void updateAttacking() {
        EndCrystalEntity target = this.getTargetCrystal();
        if (target != null) {
            if (this.rotateCheck(target)) {
                if (!(System.currentTimeMillis() - this.lastAttack <= 1000.0 / this.attackSpeed.get())) {
                    if (this.attackSwing.get()) {
                        this.clientSwing(this.attackHand.get(), Hand.MAIN_HAND);
                    }

                    this.attackEntity(target);
                    this.end("attacking");
                    this.lastAttack = System.currentTimeMillis();
                }
            }
        }
    }

    private boolean rotateCheck(EndCrystalEntity target) {
        if (!SettingUtils.shouldRotate(RotationType.Attacking)) {
            return true;
        } else {
            return SettingUtils.shouldIgnoreRotations(target)
                    ? this.checkAttackLimit()
                    : this.attackRotate(target.getBoundingBox(), this.getAttackRotationVec(target), -0.1, "attacking");
        }
    }

    private EndCrystalEntity getTargetCrystal() {
        EndCrystalEntity closestCrystal = null;
        double closestDistance = 69420.0;

        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal
                    && this.crystals.contains(crystal.getBlockPos())
                    && !AutoCrystal.getInstance().shouldAutoMineStop(entity)
                    && SettingUtils.inAttackRange(entity.getBoundingBox())) {
                double distance = BlackOut.mc.player.distanceTo(entity);
                if (!(distance >= closestDistance)) {
                    closestCrystal = crystal;
                    closestDistance = distance;
                }
            }
        }

        return closestCrystal;
    }

    private void updateMining() {
        if (this.minePos != null && !this.startedThisTick) {
            boolean holding = this.itemMinedCheck(Managers.PACKET.getStack());
            int slot = this.findBestSlot(
                            stack -> BlockUtils.getBlockBreakingDelta(
                                    this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get() && !this.onGroundSpoof.get()
                            )
                    )
                    .slot();
            ItemStack bestStack = holding ? Managers.PACKET.getStack() : BlackOut.mc.player.getInventory().getStack(slot);
            if (this.ncpProgress.get()) {
                this.minedFor++;
            } else {
                this.progress = this.progress
                        + BlockUtils.getBlockBreakingDelta(
                        this.minePos, bestStack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get() && !this.onGroundSpoof.get()
                );
            }

            if (this.minedCheck(bestStack) && this.damageSyncCheck()) {
                this.mineEndUpdate(holding, slot);
            } else if (this.almostMined(bestStack) && SettingUtils.endMineRot()) {
                this.preRotate();
            }
        }
    }

    private FindResult findBestSlot(EpicInterface<ItemStack, Double> test) {
        return InvUtils.findBest(this.pickaxeSwitch.get().hotbar, this.pickaxeSwitch.get().inventory && this.allowInventory.get(), test);
    }

    private boolean damageSyncCheck() {
        if (this.damageSync.get() && this.target != null) {
            int amogus = -this.target.timeUntilRegen + 11 + this.syncPredict.get();
            return amogus > 0 && amogus <= this.syncLength.get();
        } else {
            return true;
        }
    }

    private void mineEndUpdate(boolean holding, int slot) {
        EndCrystalEntity crystalAt = this.endCrystalAt(this.crystalPos);
        if (!this.notPressed() && !this.ignored(this.minePos)) {
            switch (this.mineType) {
                case Cev:
                    if (crystalAt == null) {
                        if (EntityUtils.intersects(BoxUtils.crystalSpawnBox(this.crystalPos), entity -> true)) {
                            return;
                        }

                        if (!this.placeCrystal(this.crystalPos.down())) {
                            return;
                        }

                        if (!this.antiAntiCev.get()) {
                            return;
                        }
                    }
                    break;
                case TrapCev:
                    if (crystalAt == null) {
                        if (EntityUtils.intersects(BoxUtils.crystalSpawnBox(this.crystalPos), entity -> true)) {
                            return;
                        }

                        if (!this.placeCrystal(this.crystalPos.down())) {
                            return;
                        }

                        if (!this.antiAntiTrapCev.get()) {
                            return;
                        }
                    }
                    break;
                case SurroundCev:
                    if (crystalAt == null
                            && (
                            !this.acceptCollide.get() || !EntityUtils.intersects(BoxUtils.get(this.crystalPos.down()), entity -> entity instanceof EndCrystalEntity)
                    )) {
                        if (EntityUtils.intersects(BoxUtils.crystalSpawnBox(this.crystalPos), entity -> true)) {
                            return;
                        }

                        if (!this.placeCrystal(this.crystalPos.down())) {
                            return;
                        }

                        if (!this.antiAntiSurroundCev.get()) {
                            return;
                        }
                    }
                    break;
                case AutoCity:
                    if (crystalAt == null && this.placeCrystal.get() && !this.placeCrystal(this.crystalPos.down())) {
                        return;
                    }
            }

            this.endMining(holding, slot);
        }
    }

    private boolean notPressed() {
        return this.useMineBind.get() && !this.mineBind.get().isPressed();
    }

    private void preRotate() {
        if (!this.notPressed()) {
            if (!(this.getBlock(this.minePos) instanceof AirBlock)) {
                if (SettingUtils.inMineRange(this.minePos)) {
                    Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
                    if (dir != null) {
                        this.rotateBlock(this.minePos, dir, this.getMineEndRotationVec(), RotationType.Mining, "mining");
                    }
                }
            }
        }
    }

    private void endMining(boolean holding, int slot) {
        if (!(this.getBlock(this.minePos) instanceof AirBlock)) {
            if (SettingUtils.inMineRange(this.minePos)) {
                Direction dir = SettingUtils.getPlaceOnDirection(this.minePos);
                if (dir != null) {
                    if (!SettingUtils.endMineRot() || this.rotateBlock(this.minePos, dir, this.getMineEndRotationVec(), RotationType.Mining, "mining")) {
                        boolean switched = false;
                        if (holding || (switched = this.pickaxeSwitch.get().swap(slot))) {
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
                            if (this.shouldInstant()) {
                                this.prevMined = this.minePos;
                            } else if (!this.manualRemine.get() || this.mineType != MineType.Manual) {
                                this.prevMined = null;
                                this.started = false;
                                this.minePos = null;
                            } else if (this.fastRemine.get()) {
                                this.start(this.minePos, true);
                            } else {
                                this.started = false;
                            }

                            this.end("mining");
                            if (this.crystalPos != null && this.shouldAttack()) {
                                this.crystals.add(this.crystalPos, this.attackTime.get());
                            }

                            if (switched) {
                                this.pickaxeSwitch.get().swapBack();
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isMining(BlockPos pos) {
        if (!this.started) {
            return false;
        } else {
            return pos.equals(this.minePos) && this.getProgress() <= 1.0;
        }
    }

    private boolean shouldInstant() {
        return switch (this.mineType) {
            case Cev -> this.instantCev.get();
            case TrapCev -> this.instantTrapCev.get();
            case SurroundCev -> this.instantSurroundCev.get();
            case AutoCity -> this.instantAutoCity.get();
            case Manual -> this.manualInstant.get();
            default -> false;
        };
    }

    public void onStart(BlockPos pos) {
        if (this.mineType == MineType.Manual && pos.equals(this.minePos)) {
            if (!this.isMining(pos)) {
                this.started = false;
            }

            this.minePos = null;
        } else {
            if (this.manualMine.get() && this.getBlock(pos) != Blocks.BEDROCK) {
                this.started = false;
                this.minePos = pos;
                this.mineType = MineType.Manual;
                this.crystalPos = null;
            }
        }
    }

    public void onAbort(BlockPos pos) {
    }

    public void onStop(BlockPos pos) {
    }

    private boolean shouldAttack() {
        return switch (this.mineType) {
            case Cev, TrapCev, SurroundCev -> true;
            case AutoCity -> this.attackCrystal.get();
            case Manual, SurroundMiner, AntiBurrow -> false;
        };
    }

    private EndCrystalEntity endCrystalAt(BlockPos pos) {
        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal && entity.getBlockPos().equals(pos)) {
                return crystal;
            }
        }

        return null;
    }

    private boolean placeCrystal(BlockPos pos) {
        if (this.paused(true)) {
            return false;
        } else if (System.currentTimeMillis() - this.lastPlace < 1000.0 / this.placeSpeed.get()) {
            return false;
        } else {
            Direction placeDir = SettingUtils.getPlaceOnDirection(pos);
            if (placeDir == null) {
                return false;
            } else {
                Hand hand = OLEPOSSUtils.getHand(Items.END_CRYSTAL);
                boolean switched = false;
                FindResult result = this.crystalSwitch.get().find(Items.END_CRYSTAL);
                if (hand == null && !result.wasFound()) {
                    return false;
                } else if (SettingUtils.shouldRotate(RotationType.Interact)
                        && !this.rotateBlock(pos, placeDir, this.getPlaceRotationVec(pos), RotationType.Interact, 0.1, "crystal")) {
                    return false;
                } else if (hand == null && !(switched = this.crystalSwitch.get().swap(result.slot()))) {
                    return false;
                } else {
                    if (this.placeSwing.get()) {
                        this.clientSwing(this.placeHand.get(), hand);
                    }

                    this.interactBlock(hand, pos.toCenterPos(), placeDir, pos);
                    this.lastPlace = System.currentTimeMillis();
                    this.end("crystal");
                    if (switched) {
                        this.crystalSwitch.get().swapBack();
                    }

                    return true;
                }
            }
        }
    }

    private void start(BlockPos pos, boolean isRemine) {
        Direction dir = SettingUtils.getPlaceOnDirection(pos);
        if (dir != null) {
            if (this.prevPos != null && !this.prevPos.equals(pos)) {
                this.abort(this.prevPos);
            }

            this.currentProgress = 0.0;
            this.prevProgress = 0.0;
            this.started = true;
            this.startedThisTick = true;
            this.progress = 0.0;
            this.minedFor = 0;
            if (!pos.equals(this.prevMined)) {
                this.prevMined = null;
                if (!isRemine && this.preSwitch.get()) {
                    int slot = this.findBestSlot(
                                    stack -> BlockUtils.getBlockBreakingDelta(
                                            this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get() && !this.onGroundSpoof.get()
                                    )
                            )
                            .slot();
                    this.pickaxeSwitch.get().swap(slot);
                }

                this.sendSequenced(s -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, dir, s));
                SettingUtils.mineSwing(SwingSettings.MiningSwingState.Start);
                this.end("mining");
                if (!isRemine && this.preSwitch.get()) {
                    this.pickaxeSwitch.get().swapBack();
                }

                if (this.mineStartSwing.get()) {
                    this.clientSwing(this.mineHand.get(), Hand.MAIN_HAND);
                }
            }
        }
    }

    private void abort(BlockPos pos) {
        this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.DOWN, 0));
        this.started = false;
    }

    private boolean itemMinedCheck(ItemStack stack) {
        return !this.ncpProgress.get()
                ? this.progress * this.speed.get() >= 1.0
                : this.minedFor * this.speed.get()
                >= Math.ceil(
                1.0
                        / BlockUtils.getBlockBreakingDelta(
                        this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get() || this.onGroundSpoof.get()
                )
        );
    }

    private boolean minedCheck(ItemStack stack) {
        if (this.minePos.equals(this.prevMined)) {
            return true;
        } else if (this.itemMinedCheck(stack)) {
            return true;
        } else {
            if (this.onGroundSpoof.get()) {
                Managers.PACKET.spoofOG(true);
            }

            return false;
        }
    }

    private boolean almostMined(ItemStack stack) {
        if (this.getBlock(this.minePos) instanceof AirBlock) {
            return false;
        } else if (!SettingUtils.inMineRange(this.minePos)) {
            return false;
        } else if (SettingUtils.getPlaceOnDirection(this.minePos) == null) {
            return false;
        } else {
            return !this.ncpProgress.get()
                    ? this.progress >= 0.9
                    : this.minedFor + 2
                    >= Math.ceil(
                    1.0
                            / BlockUtils.getBlockBreakingDelta(
                            this.minePos, stack, this.effectCheck.get(), this.waterCheck.get(), this.onGroundCheck.get() || this.onGroundSpoof.get()
                    )
            );
        }
    }

    public double getCurrentProgress() {
        return this.currentProgress;
    }

    private Vec3d getMineStartRotationVec(Direction dir) {
        return this.minePos.toCenterPos();
    }

    private Vec3d getMineEndRotationVec() {
        switch (this.mineType) {
            case Cev:
            case TrapCev:
                return new Vec3d(this.minePos.getX() + 0.5, this.minePos.getY() + 1, this.minePos.getZ() + 0.5);
            case SurroundCev:
            case SurroundMiner:
                return new Vec3d(this.minePos.getX() + 0.5, this.minePos.getY(), this.minePos.getZ() + 0.5);
            case AutoCity:
                double x = Integer.compare(this.crystalPos.getX(), this.minePos.getX()) * 0.4;
                double z = Integer.compare(this.crystalPos.getZ(), this.minePos.getZ()) * 0.4;
                return new Vec3d(this.minePos.getX() + 0.5 + x, this.minePos.getY() + 0.1, this.minePos.getZ() + 0.5 + z);
            case Manual:
                if (this.crystalBlock(this.minePos, true)) {
                    return new Vec3d(this.minePos.getX() + 0.5, this.minePos.getY() + 1, this.minePos.getZ() + 0.5);
                }

                return this.minePos.toCenterPos();
            default:
                return this.minePos.toCenterPos();
        }
    }

    private Vec3d getPlaceRotationVec(BlockPos pos) {
        switch (this.mineType) {
            case Cev:
            case TrapCev:
                return new Vec3d(this.minePos.getX() + 0.5, this.minePos.getY() + 1, this.minePos.getZ() + 0.5);
            case SurroundCev:
            case SurroundMiner:
                return new Vec3d(this.minePos.getX() + 0.5, this.minePos.getY(), this.minePos.getZ() + 0.5);
            case AutoCity:
                double x = Integer.compare(this.minePos.getX(), pos.getX()) / 3.0;
                double z = Integer.compare(this.minePos.getZ(), pos.getZ()) / 3.0;
                return new Vec3d(pos.getX() + 0.5 + x, pos.getY() + 0.9, pos.getZ() + 0.5 + z);
            case Manual:
            default:
                return new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }
    }

    private Vec3d getAttackRotationVec(EndCrystalEntity entity) {
        return entity.getPos();
    }

    public enum AnimationMode {
        Full,
        Up,
        Down,
        Double,
        None
    }

    public enum MineType {
        Cev(true),
        TrapCev(true),
        SurroundCev(true),
        SurroundMiner(false),
        AutoCity(false),
        AntiBurrow(false),
        Manual(false);

        public final boolean cev;

        MineType(boolean cev) {
            this.cev = cev;
        }
    }

    public enum Priority {
        Highest(6),
        Higher(5),
        High(4),
        Normal(3),
        Low(2),
        Lower(1),
        Lowest(0),
        Disabled(-1);

        public final int priority;

        Priority(int priority) {
            this.priority = priority;
        }
    }

    public enum SwitchState {
        Start,
        End,
        Both
    }

    private record Target(BlockPos pos, BlockPos crystal, MineType type, int priority, PlayerEntity target) {
    }
}
