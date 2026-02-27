package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.Suicide;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.*;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

// TODO: Need Patches
// TODO: задействовать existed/existedTicks/waitExt либо удалить их (сейчас не используются).
// TODO: привести rotate/serverDir/rotationMode к единому флоу, исключить двойные ротации.
// TODO: добавить строгую проверку replaceable + collision при noHitbox=false.
// TODO: добавить защиту от установки в блоках, где beds запрещены (overworld, end).
// TODO: оптимизировать calc() чтобы не сканировать куб целиком каждый тик.
// TODO: синхронизировать damageWait и extrapolation, чтобы избежать stale damage.
// TODO: добавить настройку ограничения спама packets при explodeSpeed=0.
// TODO: пересмотреть renderDamage и анимации (animMoveSpeed/exponent) — сейчас не влияют.
// TODO: добавить fallback при провале rotateBlock (retry/skip with cooldown).
// TODO: учесть AntiBot/Teams при выборе targets.
// TODO: добавить reset render boxes при потере цели/смене мира.
@OnlyDev
public class BedAura extends Module {
    private static BedAura INSTANCE;

    private final SettingGroup sgPlace = this.addGroup("Place");
    private final SettingGroup sgExplode = this.addGroup("Explode");
    private final SettingGroup sgSlow = this.addGroup("Slow");
    private final SettingGroup sgFacePlace = this.addGroup("Face Place");
    private final SettingGroup sgDamage = this.addGroup("Damage");
    private final SettingGroup sgExtrapolation = this.addGroup("Extrapolation");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final SettingGroup sgBoxRender = this.addGroup("Box Render");
    private final SettingGroup sgExplodeRender = this.addGroup("Explode Render");
    private final SettingGroup sgCalculation = this.addGroup("Calculations");

    // TODO: existed, existedTicks, waitExt, animMoveSpeed, animMoveExponent и renderDamage не используются
    private final Setting<Boolean> place = this.sgPlace.booleanSetting("Placement", true, "Enables the automatic placement of bed blocks near targets.");
    private final Setting<Boolean> pauseEatPlace = this.sgPlace.booleanSetting("Pause on Consume", false, "Suspends bed placement while the player is eating or drinking.");
    private final Setting<Double> placeSpeed = this.sgPlace.doubleSetting("Placement Speed", 20.0, 0.0, 20.0, 0.1, "The maximum number of beds to place per second.");
    private final Setting<Boolean> noHitbox = this.sgPlace.booleanSetting("Ignore Entity Hitboxes", true, "Allows placement through entities; optimized for specific server bypasses.");
    private final Setting<Boolean> floor = this.sgPlace.booleanSetting("Floor Requirement", false, "Restricts placement to the top surface of blocks only.");
    private final Setting<Boolean> fireBlocking = this.sgPlace.booleanSetting("Fire Collision Check", false, "Prevents placement if the target area is occupied by fire.");
    private final Setting<Boolean> serverDir = this.sgPlace.booleanSetting("Server-Side Direction", true, "Synchronizes placement direction with server-validated rotations.");
    private final Setting<Boolean> rotate = this.sgPlace.booleanSetting("Rotation Correction", true, "Ensures the player rotates toward the bed during placement.", this.serverDir::get);
    private final Setting<RotationMode> rotationMode = this.sgPlace.enumSetting("Rotation Logic", RotationMode.Instant, "The mathematical approach used to handle player head movements.", () -> !this.serverDir.get() || this.rotate.get());
    private final Setting<Boolean> pauseOffGround = this.sgPlace.booleanSetting("Airborne Safety", true, "Disables placement while jumping or falling to maintain accuracy.");
    private final Setting<SwitchMode> switchMode = this.sgPlace.enumSetting("Switch Mode", SwitchMode.Silent, "The inventory interaction method used to equip beds.");
    private final Setting<Boolean> pauseEatExplode = this.sgExplode.booleanSetting("Pause Detonation on Consume", false, "Suspends bed detonation while the player is eating or drinking.");
    private final Setting<AutoCrystal.DelayMode> existedMode = this.sgExplode.enumSetting("Delay Unit", AutoCrystal.DelayMode.Ticks, "Determines if detonation delays are calculated in seconds or game ticks.");
    private final Setting<Double> existed = this.sgExplode.doubleSetting("Detonation Delay (s)", 0.0, 0.0, 1.0, 0.01, "Seconds to wait after placement before detonating.", () -> this.existedMode.get() == AutoCrystal.DelayMode.Seconds);
    private final Setting<Integer> existedTicks = this.sgExplode.intSetting("Detonation Delay (t)", 0, 0, 20, 1, "Ticks to wait after placement before detonating.", () -> this.existedMode.get() == AutoCrystal.DelayMode.Ticks);
    private final Setting<AutoCrystal.ActionSpeedMode> explodeSpeedMode = this.sgExplode.enumSetting("Explosion Timing", AutoCrystal.ActionSpeedMode.Sync, "Algorithm used to synchronize explosion packets.");
    private final Setting<Double> explodeSpeedLimit = this.sgExplode.doubleSetting("Detonation Rate Limit", 0.0, 0.0, 20.0, 0.1, "Hard cap on detonations per second (0 = Unlimited).", () -> this.explodeSpeedMode.get() == AutoCrystal.ActionSpeedMode.Sync);
    private final Setting<Double> constantExplodeSpeed = this.sgExplode.doubleSetting("Static Explode Speed", 10.0, 0.0, 20.0, 0.1, "Fixed rate for detonations when using Sync mode.", () -> this.explodeSpeedMode.get() == AutoCrystal.ActionSpeedMode.Sync);
    private final Setting<Double> explodeSpeed = this.sgExplode.doubleSetting("Dynamic Explode Speed", 20.0, 0.0, 20.0, 0.1, "Variable detonation rate for standard modes.", () -> this.explodeSpeedMode.get() == AutoCrystal.ActionSpeedMode.Normal);
    private final Setting<Double> rotationHeight = this.sgExplode.doubleSetting("Interact Vector Height", 0.25, 0.0, 0.5, 0.01, "The vertical offset for the explosion interaction point.");
    private final Setting<Double> slowDamage = this.sgSlow.doubleSetting("Throttling Threshold", 3.0, 0.0, 20.0, 0.1, "Reduces speed when target damage falls below this value.");
    private final Setting<Double> slowSpeed = this.sgSlow.doubleSetting("Throttled Speed", 2.0, 0.0, 20.0, 0.1, "The placement speed applied when the Throttling Threshold is active.");
    private final Setting<Double> slowHealth = this.sgSlow.doubleSetting("Health Safety Margin", 10.0, 0.0, 20.0, 0.5, "Only applies throttled speed if the target's health is above this value.");
    private final Setting<KeyBind> holdFacePlace = this.sgFacePlace.keySetting("Face-Place Hotkey", "Forces high-speed placement at the target's head while held.");
    private final Setting<Double> facePlaceHealth = this.sgFacePlace.doubleSetting("Face-Place Health", 0.0, 0.0, 10.0, 0.1, "Automatically prioritizes head-level placement if target health is below this.");
    private final Setting<Double> armorFacePlace = this.sgFacePlace.doubleSetting("Armor Durability Trigger", 10.0, 0.0, 100.0, 1.0, "Face-places if any armor piece falls below this durability percentage.");
    private final Setting<Double> facePlaceDamage = this.sgFacePlace.doubleSetting("Face-Place Min Damage", 0.0, 0.0, 10.0, 0.1, "Overrides minimum damage requirements during face-place operations.");
    private final Setting<Boolean> ignoreSlow = this.sgFacePlace.booleanSetting("Disable Throttling", true, "Ignores 'Slow' settings during face-place operations.");
    private final Setting<Double> minPlace = this.sgDamage.doubleSetting("Min Target Damage", 5.0, 0.0, 20.0, 0.1, "Minimum damage the target must receive for placement to occur.");
    private final Setting<Boolean> checkSelfPlacing = this.sgDamage.booleanSetting("Self-Damage Safety", true, "Calculates potential self-harm before placing a bed.");
    private final Setting<Double> maxSelfPlace = this.sgDamage.doubleSetting("Max Self-Damage", 10.0, 0.0, 20.0, 0.1, "Maximum damage the player can take from their own placement.", this.checkSelfPlacing::get);
    private final Setting<Double> minSelfRatio = this.sgDamage.doubleSetting("Efficiency Ratio", 2.0, 0.0, 20.0, 0.1, "Required damage ratio (Target Damage / Self Damage) for placement.", this.checkSelfPlacing::get);
    private final Setting<Boolean> checkFriendPlacing = this.sgDamage.booleanSetting("Friendly Fire Safety", true, "Prevents placements that would harm friends.");
    private final Setting<Double> maxFriendPlace = this.sgDamage.doubleSetting("Max Friend Damage", 12.0, 0.0, 20.0, 0.1, "Maximum damage allowed to friendly players.", this.checkFriendPlacing::get);
    private final Setting<Double> minFriendRatio = this.sgDamage.doubleSetting("Friend Efficiency Ratio", 1.0, 0.0, 20.0, 0.1, "Required damage ratio (Target / Friend) for placement.", this.checkFriendPlacing::get);
    private final Setting<Boolean> checkEnemyExplode = this.sgDamage.booleanSetting("Detonation Target Check", true, "Validates damage to target before exploding.");
    private final Setting<Double> minExplode = this.sgDamage.doubleSetting("Min Explosion Damage", 5.0, 0.0, 20.0, 0.1, "Required damage to target for detonation.", this.checkEnemyExplode::get);
    private final Setting<Boolean> checkSelfExplode = this.sgDamage.booleanSetting("Self-Explosion Safety", true, "Prevents suicidal detonations.");
    private final Setting<Double> maxSelfExplode = this.sgDamage.doubleSetting("Max Explosion Self-Damage", 10.0, 0.0, 20.0, 0.1, "Maximum self-damage allowed per detonation.", this.checkSelfExplode::get);
    private final Setting<Double> minSelfExplodeRatio = this.sgDamage.doubleSetting("Explosion Efficiency Ratio", 2.0, 0.0, 20.0, 0.1, "Required damage ratio (Target / Self) for detonation.", this.checkSelfExplode::get);
    private final Setting<Boolean> checkFriendExplode = this.sgDamage.booleanSetting("Friend Detonation Safety", true, "Prevents detonations that harm friends.");
    private final Setting<Double> maxFriendExplode = this.sgDamage.doubleSetting("Max Explosion Friend Damage", 12.0, 0.0, 20.0, 0.1, "Maximum allowed damage to friends from detonation.", this.checkFriendExplode::get);
    private final Setting<Double> minFriendExplodeRatio = this.sgDamage.doubleSetting("Friend Detonation Ratio", 1.0, 0.0, 20.0, 0.1, "Required damage ratio (Target / Friend) for detonation.", this.checkFriendExplode::get);
    private final Setting<Double> forcePop = this.sgDamage.doubleSetting("Lethal Threshold", 0.0, 0.0, 5.0, 0.25, "Ignores damage constraints if the hit is guaranteed to pop a Totem.");
    private final Setting<Double> selfPop = this.sgDamage.doubleSetting("Totem Preservation", 1.0, 0.0, 5.0, 0.25, "Restricts damage if it risks popping the player's own Totem.");
    private final Setting<Double> friendPop = this.sgDamage.doubleSetting("Friend Totem Preservation", 0.0, 0.0, 5.0, 0.25, "Restricts damage if it risks popping a friend's Totem.");
    private final Setting<Integer> extrapolation = this.sgExtrapolation.intSetting("Target Prediction", 0, 0, 20, 1, "Predicts target movement in ticks for accurate damage calculation.");
    private final Setting<Integer> selfExt = this.sgExtrapolation.intSetting("Self Prediction", 0, 0, 20, 1, "Predicts player movement in ticks for self-damage calculation.");
    private final Setting<Integer> hitboxExt = this.sgExtrapolation.intSetting("Hitbox Prediction", 0, 0, 20, 1, "Predicts entity hitbox positions to prevent placement obstruction.");
    private final Setting<Boolean> damageWait = this.sgExtrapolation.booleanSetting("Latent Calculation", false, "Wait for client-server sync before final damage check.");
    private final Setting<Integer> waitExt = this.sgExtrapolation.intSetting("Latent Multiplier", 0, 0, 20, 1, "Extra ticks to predict when Latent Calculation is active.", this.damageWait::get);
    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Placement Animation", false, "Visualizes the arm swing during bed placement.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Placement Arm", SwingHand.RealHand, "The arm used for placement animations.");
    private final Setting<Boolean> explodeSwing = this.sgRender.booleanSetting("Detonation Animation", false, "Visualizes the arm swing during bed detonation.");
    private final Setting<SwingHand> explodeHand = this.sgRender.enumSetting("Detonation Arm", SwingHand.RealHand, "The arm used for detonation animations.");
    private final Setting<Boolean> renderBox = this.sgBoxRender.booleanSetting("Placement Highlight", true, "Renders a highlight box at the placement location.");
    private final Setting<Double> renderTime = this.sgBoxRender.doubleSetting("Highlight Duration", 0.3, 0.0, 10.0, 0.1, "Duration the box stays at full opacity.", this.renderBox::get);
    private final Setting<Double> fadeTime = this.sgBoxRender.doubleSetting("Highlight Fade", 1.0, 0.0, 10.0, 0.1, "Duration of the opacity decay animation.", this.renderBox::get);
    private final Setting<Double> animMoveSpeed = this.sgBoxRender.doubleSetting("Animation Fluidity", 2.0, 0.0, 10.0, 0.1, "The speed of the box's motion between targets.", this.renderBox::get);
    private final Setting<Double> animMoveExponent = this.sgBoxRender.doubleSetting("Animation Acceleration", 3.0, 0.0, 10.0, 0.1, "Scales box velocity based on distance to target.", this.renderBox::get);
    private final Setting<RenderShape> renderShape = this.sgBoxRender.enumSetting("Highlight Geometry", RenderShape.Full, "The visual structure of the placement box.", this.renderBox::get);
    private final Setting<BlackOutColor> lineColor = this.sgBoxRender.colorSetting("Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the box edges.", this.renderBox::get);
    private final Setting<BlackOutColor> sideColor = this.sgBoxRender.colorSetting("Fill Color", new BlackOutColor(255, 0, 0, 50), "The color of the box faces.", this.renderBox::get);
    private final Setting<Boolean> separateBox = this.sgBoxRender.booleanSetting("Dual Highlight", true, "Renders the head and foot of the bed as separate boxes.", this.renderBox::get);
    private final Setting<RenderShape> renderHeadShape = this.sgBoxRender.enumSetting("Head Geometry", RenderShape.Full, "Geometry for the bed's head section.", () -> this.renderBox.get() && this.separateBox.get());
    private final Setting<BlackOutColor> headLineColor = this.sgBoxRender.colorSetting("Head Outline Color", new BlackOutColor(255, 255, 255, 255), "Outline color for the bed's head section.", () -> this.renderBox.get() && this.separateBox.get());
    private final Setting<BlackOutColor> headSideColor = this.sgBoxRender.colorSetting("Head Fill Color", new BlackOutColor(255, 255, 255, 50), "Fill color for the bed's head section.", () -> this.renderBox.get() && this.separateBox.get());
    private final Setting<Boolean> renderDamage = this.sgBoxRender.booleanSetting("Display Damage", true, "Renders floating damage values at the placement site.", this.renderBox::get);
    private final Setting<Boolean> renderExplode = this.sgExplodeRender.booleanSetting("Explosion Highlight", true, "Renders a highlight box during detonation.");
    private final Setting<Double> explodeRenderTime = this.sgExplodeRender.doubleSetting("Explosion Duration", 0.3, 0.0, 10.0, 0.1, "Duration of the explosion highlight.", this.renderExplode::get);
    private final Setting<Double> explodeFadeTime = this.sgExplodeRender.doubleSetting("Explosion Fade", 1.0, 0.0, 10.0, 0.1, "Duration of the explosion highlight decay.", this.renderExplode::get);
    private final Setting<RenderShape> explodeRenderShape = this.sgExplodeRender.enumSetting("Explosion Geometry", RenderShape.Full, "Visual structure of the explosion box.", this.renderExplode::get);
    private final Setting<BlackOutColor> explodeLineColor = this.sgExplodeRender.colorSetting("Explosion Outline Color", new BlackOutColor(255, 0, 0, 255), "Outline color for the explosion highlight.", this.renderExplode::get);
    private final Setting<BlackOutColor> explodeSideColor = this.sgExplodeRender.colorSetting("Explosion Fill Color", new BlackOutColor(255, 0, 0, 50), "Fill color for the explosion highlight.", this.renderExplode::get);
    private final Setting<Boolean> separateExplode = this.sgExplodeRender.booleanSetting("Explosion Dual Highlight", true, "Separates head and foot highlights during explosion.", this.renderExplode::get);
    private final Setting<RenderShape> explodeHeadShape = this.sgExplodeRender.enumSetting("Explode Head Geometry", RenderShape.Full, "Geometry for the head during detonation.", () -> this.renderExplode.get() && this.separateExplode.get());
    private final Setting<BlackOutColor> explodeHeadLineColor = this.sgExplodeRender.colorSetting("Explode Head Outline Color", new BlackOutColor(255, 255, 255, 255), "Outline color for the head during detonation.", () -> this.renderExplode.get() && this.separateExplode.get());
    private final Setting<BlackOutColor> explodeHeadSideColor = this.sgExplodeRender.colorSetting("Explode Head Fill Color", new BlackOutColor(255, 255, 255, 50), "Fill color for the head during detonation.", () -> this.renderExplode.get() && this.separateExplode.get());
    private final Setting<Double> damageValue = this.sgCalculation.doubleSetting("Damage Weight", 1.0, -2.0, 2.0, 0.05, "Importance of target damage in target selection.");
    private final Setting<Double> selfDmgValue = this.sgCalculation.doubleSetting("Self-Damage Weight", -1.0, -2.0, 2.0, 0.05, "Importance of self-preservation in target selection.");
    private final Setting<Double> friendDmgValue = this.sgCalculation.doubleSetting("Friend Damage Weight", 0.0, -2.0, 2.0, 0.05, "Importance of friend safety in target selection.");
    private final Setting<Double> rotationValue = this.sgCalculation.doubleSetting("Rotation Weight", 3.0, -5.0, 10.0, 0.1, "Penalty applied based on the rotation angle required.");
    private final Setting<Integer> maxTargets = this.sgCalculation.intSetting("Target Limit", 3, 1, 10, 1, "The maximum number of simultaneous targets to evaluate.");
    private final Setting<Double> enemyDistance = this.sgCalculation.doubleSetting("Scan Range", 10.0, 0.0, 100.0, 1.0, "The maximum distance to search for targets.");

    public static AbstractClientPlayerEntity targetedPlayer = null;
    private final ExtrapolationMap extMap = new ExtrapolationMap();
    private final ExtrapolationMap hitboxMap = new ExtrapolationMap();
    private final List<PlayerEntity> targets = new ArrayList<>();
    private final RenderList<Pair<BlockPos, Direction>> renderList = RenderList.getList(false);
    private final TimerList<BlockPos> explodeTimers = new TimerList<>(true);
    private final TimerList<BlockPos> ignoreState = new TimerList<>(true);
    private final Map<BlockPos, Double[]> damageCache = new HashMap<>();
    private BlockPos placePos = null;
    private Direction placeDir = null;
    private BlockPos explodePos = null;
    private double selfHealth = 0.0;
    private double enemyHealth = 0.0;
    private double friendHealth = 0.0;
    private double selfDamage = 0.0;
    private double enemyDamage = 0.0;
    private double friendDamage = 0.0;
    private LivingEntity target = null;
    private BlockPos targetCalcBest = null;
    private double targetCalcValue = 0.0;
    private int targetCalcR = 0;
    private int targetProgress = 0;
    private BlockPos calcBest = null;
    private Direction calcDir = null;
    private double calcValue = 0.0;
    private int calcR = 0;
    private BlockPos calcMiddle = null;
    private int progress = 0;
    private long lastExplode = 0L;
    private long lastPlace = 0L;
    private boolean suicide = false;
    private boolean facePlacing = false;
    private BlockPos renderPos = null;
    private Direction renderDir = null;
    private double renderProgress = 0.0;

    public BedAura() {
        super("Bed Aura", "Places and blows up beds.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static BedAura getInstance() {
        return INSTANCE;
    }

    @Event
    public void onState(BlockStateEvent event) {
        if (event.state.getBlock() instanceof BedBlock) {
            this.explodeTimers.remove(timer -> timer.value.equals(event.pos));
        } else if (!this.validBlock(event.state)) {
            return;
        }

        if (this.ignoreState.contains(event.pos)) {
            event.cancel();
        }
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.calc(1.0F);
            this.updatePos();
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.updateFacePlacing();
            this.calc(event.tickDelta);
            targetedPlayer = null;
            if (this.explodePos != null && !this.pausedExploding()) {
                this.updateExplode();
            }

            this.updateRender();
            if (this.placePos != null && this.place.get() && !this.pausedPlacing()) {
                this.calcDamage(this.placePos.offset(this.placeDir));
                if (this.target instanceof AbstractClientPlayerEntity player) {
                    targetedPlayer = player;
                }

                this.renderPos = this.placePos;
                this.renderDir = this.placeDir;
                this.renderProgress = 0.0;
                this.updatePlace();
            } else {
                this.renderProgress = Math.min(this.renderProgress + event.frameTime, this.renderTime.get() + this.fadeTime.get());
            }
        }
    }

    private boolean validBlock(BlockState state) {
        return OLEPOSSUtils.replaceable(state);
    }

    private void updateRender() {
        if (this.renderBox.get() && this.renderPos != null && this.renderProgress < this.renderTime.get() + this.fadeTime.get()) {
            this.render(
                    this.renderPos,
                    this.renderDir,
                    this.separateBox,
                    this.headLineColor,
                    this.headSideColor,
                    this.renderHeadShape,
                    this.lineColor,
                    this.sideColor,
                    this.renderShape,
                    this.getAlpha(this.renderProgress, this.renderTime, this.fadeTime)
            );
        }

        if (this.renderExplode.get()) {
            this.renderList
                    .update(
                            (pair, time, delta) -> this.render(
                                    pair.getLeft(),
                                    pair.getRight(),
                                    this.separateExplode,
                                    this.explodeHeadLineColor,
                                    this.explodeHeadSideColor,
                                    this.explodeHeadShape,
                                    this.explodeLineColor,
                                    this.explodeSideColor,
                                    this.explodeRenderShape,
                                    this.getAlpha(time, this.explodeRenderTime, this.explodeFadeTime)
                            )
                    );
        }
    }

    private double getAlpha(double time, Setting<Double> rt, Setting<Double> ft) {
        return 1.0 - Math.max(time - rt.get(), 0.0) / ft.get();
    }

    private void render(
            BlockPos feetPos,
            Direction dir,
            Setting<Boolean> separate,
            Setting<BlackOutColor> headLines,
            Setting<BlackOutColor> headSides,
            Setting<RenderShape> headShape,
            Setting<BlackOutColor> feetLines,
            Setting<BlackOutColor> feetSides,
            Setting<RenderShape> feetShape,
            double alpha
    ) {
        if (separate.get()) {
            Render3DUtils.box(this.getBoxAt(feetPos), feetSides.get().alphaMulti(alpha), feetLines.get().alphaMulti(alpha), feetShape.get());
            Render3DUtils.box(this.getBoxAt(feetPos.offset(dir)), headSides.get().alphaMulti(alpha), headLines.get().alphaMulti(alpha), headShape.get());
        } else {
            Box box = this.getDirectionBox(feetPos, dir);
            Render3DUtils.box(box, headSides.get().alphaMulti(alpha), headLines.get().alphaMulti(alpha), headShape.get());
        }
    }

    private Box getBoxAt(BlockPos pos) {
        return new Box(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.55, pos.getZ() + 1
        );
    }

    private Box getDirectionBox(BlockPos pos, Direction dir) {
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();
        double maxX = pos.getX() + 1;
        double maxY = pos.getY() + 0.55;
        double maxZ = pos.getZ() + 1;
        switch (dir) {
            case NORTH:
                minZ--;
                break;
            case SOUTH:
                maxZ++;
                break;
            case WEST:
                minX--;
                break;
            case EAST:
                maxX++;
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean pausedExploding() {
        return this.pauseEatExplode.get() && BlackOut.mc.player.isUsingItem();
    }

    private boolean pausedPlacing() {
        return this.pauseEatPlace.get() && BlackOut.mc.player.isUsingItem();
    }

    private void updatePlace() {
        if (OLEPOSSUtils.replaceable(this.placePos)) {
            if (this.placeDelayCheck()) {
                this.place();
            }
        }
    }

    private boolean placeDelayCheck() {
        return System.currentTimeMillis() - this.lastPlace >= 1000.0 / (this.shouldSlow() ? this.slowSpeed.get() : this.placeSpeed.get());
    }

    private void updateFacePlacing() {
        this.facePlacing = this.holdFacePlace.get().isPressed();
    }

    private boolean shouldFacePlace() {
        if (this.facePlacing) {
            return true;
        } else if (this.enemyHealth <= this.facePlaceHealth.get()) {
            return true;
        } else if (this.target == null) {
            return false;
        } else {
            for (ItemStack stack : this.target.getArmorItems()) {
                if (stack.isDamageable() && 1.0 - (double) stack.getDamage() / stack.getMaxDamage() <= this.armorFacePlace.get() / 100.0) {
                    return true;
                }
            }

            return false;
        }
    }

    private boolean shouldSlow() {
        this.calcDamage(this.placePos.offset(this.placeDir));
        if (this.ignoreSlow.get() && this.shouldFacePlace()) {
            return false;
        } else {
            return !(this.enemyHealth < this.slowHealth.get()) && this.enemyDamage <= this.slowDamage.get();
        }
    }

    private void place() {
        PlaceData data = SettingUtils.getPlaceData(
                this.placePos,
                null,
                (p, d) -> (!this.floor.get() || d == Direction.DOWN) && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof BedBlock)
        );
        if (data.valid()) {
            Hand hand = OLEPOSSUtils.getHand(OLEPOSSUtils::isBed);
            FindResult result = this.switchMode.get().find(OLEPOSSUtils::isBed);
            if (hand != null || result.wasFound()) {
                if (!SettingUtils.shouldRotate(RotationType.BlockPlace)
                        || this.rotateBlock(data, data.pos().toCenterPos().offset(data.dir(), 0.5), RotationType.BlockPlace, "placing")) {
                    if (!this.pauseOffGround.get() || BlackOut.mc.player.isOnGround()) {
                        switch (this.rotationMode.get()) {
                            case Instant:
                                this.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.placeDir.asRotation(), Managers.ROTATION.nextPitch, Managers.PACKET.isOnGround()));
                                break;
                            case Manager:
                                if (!this.rotateYaw(this.placeDir.asRotation(), RotationType.Other, "placing")) {
                                    return;
                                }
                        }

                        boolean switched = false;
                        if (hand != null || (switched = this.switchMode.get().swap(result.slot()))) {
                            this.placeBlock(hand, data.pos().toCenterPos(), data.dir(), data.pos());
                            ItemStack stack = hand == null ? result.stack() : Managers.PACKET.handStack(hand);
                            if (stack.getItem() instanceof BedItem bedItem) {
                                BlockState feetState = bedItem.getBlock()
                                        .getDefaultState()
                                        .with(BedBlock.PART, BedPart.FOOT)
                                        .with(HorizontalFacingBlock.FACING, this.placeDir);
                                BlockState headState = bedItem.getBlock()
                                        .getDefaultState()
                                        .with(BedBlock.PART, BedPart.HEAD)
                                        .with(HorizontalFacingBlock.FACING, this.placeDir);
                                BlackOut.mc.world.setBlockState(this.placePos, feetState);
                                BlackOut.mc.world.setBlockState(this.placePos.offset(this.placeDir), headState);
                                this.ignoreState.add(this.placePos, 0.3);
                                this.ignoreState.add(this.placePos.offset(this.placeDir), 0.3);
                            }

                            this.lastPlace = System.currentTimeMillis();
                            if (this.placeSwing.get()) {
                                this.clientSwing(this.placeHand.get(), hand);
                            }

                            this.end("placing");
                            if (switched) {
                                this.switchMode.get().swapBack();
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateExplode() {
        BlockState state = BlackOut.mc.world.getBlockState(this.explodePos);
        if (state.getBlock() instanceof BedBlock) {
            switch (this.explodeSpeedMode.get()) {
                case Sync:
                    if (this.explodeSpeedLimit.get() > 0.0 && System.currentTimeMillis() - this.lastExplode <= 1000.0 / this.explodeSpeedLimit.get()) {
                        return;
                    }

                    if (this.explodeTimers.contains(this.explodePos)) {
                        return;
                    }
                    break;
                case Normal:
                    if (System.currentTimeMillis() - this.lastExplode <= 1000.0 / this.explodeSpeed.get()) {
                        return;
                    }
            }

            this.explode(this.explodePos);
        }
    }

    private void explode(BlockPos pos) {
        Direction dir = SettingUtils.getPlaceOnDirection(pos);
        if (dir != null) {
            Vec3d placeVec = new Vec3d(pos.getX() + 0.5, pos.getY() + this.rotationHeight.get(), pos.getZ() + 0.5);
            this.end("placing");
            if (!SettingUtils.shouldRotate(RotationType.Interact) || this.rotateBlock(pos, dir, placeVec, RotationType.Interact, 0.1, "explode")) {
                BlockState state = BlackOut.mc.world.getBlockState(pos);
                Direction direction = state.get(HorizontalFacingBlock.FACING);
                BlockPos headPos;
                BlockPos feetPos;
                if (state.get(BedBlock.PART) == BedPart.HEAD) {
                    headPos = pos;
                    feetPos = pos.offset(direction.getOpposite());
                } else {
                    feetPos = pos;
                    headPos = pos.offset(direction);
                }

                this.renderList.add(new Pair<>(feetPos, direction), this.explodeRenderTime.get() + this.explodeFadeTime.get());
                BlackOut.mc.world.setBlockState(feetPos, Blocks.AIR.getDefaultState());
                BlackOut.mc.world.setBlockState(headPos, Blocks.AIR.getDefaultState());
                this.ignoreState.add(feetPos, 0.3);
                this.ignoreState.add(headPos, 0.3);
                this.interactBlock(Hand.MAIN_HAND, placeVec, dir, pos);
                this.explodeTimers.add(pos, 1.0 / this.constantExplodeSpeed.get());
                this.lastExplode = System.currentTimeMillis();
                this.explodePos = null;
                if (this.explodeSwing.get()) {
                    this.clientSwing(this.explodeHand.get(), Hand.MAIN_HAND);
                }

                this.end("explode");
            }
        }
    }

    private void calc(float tickDelta) {
        if (this.calcMiddle != null) {
            int d = this.calcR * 2 + 1;
            int target = d * d * d;

            for (int i = this.progress; i < target * tickDelta; i++) {
                this.progress = i;
                int x = i % d - this.calcR;
                int y = i / d % d - this.calcR;
                int z = i / d / d % d - this.calcR;
                BlockPos pos = this.calcMiddle.add(x, y, z);
                this.calcPos(pos);
            }

            d = this.targetCalcR * 2 + 1;
            target = d * d * d;

            for (int i = this.targetProgress; i < target * tickDelta; i++) {
                this.targetProgress = i;
                int x = i % d - this.targetCalcR;
                int y = i / d % d - this.targetCalcR;
                int z = i / d / d % d - this.targetCalcR;
                BlockPos pos = this.calcMiddle.add(x, y, z);
                this.calcTarget(pos);
            }
        }
    }

    private void calcTarget(BlockPos pos) {
        BlockState state = BlackOut.mc.world.getBlockState(pos);
        if (state.getBlock() instanceof BedBlock) {
            if (SettingUtils.getPlaceOnDirection(pos) != null) {
                if (SettingUtils.inPlaceRange(pos)) {
                    this.calcDamage(
                            state.get(BedBlock.PART) == BedPart.HEAD
                                    ? pos
                                    : pos.offset(state.get(HorizontalFacingBlock.FACING))
                    );
                    if (this.explodeDamageCheck()) {
                        double value = this.getValue(pos, false);
                        if (!(value <= this.targetCalcValue)) {
                            this.targetCalcBest = pos;
                            this.targetCalcValue = value;
                        }
                    }
                }
            }
        }
    }

    private boolean explodeDamageCheck() {
        if (this.selfDamage * this.selfPop.get() > this.selfHealth) {
            return false;
        } else if (this.friendDamage * this.friendPop.get() > this.friendHealth) {
            return false;
        } else if (this.enemyDamage * this.forcePop.get() > this.enemyHealth) {
            return true;
        } else if (this.checkEnemyExplode.get() && this.enemyDamage < (this.shouldFacePlace() ? this.facePlaceDamage.get() : this.minExplode.get())) {
            return false;
        } else {
            if (this.checkSelfExplode.get()) {
                if (this.selfDamage > this.maxSelfExplode.get()) {
                    return false;
                }

                if (this.checkEnemyExplode.get() && this.enemyDamage / this.selfDamage < this.minSelfExplodeRatio.get()) {
                    return false;
                }
            }

            if (this.checkFriendExplode.get()) {
                return !(this.friendDamage > this.maxFriendExplode.get()) && !(this.enemyDamage / this.friendDamage < this.minFriendExplodeRatio.get());
            } else {
                return true;
            }
        }
    }

    private boolean placeDamageCheck() {
        if (this.selfDamage * this.selfPop.get() > this.selfHealth) {
            return false;
        } else if (this.friendDamage * this.friendPop.get() > this.friendHealth) {
            return false;
        } else if (this.enemyDamage * this.forcePop.get() > this.enemyHealth) {
            return true;
        } else if (this.enemyDamage < (this.shouldFacePlace() ? this.facePlaceDamage.get() : this.minPlace.get())) {
            return false;
        } else {
            if (this.checkSelfPlacing.get()) {
                if (this.selfDamage > this.maxSelfPlace.get()) {
                    return false;
                }

                if (this.enemyDamage / this.selfDamage < this.minSelfRatio.get()) {
                    return false;
                }
            }

            if (this.checkFriendPlacing.get()) {
                return !(this.friendDamage > this.maxFriendPlace.get()) && !(this.enemyDamage / this.friendDamage < this.minFriendRatio.get());
            } else {
                return true;
            }
        }
    }

    private void calcPos(BlockPos pos) {
        if (this.validBlock(pos)) {
            if (!this.floor.get() || this.validFloor(pos.down())) {
                if (this.inRangeToEnemies(pos)) {
                    boolean midInRange = SettingUtils.inInteractRange(pos) && SettingUtils.getPlaceOnDirection(pos) != null;
                    this.calcDamage(pos);
                    if (this.placeDamageCheck()) {
                        double value = this.getValue(pos, true);
                        if (!(value <= this.calcValue)) {
                            for (Direction dir : Direction.Type.HORIZONTAL) {
                                BlockPos pos2 = pos.offset(dir);
                                if ((!this.serverDir.get() || !(Math.abs(RotationUtils.yawAngle(RotationUtils.getYaw(pos2), dir.getOpposite().asRotation())) > 45.0))
                                        && this.validBlock(pos2)
                                        && (!this.floor.get() || this.validFloor(pos2.down()))) {
                                    PlaceData data = SettingUtils.getPlaceData(
                                            pos2,
                                            null,
                                            (p, d) -> (!this.floor.get() || d == Direction.DOWN) && !(BlackOut.mc.world.getBlockState(p).getBlock() instanceof BedBlock)
                                    );
                                    if (data.valid()
                                            && (midInRange || SettingUtils.getPlaceOnDirection(pos2) != null)
                                            && (midInRange || SettingUtils.inInteractRange(pos2))
                                            && SettingUtils.inPlaceRange(data.pos())) {
                                        if (!this.noHitbox.get()
                                                && EntityUtils.intersects(BoxUtils.get(pos2), entity -> !(entity instanceof ItemEntity), this.hitboxMap.getMap())) {
                                            return;
                                        }

                                        this.calcBest = pos;
                                        this.calcValue = value;
                                        this.calcDir = dir;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean validFloor(BlockPos pos) {
        return !(BlackOut.mc.world.getBlockState(pos).getBlock() instanceof BedBlock) && OLEPOSSUtils.solid2(pos);
    }

    private boolean validBlock(BlockPos pos) {
        Block block = BlackOut.mc.world.getBlockState(pos).getBlock();
        return (this.validBlock(BlackOut.mc.world.getBlockState(pos)) || block instanceof BedBlock) && (!this.fireBlocking.get() || !(block instanceof FireBlock));
    }

    private void updatePos() {
        Suicide suicideModule = Suicide.getInstance();
        this.suicide = suicideModule.enabled && suicideModule.useBA.get();
        this.findTargets();
        this.extMap.update(player -> player == BlackOut.mc.player ? this.selfExt.get() : this.extrapolation.get());
        this.hitboxMap.update(player -> player == BlackOut.mc.player ? 0 : this.hitboxExt.get());
        this.placePos = this.calcBest == null ? null : this.calcBest.offset(this.calcDir);
        this.placeDir = this.calcDir == null ? null : this.calcDir.getOpposite();
        this.explodePos = this.targetCalcBest;
        this.startCalc();
    }

    private void startCalc() {
        this.selfHealth = this.getHealth(BlackOut.mc.player);
        this.calcBest = null;
        this.calcValue = -42069.0;
        this.progress = 0;
        this.calcR = (int) Math.ceil(SettingUtils.maxPlaceRange());
        this.calcMiddle = BlockPos.ofFloored(BlackOut.mc.player.getEyePos());
        this.targetCalcBest = null;
        this.targetCalcValue = -42069.0;
        this.targetCalcR = (int) Math.ceil(SettingUtils.maxInteractRange());
        this.targetProgress = 0;
        this.damageCache.clear();
    }

    private double getValue(BlockPos pos, boolean place) {
        double value = 0.0;
        if (place && SettingUtils.shouldRotate(RotationType.BlockPlace) || !place && SettingUtils.shouldRotate(RotationType.Interact)) {
            value += this.rotationMod(pos.toCenterPos());
        }

        value += this.enemyMod();
        value += this.selfMod();
        return value + this.friendMod();
    }

    private double enemyMod() {
        return this.enemyDamage * this.damageValue.get();
    }

    private double selfMod() {
        return this.selfDamage * this.selfDmgValue.get();
    }

    private double friendMod() {
        return this.friendDamage * this.friendDmgValue.get();
    }

    private double rotationMod(Vec3d pos) {
        double yawStep = 45.0;
        double pitchStep = 22.0;
        int yawSteps = (int) Math.ceil(Math.abs(RotationUtils.yawAngle(Managers.ROTATION.prevYaw, RotationUtils.getYaw(pos)) / yawStep));
        int pitchSteps = (int) Math.ceil(Math.abs(RotationUtils.pitchAngle(Managers.ROTATION.prevPitch, RotationUtils.getPitch(pos)) / pitchStep));
        int steps = Math.max(yawSteps, pitchSteps);
        return (3 - Math.min(steps, 3)) * this.rotationValue.get();
    }

    private boolean inRangeToEnemies(BlockPos pos) {
        Vec3d vec = pos.toCenterPos();
        if (this.suicide) {
            return BoxUtils.middle(BlackOut.mc.player.getBoundingBox()).distanceTo(vec) < 3.0;
        } else {
            for (PlayerEntity player : this.targets) {
                if (BoxUtils.middle(player.getBoundingBox()).distanceTo(vec) < 3.0) {
                    return true;
                }
            }

            return false;
        }
    }

    private void findTargets() {
        Map<PlayerEntity, Double> map = new HashMap<>();

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && !(player.getHealth() <= 0.0F)) {
                double distance = BlackOut.mc.player.distanceTo(player);
                if (!(distance > this.enemyDistance.get())) {
                    if (map.size() < this.maxTargets.get()) {
                        map.put(player, distance);
                    } else {
                        for (Entry<PlayerEntity, Double> entry : map.entrySet()) {
                            if (entry.getValue() > distance) {
                                map.remove(entry.getKey());
                                map.put(player, distance);
                                break;
                            }
                        }
                    }
                }
            }
        }

        this.targets.clear();
        map.forEach((playerx, d) -> this.targets.add(playerx));
    }

    private void calcDamage(BlockPos pos) {
        Vec3d vec = pos.toCenterPos();
        if (this.damageCache.containsKey(pos)) {
            Double[] array = this.damageCache.get(pos);
            this.selfDamage = array[0];
            this.enemyDamage = array[1];
            this.friendDamage = array[2];
            this.enemyHealth = array[3];
            this.friendHealth = array[4];
        } else {
            this.selfDamage = DamageUtils.anchorDamage(BlackOut.mc.player, this.extMap.get(BlackOut.mc.player), vec, pos);
            this.enemyDamage = 0.0;
            this.friendDamage = 0.0;
            if (this.suicide) {
                this.enemyDamage = this.selfDamage;
                this.selfDamage = 0.0;
                this.friendDamage = 0.0;
                this.enemyHealth = 20.0;
                this.friendHealth = 36.0;
                this.cache(pos);
            } else {
                this.enemyHealth = 20.0;
                this.friendHealth = 20.0;
                this.targets.forEach(player -> {
                    Box box = this.extMap.get(player);
                    if (!(player.getHealth() <= 0.0F) && player != BlackOut.mc.player) {
                        double dmg = DamageUtils.anchorDamage(player, box, vec, pos);
                        double health = this.getHealth(player);
                        if (Managers.FRIENDS.isFriend(player)) {
                            if (dmg > this.friendDamage) {
                                this.friendDamage = dmg;
                                this.friendHealth = health;
                            }
                        } else if (dmg > this.enemyDamage) {
                            this.enemyDamage = dmg;
                            this.enemyHealth = health;
                            this.target = player;
                        }
                    }
                });
                this.cache(pos);
            }
        }
    }

    private void cache(BlockPos pos) {
        this.damageCache.put(pos, new Double[]{this.selfDamage, this.enemyDamage, this.friendDamage, this.enemyHealth, this.friendHealth});
    }

    private double getHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    public enum RotationMode {
        Instant,
        Manager
    }
}
