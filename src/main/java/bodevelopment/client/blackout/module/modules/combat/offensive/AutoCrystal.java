package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.*;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.*;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalEntity;
import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.Suicide;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.randomstuff.ExtrapolationMap;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.Rotation;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

// TODO: NEED PATCHES
// TODO: оптимизировать расчёт урона/поиска позиций (кеширование, ранний выход, лимит на тик).
// TODO: разгрузить основную нить: часть калькуляций вынести в batched режим с контролем времени.
// TODO: учесть десинхрон при raytraceBypass и добавить cooldown/rollback при фейле.
// TODO: добавить защиту от спама пакетов при high CPS и строгих античитах (adaptive throttle).
// TODO: валидировать target crystal state (alive/removed) перед атакой и перед setDead.
// TODO: доработать id-predict: предикт оффсет должен сбрасываться при лаге/телепорте.
// TODO: синхронизировать взаимодействие с Suicide/other modules чтобы избегать конфликтов.
// TODO: пересмотреть logic inhibit/fullInhibit чтобы не блокировать выгодные дубли.
// TODO: добавить чёткий приоритет для face-place vs. slow logic, чтобы избежать флипа.
// TODO: добавить проверку смены измерения/серверной телепортации и автоматический reset state.
// TODO: улучшить обработку anti-weakness swap (fallback на оружие + проверка durability).
// TODO: добавить профили ротаций (Instant/Smooth/Hybrid) и интегрировать requireRotation.
// TODO: добавить явный лимит на количество target-entities в extrapolationMap (memory bound).
// TODO: унифицировать Place/Attack delay в один таймерный объект для предотвращения drift.
@OnlyDev
public class AutoCrystal extends Module {
    private static AutoCrystal INSTANCE;

    private final SettingGroup sgPlace = this.addGroup("Place");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgIdPredict = this.addGroup("ID Predict", "Experimental: High risk of server kicks or instability.");
    private final SettingGroup sgInhibit = this.addGroup("Inhibit");
    private final SettingGroup sgSlow = this.addGroup("Slow");
    private final SettingGroup sgFacePlace = this.addGroup("Face Place");
    private final SettingGroup sgRaytraceBypass = this.addGroup("Raytrace Bypass");
    private final SettingGroup sgRotation = this.addGroup("Rotation");
    private final SettingGroup sgSwitch = this.addGroup("Switch");
    private final SettingGroup sgDamage = this.addGroup("Damage");
    private final SettingGroup sgExtrapolation = this.addGroup("Extrapolation");
    private final SettingGroup sgDamageWait = this.addGroup("Damage Wait");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final SettingGroup sgCalculation = this.addGroup("Calculations");
    private final SettingGroup sgCompatibility = this.addGroup("Compatibility");
    private final SettingGroup sgDebug = this.addGroup("Debug");

    public final Setting<Boolean> raytraceBypass = this.sgRaytraceBypass.booleanSetting("Raytrace Bypass", false, "Allows crystals to be placed or hit through obstacles using bypass logic.");
    public final Setting<Integer> raytraceDelay = this.sgRaytraceBypass.intSetting("Bypass Delay", 10, 0, 100, 1, "The interval between attempts when raytrace bypass is active.", this.raytraceBypass::get);
    public final Setting<Integer> raytraceTime = this.sgRaytraceBypass.intSetting("Bypass Duration", 15, 0, 100, 1, "How long the bypass remains active after a successful raytrace check.", this.raytraceBypass::get);
    public final Setting<Integer> raytraceAngle = this.sgRaytraceBypass.intSetting("Minimum Angle", 45, 0, 100, 1, "The minimum required angle for the bypass logic to engage.", this.raytraceBypass::get);

    private final Setting<Double> raytraceBypassValue = this.sgCalculation.doubleSetting("Bypass Score Multiplier", -4.0, -5.0, 5.0, 0.1, "Weighting factor applied to the calculation score when bypass is active.", this.raytraceBypass::get);

    private final Setting<Boolean> place = this.sgPlace.booleanSetting("Crystal Placing", true, "Enables the automatic placement of End Crystals.");
    private final Setting<Boolean> pauseEatPlace = this.sgPlace.booleanSetting("Pause on Consume", false, "Stops crystal placement while the player is eating or drinking.");
    private final Setting<ActionSpeedMode> placeSpeedMode = this.sgPlace.enumSetting("Placement Logic", ActionSpeedMode.Sync, "Determines the timing algorithm used for placing crystals.");
    private final Setting<Double> placeSpeedLimit = this.sgPlace.doubleSetting("Maximum CPS", 0.0, 0.0, 20.0, 0.1, "Limits placement speed (Clicks Per Second). Set to 0 for unlimited.", () -> this.placeSpeedMode.get() == ActionSpeedMode.Sync);
    private final Setting<Double> constantPlaceSpeed = this.sgPlace.doubleSetting("Constant Speed", 10.0, 0.0, 20.0, 0.1, "Fixed interval placement speed used in Sync mode.", () -> this.placeSpeedMode.get() == ActionSpeedMode.Sync);
    private final Setting<Double> placeSpeed = this.sgPlace.doubleSetting("Placement Frequency", 20.0, 0.0, 20.0, 0.1, "The frequency of placement attempts per second.", () -> this.placeSpeedMode.get() == ActionSpeedMode.Normal);
    private final Setting<DelayMode> placeDelayMode = this.sgPlace.enumSetting("Delay Unit", DelayMode.Ticks, "Defines whether placement delay is measured in seconds or game ticks.");
    private final Setting<Double> placeDelay = this.sgPlace.doubleSetting("Placement Delay", 0.0, 0.0, 1.0, 0.01, "Seconds to wait between placement actions.", () -> this.placeDelayMode.get() == DelayMode.Seconds);
    private final Setting<Integer> placeDelayTicks = this.sgPlace.intSetting("Placement Tick Delay", 0, 0, 20, 1, "Ticks to wait between placement actions.", () -> this.placeDelayMode.get() == DelayMode.Ticks);
    private final Setting<Boolean> ahd = this.sgPlace.booleanSetting("Anti-Hull Damage", true, "Prevents placements that would cause excessive environmental damage or block interaction.");
    private final Setting<Integer> ahdTries = this.sgPlace.intSetting("AHD Retry Count", 3, 0, 20, 1, "Number of placement attempts before bypassing AHD.", this.ahd::get);
    private final Setting<Integer> ahdTime = this.sgPlace.intSetting("AHD Window", 20, 0, 100, 1, "Duration in ticks for the AHD safety window.", this.ahd::get);
    private final Setting<Boolean> ignoreItems = this.sgPlace.booleanSetting("Ignore Entity Items", true, "Allows placement even if dropped items are occupying the space.");
    private final Setting<Boolean> ignoreExp = this.sgPlace.booleanSetting("Ignore Exp Orbs", true, "Allows placement even if experience orbs are in the target location.");
    private final Setting<Boolean> requireRotation = this.sgPlace.booleanSetting("Strict Rotations", true, "Only places crystals if the client is looking at the placement position.", () -> SettingUtils.shouldRotate(RotationType.Interact));

    private final Setting<Boolean> attack = this.sgAttack.booleanSetting("Crystal Attacking", true, "Enables the automatic detonation of End Crystals.");
    private final Setting<Integer> attackPackets = this.sgAttack.intSetting("Burst Packets", 1, 0, 5, 1, "The number of attack packets to send simultaneously per hit.");
    private final Setting<Boolean> pauseEatAttack = this.sgAttack.booleanSetting("Pause Attack on Consume", false, "Stops attacking crystals while the player is eating.");
    private final Setting<Boolean> onlyOwn = this.sgAttack.booleanSetting("Own Crystals Only", false, "Restricts the module to only detonate crystals placed by this client.");
    private final Setting<Boolean> antiWeakness = this.sgAttack.booleanSetting("Anti-Weakness", true, "Automatically swaps to a weapon to bypass the Weakness status effect.");
    private final Setting<ExistedMode> existedCheckMode = this.sgAttack.enumSetting("Existence Validation", ExistedMode.Client, "Method used to verify if a crystal exists before attacking.");
    private final Setting<DelayMode> existedMode = this.sgAttack.enumSetting("Detonation Delay Unit", DelayMode.Ticks, "Timing unit for the existence check before detonation.");
    private final Setting<Double> existed = this.sgAttack.doubleSetting("Existence Delay", 0.0, 0.0, 1.0, 0.01, "Minimum time a crystal must exist before it is attacked.", () -> this.existedMode.get() == DelayMode.Seconds);
    private final Setting<Integer> existedTicks = this.sgAttack.intSetting("Existence Tick Delay", 0, 0, 20, 1, "Minimum ticks a crystal must exist before it is attacked.", () -> this.existedMode.get() == DelayMode.Ticks);
    private final Setting<ActionSpeedMode> attackSpeedMode = this.sgAttack.enumSetting("Detonation Logic", ActionSpeedMode.Sync, "Determines the timing algorithm used for attacking crystals.");
    private final Setting<Double> attackSpeedLimit = this.sgAttack.doubleSetting("Max Attack CPS", 0.0, 0.0, 20.0, 0.1, "Limits detonation speed. Set to 0 for unlimited.", () -> this.attackSpeedMode.get() == ActionSpeedMode.Sync);
    private final Setting<Double> constantAttackSpeed = this.sgAttack.doubleSetting("Constant Attack Speed", 10.0, 0.0, 20.0, 0.1, "Fixed interval detonation speed used in Sync mode.", () -> this.attackSpeedMode.get() == ActionSpeedMode.Sync);
    private final Setting<Double> attackSpeed = this.sgAttack.doubleSetting("Detonation Frequency", 20.0, 0.0, 20.0, 0.1, "The frequency of detonation attempts per second.", () -> this.attackSpeedMode.get() == ActionSpeedMode.Normal);
    private final Setting<SetDeadMode> setDead = this.sgAttack.enumSetting("Despawn Simulation", SetDeadMode.Disabled, "Removes the crystal entity client-side immediately after an attack packet is sent.");
    private final Setting<Double> cpsTime = this.sgAttack.doubleSetting("CPS Averaging Window", 5.0, 1.0, 20.0, 0.1, "The timeframe in seconds used to calculate average Clicks Per Second.");

    private final Setting<Integer> predictAttacks = this.sgIdPredict.intSetting("ID Attack Count", 0, 0, 10, 1, "Number of predicted ID attacks to attempt.");
    private final Setting<Integer> idStart = this.sgIdPredict.intSetting("ID Start Offset", 1, 1, 5, 1, "The starting offset for entity ID prediction.");
    private final Setting<Integer> predictStep = this.sgIdPredict.intSetting("ID Incremental Step", 1, 1, 5, 1, "The step value for each predicted ID packet.");
    private final Setting<Integer> predictFlexibility = this.sgIdPredict.intSetting("Prediction Slack", 2, 0, 10, 1, "Adjustment for server latency during ID prediction.");
    private final Setting<Boolean> predictSwing = this.sgIdPredict.booleanSetting("Predictive Swing", true, "Renders a hand swing for predicted attacks.");

    private final Setting<Boolean> inhibit = this.sgInhibit.booleanSetting("Detonation Inhibitor", true, "Prevents redundant attacks on the same crystal position.");
    private final Setting<Boolean> fullInhibit = this.sgInhibit.booleanSetting("Strict Inhibition", true, "Strictly enforces attack limits per crystal.", this.inhibit::get);
    private final Setting<Integer> fullInhibitTicks = this.sgInhibit.intSetting("Inhibition Window", 100, 0, 400, 5, "Duration in ticks before an inhibited position can be targeted again.", () -> this.inhibit.get() && this.fullInhibit.get());
    private final Setting<Integer> fullInhibitAttacks = this.sgInhibit.intSetting("Inhibition Limit", 2, 1, 10, 1, "Number of attacks allowed before inhibition kicks in.", () -> this.inhibit.get() && this.fullInhibit.get());
    private final Setting<Boolean> inhibitCollide = this.sgInhibit.booleanSetting("Collision Inhibition", false, "Prevents new placements from overlapping with recently inhibited crystals.", () -> this.inhibit.get() && this.fullInhibit.get());
    private final Setting<Integer> inhibitTicks = this.sgInhibit.intSetting("Base Inhibit Ticks", 10, 0, 100, 1, "The default inhibition duration.");
    private final Setting<Integer> inhibitAttacks = this.sgInhibit.intSetting("Base Inhibit Attacks", 1, 1, 10, 1, "The default attack limit before inhibition.");

    private final Setting<Double> slowDamage = this.sgSlow.doubleSetting("Low Damage Threshold", 3.0, 0.0, 20.0, 0.1, "Switches to a slower placement rate if damage falls below this value.");
    private final Setting<Double> slowSpeed = this.sgSlow.doubleSetting("Slow Placement Speed", 2.0, 0.0, 20.0, 0.1, "The placement speed used when the low damage threshold is met.");
    private final Setting<Double> slowHealth = this.sgSlow.doubleSetting("Slow Health Buffer", 10.0, 0.0, 20.0, 0.5, "Only applies slow placement logic if the target has health above this value.");

    private final Setting<KeyBind> holdFacePlace = this.sgFacePlace.keySetting("Face-Place Hotkey", "Forces the module to target the head/face area when held.");
    private final Setting<Double> facePlaceHealth = this.sgFacePlace.doubleSetting("Face-Place HP Trigger", 0.0, 0.0, 10.0, 0.1, "Automatically prioritizes face-placing when target health is below this.");
    private final Setting<Double> armorFacePlace = this.sgFacePlace.doubleSetting("Armor Durability Trigger", 10.0, 0.0, 100.0, 1.0, "Face-places if any target armor piece's durability percentage is below this.");
    private final Setting<Double> facePlaceDamage = this.sgFacePlace.doubleSetting("Face-Place Damage Min", 0.0, 0.0, 10.0, 0.1, "Overrides minimum damage requirements when face-placing.");
    private final Setting<Boolean> ignoreSlow = this.sgFacePlace.booleanSetting("Face-Place Speed Override", true, "Ignores the slow placement logic while face-placing is active.");

    private final Setting<Boolean> moveOffset = this.sgRotation.booleanSetting("Prediction Offset", true, "Adjusts rotations based on the target's movement velocity.");
    private final Setting<Double> placeHeight = this.sgRotation.doubleSetting("Placement Pitch Offset", 1.0, 0.0, 1.0, 0.01, "The vertical height target for placement rotations.");
    private final Setting<Double> attackHeight = this.sgRotation.doubleSetting("Attack Pitch Offset", 0.0, 0.0, 2.0, 0.01, "The vertical height target for detonation rotations.");

    private final Setting<ACSwitchMode> switchMode = this.sgSwitch.enumSetting("Auto Crystal Switch", ACSwitchMode.Disabled, "The method used to automatically equip crystals.");
    private final Setting<SwitchMode> antiWeaknessSwitch = this.sgSwitch.enumSetting("Anti-Weakness Mode", SwitchMode.Silent, "The method used to swap to weapons for Anti-Weakness.");
    private final Setting<Double> placeSwitchPenalty = this.sgSwitch.doubleSetting("Placement Swap Delay", 0.0, 0.0, 1.0, 0.05, "Cooldown time after switching items before placement is allowed.");
    private final Setting<Double> attackSwitchPenalty = this.sgSwitch.doubleSetting("Attack Swap Delay", 0.0, 0.0, 1.0, 0.05, "Cooldown time after switching items before detonation is allowed.");

    private final Setting<Double> minPlace = this.sgDamage.doubleSetting("Minimum Placement Damage", 5.0, 0.0, 20.0, 0.1, "Minimum damage required to initiate a placement.");
    private final Setting<Boolean> checkSelfPlacing = this.sgDamage.booleanSetting("Self Placement Safety", true, "Enforces safety checks to prevent high self-damage during placement.");
    private final Setting<Double> maxSelfPlace = this.sgDamage.doubleSetting("Maximum Self-Damage", 10.0, 0.0, 20.0, 0.1, "Highest allowed self-damage value for crystal placement.", this.checkSelfPlacing::get);
    private final Setting<Double> minSelfRatio = this.sgDamage.doubleSetting("Placement Damage Ratio", 2.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Self) for placing.", this.checkSelfPlacing::get);
    private final Setting<Boolean> checkFriendPlacing = this.sgDamage.booleanSetting("Friendly Placement Safety", true, "Enforces safety checks to prevent damaging allies during placement.");
    private final Setting<Double> maxFriendPlace = this.sgDamage.doubleSetting("Max Allied Damage", 12.0, 0.0, 20.0, 0.1, "Highest allowed damage to allies for crystal placement.", this.checkFriendPlacing::get);
    private final Setting<Double> minFriendRatio = this.sgDamage.doubleSetting("Allied Damage Ratio", 1.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Ally) for placing.", this.checkFriendPlacing::get);
    private final Setting<Boolean> checkEnemyAttack = this.sgDamage.booleanSetting("Target Detonation Check", true, "Ensures the explosion deals enough damage to the target.");
    private final Setting<Double> minAttack = this.sgDamage.doubleSetting("Minimum Detonation Damage", 5.0, 0.0, 20.0, 0.1, "Minimum damage required to trigger a detonation.", this.checkEnemyAttack::get);
    private final Setting<Boolean> checkSelfAttack = this.sgDamage.booleanSetting("Self Detonation Safety", true, "Enforces safety checks to prevent high self-damage during detonation.");
    private final Setting<Double> maxSelfAttack = this.sgDamage.doubleSetting("Max Self-Attack Damage", 10.0, 0.0, 20.0, 0.1, "Highest allowed self-damage value for detonation.", this.checkSelfAttack::get);
    private final Setting<Double> minSelfAttackRatio = this.sgDamage.doubleSetting("Detonation Damage Ratio", 2.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Self) for attacking.", () -> this.checkSelfAttack.get() && this.checkEnemyAttack.get());
    private final Setting<Boolean> checkFriendAttack = this.sgDamage.booleanSetting("Friendly Detonation Safety", true, "Enforces safety checks to prevent damaging allies during detonation.");
    private final Setting<Double> maxFriendAttack = this.sgDamage.doubleSetting("Max Allied Attack Damage", 12.0, 0.0, 20.0, 0.1, "Highest allowed damage to allies during detonation.", this.checkFriendAttack::get);
    private final Setting<Double> minFriendAttackRatio = this.sgDamage.doubleSetting("Allied Attack Ratio", 1.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Ally) for attacking.", this.checkFriendAttack::get);
    private final Setting<Double> forcePop = this.sgDamage.doubleSetting("Force Totem Pop", 0.0, 0.0, 5.0, 0.25, "Bypasses damage checks if the explosion is likely to pop an enemy totem.");
    private final Setting<Double> selfPop = this.sgDamage.doubleSetting("Anti Self-Pop Safety", 1.0, 0.0, 5.0, 0.25, "Strictness of self-pop prevention.");
    private final Setting<Double> friendPop = this.sgDamage.doubleSetting("Anti Allied-Pop Safety", 0.0, 0.0, 5.0, 0.25, "Strictness of allied-pop prevention.");
    private final Setting<AntiPopMode> antiPopMode = this.sgDamage.enumSetting("Pop Prevention Mode", AntiPopMode.Change, "Method used to mitigate accidental totem pops.");

    private final Setting<Integer> extrapolation = this.sgExtrapolation.intSetting("Enemy Prediction", 0, 0, 20, 1, "The amount of ticks to predict enemy movement for damage calculations.");
    private final Setting<Integer> selfExt = this.sgExtrapolation.intSetting("Self Prediction", 0, 0, 20, 1, "The amount of ticks to predict your own movement for damage calculations.");
    private final Setting<RangeExtMode> rangeExtMode = this.sgExtrapolation.enumSetting("Range Scaling Mode", RangeExtMode.Semi, "Determines how ranges are calculated for moving entities.");
    private final Setting<Integer> rangeExt = this.sgExtrapolation.intSetting("Range Prediction", 0, 0, 20, 1, "Ticks of movement prediction applied to reachability checks.");
    private final Setting<Double> hitboxExpand = this.sgExtrapolation.doubleSetting("Hitbox Dilation", 1.0, 0.0, 2.0, 0.02, "Multiplies the size of target hitboxes during damage calculation.");
    private final Setting<Boolean> flexibleHitbox = this.sgExtrapolation.booleanSetting("Adaptive Hitboxes", false, "Varies hitbox size based on entity movement and latency.", () -> this.hitboxExpand.get() > 0.0);
    private final Setting<Boolean> extrapolateHitbox = this.sgExtrapolation.booleanSetting("Hitbox Extrapolation", false, "Predicts the physical location of the target's hitbox in future ticks.");
    private final Setting<Double> preferHitboxExpand = this.sgExtrapolation.doubleSetting("Static Dilation Value", 2.0, 0.0, 2.0, 0.02, "The fallback dilation value for static targets.");
    private final Setting<Double> hitboxValue = this.sgExtrapolation.doubleSetting("Hitbox Influence", -8.0, -10.0, 10.0, 0.2, "Score adjustment based on hitbox proximity.");

    private final Setting<Boolean> damageWait = this.sgDamageWait.booleanSetting("Delay for Max Damage", false, "Waits to detonate until the target reaches the highest predicted damage position.");
    private final Setting<Integer> waitStartExt = this.sgDamageWait.intSetting("Wait Entry Prediction", 2, 0, 20, 1, "Ticks of prediction to start the wait logic.");
    private final Setting<Integer> waitEndExt = this.sgDamageWait.intSetting("Wait Peak Prediction", 5, 0, 20, 1, "Ticks of prediction to end the wait logic.");
    private final Setting<Double> minDifference = this.sgDamageWait.doubleSetting("Wait Sensitivity", 0.0, 0.0, 10.0, 0.1, "Minimum damage improvement required to justify waiting.");
    private final Setting<Integer> maxWait = this.sgDamageWait.intSetting("Maximum Wait Ticks", 3, 0, 20, 1, "Maximum duration the detonator will wait for peak damage.");

    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Placement Swing", false, "Shows a hand swing animation when placing a crystal.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Placement Hand", SwingHand.RealHand, "The hand used for the placement animation.");
    private final Setting<Boolean> attackSwing = this.sgRender.booleanSetting("Attack Swing", false, "Shows a hand swing animation when attacking a crystal.");
    private final Setting<SwingHand> attackHand = this.sgRender.enumSetting("Attack Hand", SwingHand.RealHand, "The hand used for the attack animation.");
    private final Setting<Boolean> render = this.sgRender.booleanSetting("Visual Highlights", true, "Renders visual indicators for placement and detonation.");
    private final Setting<RenderMode> renderMode = this.sgRender.enumSetting("Visual Style", RenderMode.BlackOut, "Selects the aesthetic style of the block highlights.", this.render::get);
    private final Setting<Double> textScale = this.sgRender.doubleSetting("Damage Text Size", 0.3, 0.0, 1.0, 0.01, "The size of the damage numbers rendered at the placement site.");
    private final Setting<Double> renderTime = this.sgRender.doubleSetting("Highlight Persistence", 0.3, 0.0, 10.0, 0.1, "How long the block highlight remains at full opacity.");
    private final Setting<Double> fadeTime = this.sgRender.doubleSetting("Fading Speed", 1.0, 0.0, 10.0, 0.1, "The time it takes for the highlight to fade to zero alpha.");
    private final Setting<Double> animMoveSpeed = this.sgRender.doubleSetting("Interpolation Speed", 2.0, 0.0, 10.0, 0.1, "How quickly the highlight box moves between positions.");
    private final Setting<Double> animMoveExponent = this.sgRender.doubleSetting("Acceleration Curve", 3.0, 0.0, 10.0, 0.1, "Exponential factor for the movement animation speed.");
    private final Setting<Double> animSizeExponent = this.sgRender.doubleSetting("Scaling Curve", 3.0, 0.0, 10.0, 0.1, "Exponential factor for the box scaling animation.");
    private final Setting<AnimationMode> animationMode = this.sgRender.enumSetting("Scaling Logic", AnimationMode.Full, "Determines how the highlight box scales during its animation.");
    private final BoxMultiSetting renderSetting = BoxMultiSetting.of(this.sgRender, "Highlighter Settings");
    private final Setting<Boolean> renderDamage = this.sgRender.booleanSetting("Show Damage Values", true, "Renders floating text indicating potential damage.");
    private final Setting<Boolean> renderExt = this.sgRender.booleanSetting("Render Enemy Prediction", false, "Visualizes the extrapolated positions of targeted players.");
    private final Setting<Boolean> renderBoxExt = this.sgRender.booleanSetting("Render Prediction Boxes", false, "Visualizes the extrapolated hitboxes of targeted players.");
    private final Setting<Boolean> renderSelfExt = this.sgRender.booleanSetting("Render Self Prediction", false, "Visualizes your own extrapolated position.");

    private final Setting<Double> damageValue = this.sgCalculation.doubleSetting("Target Damage Score", 1.0, -5.0, 5.0, 0.1, "Scoring weight for damage dealt to enemies.");
    private final Setting<Double> selfDmgValue = this.sgCalculation.doubleSetting("Self Damage Score", -1.0, -5.0, 5.0, 0.05, "Scoring weight (penalty) for damage dealt to yourself.");
    private final Setting<Double> friendDmgValue = this.sgCalculation.doubleSetting("Allied Damage Score", 0.0, -5.0, 5.0, 0.05, "Scoring weight (penalty) for damage dealt to allies.");
    private final Setting<Double> moveValue = this.sgCalculation.doubleSetting("Proximity Direction Score", 0.0, -5.0, 5.0, 0.1, "Score bonus for targets moving toward the explosion.");
    private final Setting<Double> selfMoveValue = this.sgCalculation.doubleSetting("Self Proximity Score", 0.0, -5.0, 5.0, 0.1, "Score penalty for yourself moving toward the explosion.");
    private final Setting<Double> friendMoveValue = this.sgCalculation.doubleSetting("Allied Proximity Score", 0.0, -5.0, 5.0, 0.1, "Score penalty for allies moving toward the explosion.");
    private final Setting<Double> rotationValue = this.sgCalculation.doubleSetting("Look Vector Score", 0.0, -5.0, 5.0, 0.1, "Score bonus based on how close the rotation is to your current look vector.");
    private final Setting<Double> wallValue = this.sgCalculation.doubleSetting("Occlusion Penalty", 0.0, -5.0, 5.0, 0.1, "Score adjustment for targets behind walls.");
    private final Setting<Double> noRotateValue = this.sgCalculation.doubleSetting("Fixed View Score", 0.0, -5.0, 5.0, 0.1, "Score bonus when using placements that don't require camera rotations.", SettingUtils::rotationIgnoreEnabled);
    private final Setting<Integer> maxTargets = this.sgCalculation.intSetting("Maximum Target Count", 3, 1, 10, 1, "The number of nearby players to track simultaneously for optimal placement.");
    private final Setting<Boolean> noCollide = this.sgCalculation.booleanSetting("Intersection Prevention", false, "Prevents placement if existing crystals intersect the target position.");
    private final Setting<Boolean> spawningCollide = this.sgCalculation.booleanSetting("Spawn Safety Check", false, "Prevents placement if newly spawning crystals are in the way.", this.noCollide::get);
    private final Setting<Boolean> attackCollide = this.sgCalculation.booleanSetting("Detonation Safety Check", false, "Bypasses collision checks for crystals currently being targeted for attack.", this.noCollide::get);
    private final Setting<Double> antiJitter = this.sgCalculation.doubleSetting("Input Smoothing", 0.5, 0.0, 5.0, 0.1, "Reduces rapid flickering of the target placement position.");
    private final Setting<Double> antiJitterTime = this.sgCalculation.doubleSetting("Smoothing Window", 0.2, 0.0, 1.0, 0.01, "Duration in seconds for the smoothing logic.", () -> this.antiJitter.get() != 0.0);
    private final Setting<Double> autoMineCollideValue = this.sgCalculation.doubleSetting("AutoMine Interference Score", 0.0, -5.0, 5.0, 0.1, "Score adjustment when a placement coincides with an active AutoMine block.");
    private final Setting<AsyncMode> async = this.sgCalculation.enumSetting("Thread Performance", AsyncMode.Basic, "Determines the threading model for calculation tasks.");
    private final Setting<Boolean> rotationFriendly = this.sgCalculation.booleanSetting("Smooth Orienting", true, "Optimizes rotations to prevent jerky camera movement.");
    private final Setting<Double> rangeValue = this.sgCalculation.doubleSetting("Proximity Advantage Score", 1.0, -5.0, 5.0, 0.1, "Score bonus for placements closer to the player.");
    private final Setting<Double> rangeStartDist = this.sgCalculation.doubleSetting("Advantage Min Distance", 0.0, 0.0, 6.0, 0.1, "Distance at which the proximity score bonus begins.", () -> this.rangeValue.get() != 0.0);
    private final Setting<Boolean> eco = this.sgCalculation.booleanSetting("Economic Detonation", false, "Optimizes crystal usage to minimize waste.");

    private final Setting<Double> prePlaceProgress = this.sgCompatibility.doubleSetting("Handshake Buffering", 0.9, 0.0, 1.0, 0.01, "Optimizes packet timing for server compatibility.");
    private final Setting<Boolean> autoMineAttack = this.sgCompatibility.booleanSetting("AutoMine Detonation Sync", true, "Synchronizes attacks with AutoMine progress.");
    private final Setting<Double> autoMineAttackProgress = this.sgCompatibility.doubleSetting("AutoMine Sync Threshold", 0.75, 0.0, 1.0, 0.01, "Percentage of block mining completion before triggering detonation sync.", this.autoMineAttack::get);

    private final Setting<Boolean> debugPlace = this.sgDebug.booleanSetting("Log Placement", false, "Prints placement debug information to the console.");
    private final Setting<Boolean> debugAttack = this.sgDebug.booleanSetting("Log Detonation", false, "Prints attack debug information to the console.");
    private final Setting<Boolean> removeTime = this.sgDebug.booleanSetting("Log Despawn Timing", false, "Prints the time taken for crystals to despawn.");

    private final TimerList<Box> spawning = new TimerList<>(true);
    private final TickTimerList<BlockPos> existedTicksList = new TickTimerList<>(true);
    private final TimerList<BlockPos> existedList = new TimerList<>(true);
    private final TickTimerList<BlockPos> placeDelayTicksList = new TickTimerList<>(true);
    private final TimerList<BlockPos> placeDelayList = new TimerList<>(true);
    private final TimerList<Integer> attackTimers = new TimerList<>(true);
    private final ExtrapolationMap extMap = new ExtrapolationMap();
    private final ExtrapolationMap minWaitExtMap = new ExtrapolationMap();
    private final ExtrapolationMap maxWaitExtMap = new ExtrapolationMap();
    private final Map<Entity, Box> boxMap = new HashMap<>();
    private final List<Box> valueBoxes = new ArrayList<>();
    private final TickTimerList<Integer> attacked = new TickTimerList<>(true);
    private final TickTimerList<int[]> inhibitList = new TickTimerList<>(true);
    private final TickTimerList<int[]> fullInhibitList = new TickTimerList<>(true);
    private final TimerList<BlockPos> own = new TimerList<>(true);
    private final TimerMap<BlockPos, Integer> hitBoxDesyncList = new TimerMap<>(true);
    private final TickTimerList<Integer> waitTimes = new TickTimerList<>(true);
    private final List<PlayerEntity> targets = new ArrayList<>();
    private final Map<PlayerEntity, Float> moveDirs = new HashMap<>();
    private final RenderList<BlockPos> earthRender = RenderList.getList(false);
    private final List<Long> explosions = Collections.synchronizedList(new ArrayList<>());
    private final Predicate<ItemStack> antiWeaknessPredicate = stack -> stack.getItem() instanceof ToolItem;
    public BlockPos placePos = null;
    public double enemyDamage = 0.0;
    public boolean placing = false;
    public AbstractClientPlayerEntity targetedPlayer = null;
    private double moveModifier = 0.0;
    private Vec3d rangePos = null;
    private boolean shouldCalc = false;
    private EndCrystalEntity targetCrystal = null;
    private LivingEntity target = null;
    private double selfHealth = 0.0;
    private double enemyHealth = 0.0;
    private double friendHealth = 0.0;
    private double selfDamage = 0.0;
    private double friendDamage = 0.0;
    private boolean isPop = false;
    private boolean suicide = false;
    private long lastAttack = 0L;
    private long lastPlace = 0L;
    private long lastSwitch = 0L;
    private long lastCalc = 0L;
    private long lastChange = 0L;
    private boolean facePlacing = false;
    private Vec3d movement = new Vec3d(0.0, 0.0, 0.0);
    private double renderProgress = 0.0;
    private BlockPos renderPos = BlockPos.ORIGIN;
    private Vec3d renderVec = Vec3d.ZERO;
    private Vec3d renderTargetVec = Vec3d.ZERO;
    private Direction crystalDir = Direction.DOWN;
    private FindResult crystalResult = null;
    private Hand crystalHand = null;
    private boolean lastWasAttack = false;
    private boolean antiWeaknessAvailable = false;
    private FindResult awResult;
    private double cps = 0.0;
    private int bypassTimer = 0;
    private int raytraceLeft = 0;
    private int confirmedId = 0;
    private int sentId = 0;

    public AutoCrystal() {
        super("Auto Crystal", "An advanced offensive utility for high-speed End Crystal placement and detonation.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static AutoCrystal getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.placePos = null;
        this.shouldCalc = true;
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Override
    public String getInfo() {
        return String.format("%.1f", this.cps);
    }

    @Event
    public void onEntity(EntityAddEvent.Pre event) {
        if (this.predictAttacks.get() > 0) {
            this.confirmedId = Math.max(this.confirmedId, event.id);
            if (this.sentId > this.confirmedId) {
                this.sentId = MathHelper.clamp(this.sentId, this.confirmedId, this.confirmedId + this.predictFlexibility.get());
            } else {
                this.sentId = this.confirmedId;
            }
        }

        if (this.enabled) {
            if (event.entity instanceof EndCrystalEntity) {
                BlockPos p = event.entity.getBlockPos();
                if (p.equals(this.placePos)) {
                    this.explosions.add(System.currentTimeMillis());
                }

                if (this.existedCheckMode.get() == ExistedMode.Client) {
                    this.addExisted(p);
                }

                this.placeDelayList.remove(timer -> timer.value.equals(p));
                this.placeDelayTicksList.remove(timer -> timer.value.equals(p));
                this.spawning.remove(timer -> BlockPos.ofFloored(BoxUtils.feet(timer.value)).equals(event.entity.getBlockPos()));
                if (this.ahd.get()) {
                    this.hitBoxDesyncList.remove((pos, timer) -> pos.equals(p.down()));
                }
            }
        }
    }

    @Event
    public void onEntity(EntityAddEvent.Post event) {
        if (this.enabled && event.entity instanceof EndCrystalEntity) {
            switch (this.async.get()) {
                case Basic:
                    this.updateAttacking();
                    break;
                case Dumb:
                    this.updateAttacking();
                    this.updatePlacing(true);
                    break;
                case Heavy:
                    if (this.updateCalc()) {
                        this.updatePos();
                    }

                    this.update(true);
            }
        }
    }

    @Event
    public void onMovePre(MoveEvent.Pre event) {
        this.moveModifier -= 0.1;
        this.moveModifier = this.moveModifier + event.movement.length();
        this.moveModifier = MathHelper.clamp(this.moveModifier, 0.0, 1.0);
    }

    @Event
    public void onMove(MoveEvent.Post event) {
        if (this.enabled) {
            if (this.updateCalc()) {
                this.updatePos();
            }

            this.update(false);
        }
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.enabled) {
            this.raytraceLeft--;
            if (this.shouldRaytraceBypass(this.placePos) && ++this.bypassTimer > this.raytraceDelay.get()) {
                Rotation rotation = this.raytraceRotation(this.placePos, true);
                if (rotation != null) {
                    this.rotate(rotation.yaw(), rotation.pitch(), 1.0, RotationType.Other, "raytrace");
                    this.bypassTimer = 0;
                    this.raytraceLeft = this.raytraceTime.get();
                }
            }

            if (this.ahd.get()) {
                this.hitBoxDesyncList.remove((pos, timer) -> !this.almostColliding(pos.up()));
            }
        }
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.enabled) {
            this.end("raytrace");
            this.movement = BlackOut.mc
                    .player
                    .getPos()
                    .subtract(BlackOut.mc.player.prevX, BlackOut.mc.player.prevY, BlackOut.mc.player.prevZ);
            this.update(true);
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (!this.enabled) {
            this.updateRender(event.frameTime, true);
        } else {
            this.updateFacePlace();
            this.updateAntiWeakness();
            this.cps = 0.0;
            synchronized (this.explosions) {
                this.explosions.removeIf(time -> {
                    double p = (System.currentTimeMillis() - time) / 1000.0;
                    if (p >= this.cpsTime.get()) {
                        return true;
                    } else {
                        double d = Math.min(this.cpsTime.get() - p, 1.0);
                        this.cps += d;
                        return false;
                    }
                });
            }

            this.cps = this.cps / (this.cpsTime.get() - 0.5);
            if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
                this.update(true);
                this.updateRender(event.frameTime, false);
            }
        }
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (this.enabled) {
            if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
                this.lastSwitch = System.currentTimeMillis();
            }

            if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
                this.own.replace(packet.getBlockHitResult().getBlockPos().up(), 2.0);
            }
        }
    }

    @Event
    public void onRemove(RemoveEvent event) {
        if (this.removeTime.get() && event.entity instanceof EndCrystalEntity entity) {
            long diff = System.currentTimeMillis() - ((IEndCrystalEntity) entity).blackout_Client$getSpawnTime();
            this.debug("removed after", diff + "ms");
        }
    }

    private void debug(String string, String value) {
        ChatUtils.addMessage(string + " " + Formatting.AQUA + value);
    }

    private void updateAntiWeakness() {
        this.antiWeaknessAvailable = this.canAntiWeakness();
    }

    private boolean canAntiWeakness() {
        this.awResult = null;
        if (!this.antiWeakness.get()) {
            return false;
        } else {
            return this.antiWeaknessPredicate.test(Managers.PACKET.getStack()) || (this.awResult = this.antiWeaknessSwitch.get().find(this.antiWeaknessPredicate)).wasFound();
        }
    }

    private void updateMaps() {
        this.updateMap(this.extMap, playerx -> playerx == BlackOut.mc.player ? this.selfExt.get() : this.extrapolation.get());
        if (this.damageWait.get()) {
            this.updateMap(this.minWaitExtMap, playerx -> this.waitStartExt.get());
            this.updateMap(this.maxWaitExtMap, playerx -> this.waitEndExt.get());
        }

        this.boxMap.clear();

        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            Box intersectsBox = this.expanded(player, this.extrapolateHitbox.get(), this.hitboxExpand.get());
            this.boxMap.put(player, intersectsBox);
        }

        this.valueBoxes.clear();

        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            Box valueBox = this.expanded(player, false, this.preferHitboxExpand.get());
            this.valueBoxes.add(valueBox);
        }
    }

    private Box expanded(AbstractClientPlayerEntity player, boolean extrapolate, double multi) {
        Vec3d velocity = this.clampVec(player.getPos(), player.prevX, player.prevY, player.prevZ);
        Vec3d newVelocity = MovementPrediction.adjustMovementForCollisions(player, velocity);
        Box box;
        if (extrapolate) {
            box = player.getBoundingBox().offset(newVelocity.getX(), 0.0, newVelocity.getZ()).stretch(0.0, newVelocity.getY(), 0.0);
        } else {
            box = player.getBoundingBox();
        }

        List<VoxelShape> list = BlackOut.mc.world.getEntityCollisions(player, box.stretch(velocity));
        Vec3d vec = Entity.adjustMovementForCollisions(player, velocity.multiply(multi, 0.0, multi), box, BlackOut.mc.world, list);
        return box.stretch(vec);
    }

    private Vec3d clampVec(Vec3d pos, double x, double y, double z) {
        Vec3d vec = pos.subtract(x, y, z);
        double lengthH = vec.horizontalLength();
        if (lengthH > 0.3) {
            double sus = 0.3 / lengthH;
            return vec.multiply(sus, 1.0, sus);
        } else {
            return vec;
        }
    }

    private void updateMap(ExtrapolationMap extrapolationMap, EpicInterface<Entity, Integer> ticks) {
        Map<Entity, Box> map = extrapolationMap.getMap();
        map.clear();
        Managers.EXTRAPOLATION.getDataMap().forEach((player, data) -> {
            if (this.targets.contains(player)) {
                Box box = data.extrapolate(player, ticks.get(player));
                map.put(player, box);
            }
        });
    }

    private void updateTargets() {
        Map<PlayerEntity, Double> map = new HashMap<>();

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && !(player.getHealth() <= 0.0F)) {
                double distance = BlackOut.mc.player.distanceTo(player);
                if (!(distance > 15.0)) {
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
        this.targets.add(BlackOut.mc.player);
        map.forEach((playerx, d) -> this.targets.add(playerx));
        this.moveDirs.clear();
        this.targets.forEach(playerx -> {
            Vec3d movement = playerx.getPos().subtract(playerx.prevX, playerx.prevY, playerx.prevZ);
            if (!(movement.horizontalLengthSquared() < 0.01)) {
                this.moveDirs.put(playerx, (float) RotationUtils.getYaw(Vec3d.ZERO, movement, 0.0));
            }
        });
        this.updateMaps();
    }

    private boolean updateCalc() {
        if (this.shouldCalc()) {
            this.shouldCalc = true;
        }

        return this.shouldCalc;
    }

    private void updateRender(double delta, boolean disabled) {
        if (this.render.get()) {
            this.renderBasic(delta, disabled);
            if (!disabled) {
                this.renderExtrapolation();
            }
        }
    }

    private void renderBasic(double delta, boolean disabled) {
        boolean renderActive = this.placePos != null && this.placing && !disabled;
        this.renderProgress = MathHelper.clamp(this.renderProgress + (renderActive ? delta : -delta), 0.0, this.fadeTime.get() + this.renderTime.get());
        double p = Math.min(this.renderProgress, this.fadeTime.get()) / this.fadeTime.get();
        switch (this.renderMode.get()) {
            case Earthhack:
                this.earthRender
                        .update(
                                (pos, time, d) -> {
                                    float progressx = (float) (1.0 - Math.max(time - this.renderTime.get(), 0.0) / this.fadeTime.get());
                                    this.renderSetting.render(BoxUtils.get(pos), progressx, 1.0F);
                                    this.calcDamage(new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), false);
                                    if (this.renderDamage.get()) {
                                        Render3DUtils.text(
                                                String.format("%.1f", this.enemyDamage), pos.toCenterPos(), new Color(255, 255, 255, (int) (progressx * 255.0F)).getRGB(), textScale.get().floatValue()
                                        );
                                    }
                                }
                        );
                break;
            case BlackOut:
                if (renderActive) {
                    this.renderPos = this.placePos;
                    this.renderTargetVec = new Vec3d(this.placePos.getX() + 0.5, this.placePos.getY() - 0.5, this.placePos.getZ() + 0.5);
                }

                if (this.renderProgress <= 0.0) {
                    this.renderVec = this.renderTargetVec;
                } else {
                    this.moveRender(delta);
                }

                double progress = 1.0 - Math.pow(1.0 - p, this.animSizeExponent.get());
                if (p > 0.0) {
                    this.renderSetting.render(this.getBox(this.renderVec, progress / 2.0), (float) p, (float) p);
                    if (this.renderDamage.get()) {
                        this.calcDamage(new Vec3d(this.renderPos.getX() + 0.5, this.renderPos.getY(), this.renderPos.getZ() + 0.5), false);
                        Render3DUtils.text(String.format("%.1f", this.enemyDamage), this.renderVec, new Color(255, 255, 255, (int) (progress * 255.0)).getRGB(), textScale.get().floatValue());
                    }
                }
                break;
            case Simple:
                if (renderActive) {
                    this.renderPos = this.placePos.down();
                }

                if (p > 0.0) {
                    this.renderSetting.render(BoxUtils.get(this.renderPos), (float) p, (float) p);
                    this.calcDamage(
                            new Vec3d(this.renderPos.getX() + 0.5, this.renderPos.getY() + 1, this.renderPos.getZ() + 0.5), false
                    );
                    if (this.renderDamage.get()) {
                        Render3DUtils.text(
                                String.format("%.1f", this.enemyDamage), this.renderPos.toCenterPos(), new Color(255, 255, 255, (int) (p * 255.0)).getRGB(), textScale.get().floatValue()
                        );
                    }
                }
                break;
            case Confirm:
                if (p > 0.0) {
                    this.renderSetting.render(BoxUtils.get(this.renderPos), (float) p, (float) p);
                    this.calcDamage(
                            new Vec3d(this.renderPos.getX() + 0.5, this.renderPos.getY() + 1, this.renderPos.getZ() + 0.5), false
                    );
                    if (this.renderDamage.get()) {
                        Render3DUtils.text(
                                String.format("%.1f", this.enemyDamage), this.renderPos.toCenterPos(), new Color(255, 255, 255, (int) (p * 255.0)).getRGB(), textScale.get().floatValue()
                        );
                    }
                }
        }
    }

    private void moveRender(double delta) {
        double dist = this.renderVec.distanceTo(this.renderTargetVec);
        double movement = (this.animMoveSpeed.get() * 5.0 + dist * (this.animMoveExponent.get() - 1.0) * 3.0) * delta;
        double newDist = MathHelper.clamp(dist - movement, 0.0, dist);
        double f = dist == 0.0 && newDist == 0.0 ? 1.0 : newDist / dist;
        Vec3d offset = this.renderTargetVec.subtract(this.renderVec);
        Vec3d m = offset.multiply(1.0 - f);
        this.renderVec = this.renderVec.add(m);
    }

    private Box getBox(Vec3d middle, double p) {
        double up = 0.5;
        double down = 0.5;
        double sides = 0.5;
        switch (this.animationMode.get()) {
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
        }

        return new Box(
                middle.getX() - sides,
                middle.getY() - down,
                middle.getZ() - sides,
                middle.getX() + sides,
                middle.getY() + up,
                middle.getZ() + sides
        );
    }

    private void renderExtrapolation() {
        if (this.renderExt.get()) {
            this.extMap.forEach((player, box) -> {
                if (player != BlackOut.mc.player) {
                    this.renderSetting.render(box);
                }
            });
        }

        if (this.renderBoxExt.get()) {
            this.boxMap.forEach((player, box) -> this.renderSetting.render(box));
        }

        if (this.renderSelfExt.get() && this.extMap.contains(BlackOut.mc.player)) {
            this.renderSetting.render(this.extMap.get(BlackOut.mc.player));
        }
    }

    private void update(boolean canPlace) {
        this.placing = false;
        if (this.updateAttacking()) {
            this.end("attacking");
        }

        this.updatePlacing(canPlace);
        if (this.placing) {
            this.calcDamage(new Vec3d(this.placePos.getX() + 0.5, this.placePos.getY(), this.placePos.getZ() + 0.5), false);
            if (this.target instanceof AbstractClientPlayerEntity player) {
                this.targetedPlayer = player;
            } else {
                this.targetedPlayer = null;
            }
        } else {
            this.targetedPlayer = null;
        }
    }

    private boolean canPlace() {
        this.crystalResult = this.switchMode.get().find(Items.END_CRYSTAL);
        if (this.switchMode.get() == ACSwitchMode.Gapple && this.gappleSwitch(this.placePos != null)) {
            return false;
        } else if (this.placePos == null) {
            return false;
        } else {
            this.crystalHand = OLEPOSSUtils.getHand(stack -> stack.getItem() == Items.END_CRYSTAL);
            if (this.crystalHand == null && !this.crystalResult.wasFound()) {
                return false;
            } else if (this.pauseEatPlace.get() && BlackOut.mc.player.isUsingItem()) {
                return false;
            } else if (System.currentTimeMillis() - this.lastSwitch < this.placeSwitchPenalty.get() * 1000.0) {
                return false;
            } else if (this.targetCrystal != null && this.eco.get()) {
                return false;
            } else {
                this.crystalDir = SettingUtils.getPlaceOnDirection(this.placePos.down());
                return this.crystalDir != null;
            }
        }
    }

    private boolean canAttack() {
        if (!this.attack.get()) {
            return false;
        } else {
            this.targetCrystal = null;
            double bestVal = 0.0;

            for (Entity entity : BlackOut.mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity crystal && this.canAttack(entity, null)) {
                    double value = this.getAttackValue(crystal);
                    if (this.targetCrystal == null || !(value < bestVal)) {
                        this.targetCrystal = crystal;
                        bestVal = value;
                    }
                }
            }

            if (this.targetCrystal == null) {
                return false;
            } else if (this.hasWeakness() && !this.antiWeaknessAvailable) {
                return false;
            } else {
                return (!this.pauseEatAttack.get() || !BlackOut.mc.player.isUsingItem()) && this.existedCheck(this.targetCrystal);
            }
        }
    }

    private boolean shouldDamageWait(Entity entity) {
        double min = this.getHighestDamage(this.minWaitExtMap, entity);
        double max = this.getHighestDamage(this.maxWaitExtMap, entity);
        double difference = max - min;
        if (difference < this.minDifference.get()) {
            return false;
        } else {
            if (!this.waitTimes.contains(entity.getId())) {
                this.waitTimes.add(entity.getId(), this.maxWait.get() + 50);
            }

            int ticksLeft = this.waitTimes.get(timer -> timer.value == entity.getId()).ticks;
            return ticksLeft > 50;
        }
    }

    private double getHighestDamage(ExtrapolationMap map, Entity entity) {
        AtomicReference<Double> highest = new AtomicReference<>(0.0);

        for (PlayerEntity player : this.targets) {
            if (this.suicide == (player == BlackOut.mc.player) && !Managers.FRIENDS.isFriend(player)) {
                highest.set(Math.max(highest.get(), this.crystalDamage(player, map.get(player), entity.getPos())));
            }
        }

        return highest.get();
    }

    private boolean hasWeakness() {
        return BlackOut.mc.player.getActiveStatusEffects().containsKey(StatusEffects.WEAKNESS);
    }

    private void updateFacePlace() {
        this.facePlacing = this.holdFacePlace.get().isPressed();
    }

    private boolean updateAttacking() {
        this.placing = false;
        if (!this.canAttack()) {
            return true;
        } else if (!this.doAttackRotate()) {
            return false;
        } else if (this.shouldAutoMineStop(this.targetCrystal)) {
            return false;
        } else if (System.currentTimeMillis() - this.lastSwitch < this.attackSwitchPenalty.get() * 1000.0) {
            return false;
        } else {
            switch (this.attackSpeedMode.get()) {
                case Sync:
                    if (this.attackSpeedLimit.get() > 0.0 && System.currentTimeMillis() - this.lastAttack <= 1000.0 / this.attackSpeedLimit.get()) {
                        return false;
                    }

                    if (this.attackTimers.contains(this.targetCrystal.getId())) {
                        return false;
                    }
                    break;
                case Normal:
                    if (System.currentTimeMillis() - this.lastAttack <= 1000.0 / this.attackSpeed.get()) {
                        return false;
                    }
            }

            if (this.startAntiWeakness()) {
                return false;
            } else {
                this.attack(this.targetCrystal.getId(), this.targetCrystal.getPos(), false);
                this.endAntiWeakness();
                return true;
            }
        }
    }

    private boolean startAntiWeakness() {
        return this.hasWeakness() && this.awResult != null && !this.antiWeaknessSwitch.get().swap(this.awResult.slot());
    }

    private void endAntiWeakness() {
        if (this.hasWeakness() && this.awResult != null) {
            this.antiWeaknessSwitch.get().swapBack();
        }
    }

    private boolean doAttackRotate() {
        if (this.shouldRaytraceBypass(this.placePos) && this.raytraceLeft > 0) {
            return true;
        } else if (!SettingUtils.shouldRotate(RotationType.Attacking)) {
            return true;
        } else {
            return SettingUtils.shouldIgnoreRotations(this.targetCrystal)
                    ? this.checkAttackLimit()
                    : this.attackRotate(
                    this.targetCrystal.getBoundingBox(), this.getAttackVec(this.targetCrystal.getPos()), this.lastWasAttack ? -0.1 : 0.1, "attacking"
            );
        }
    }

    private void attack(int id, Vec3d vec, boolean predict) {
        BlockPos pos = BlockPos.ofFloored(vec);
        if (!predict) {
            this.attackTimers.add(id, 1.0 / this.constantAttackSpeed.get());
            this.lastAttack = System.currentTimeMillis();
            Managers.ENTITY.setSemiDead(id);
            this.lastWasAttack = true;
            if (this.attacked.contains(id)) {
                this.attacked.remove(timer -> timer.value == id);
            }

            this.attacked.add(id, 10);
            if (this.inhibit.get()) {
                TickTimerList.TickTimer<int[]> t = this.inhibitList.get(timer -> timer.value[0] == id);
                int[] i;
                if (t != null) {
                    t.value[1]--;
                    i = t.value;
                } else {
                    i = new int[]{id, this.inhibitAttacks.get() - 1};
                }

                this.inhibitList.remove(t);
                this.inhibitList.add(i, this.inhibitTicks.get());
            }

            if (this.inhibit.get() && this.fullInhibit.get()) {
                TickTimerList.TickTimer<int[]> t = this.fullInhibitList.get(timer -> timer.value[0] == id);
                int[] i;
                if (t != null) {
                    t.value[1]--;
                    i = t.value;
                } else {
                    i = new int[]{id, this.fullInhibitAttacks.get() - 1};
                }

                this.fullInhibitList.remove(t);
                this.fullInhibitList.add(i, this.fullInhibitTicks.get());
            }
        }

        for (int i = 0; i < (predict ? 1 : this.attackPackets.get()); i++) {
            this.sendAttack(id, !predict || this.predictSwing.get());
        }

        if (!predict) {
            this.addPlaceDelay(pos);
            this.existedTicksList.remove(timer -> timer.value.equals(pos));
            this.existedList.remove(timer -> timer.value.equals(pos));
            this.spawning.clear();
            this.end("attacking");
            if (this.debugAttack.get()) {
                this.debug("attacked after", System.currentTimeMillis() - ((IEndCrystalEntity) this.targetCrystal).blackout_Client$getSpawnTime() + "ms");
            }

            if (this.setDead.get() != SetDeadMode.Disabled) {
                Managers.ENTITY.setDead(id, this.setDead.get() == SetDeadMode.Full);
            }
        } else if (this.debugAttack.get()) {
            this.debug("predicted", id + " (" + this.confirmedId + " " + this.sentId + ")");
        }
    }

    private void sendAttack(int id, boolean swing) {
        PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(BlackOut.mc.player, BlackOut.mc.player.isSneaking());
        ((AccessorInteractEntityC2SPacket) packet).setId(id);
        if (swing) {
            SettingUtils.swing(SwingState.Pre, SwingType.Attacking, Hand.MAIN_HAND);
        }

        this.sendPacket(packet);
        if (swing) {
            SettingUtils.swing(SwingState.Post, SwingType.Attacking, Hand.MAIN_HAND);
        }

        if (this.attackSwing.get()) {
            this.clientSwing(this.attackHand.get(), Hand.MAIN_HAND);
        }
    }

    private boolean isBlocked(BlockPos pos) {
        Box box = new Box(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1,
                pos.getY() + (SettingUtils.cc() ? 1 : 2),
                pos.getZ() + 1
        );

        for (TimerList.Timer<Box> t : this.spawning.getTimers()) {
            if (t.value.intersects(box)) {
                return true;
            }
        }

        return false;
    }

    private boolean almostExistedCheck(Entity entity) {
        BlockPos pos = entity.getBlockPos();
        if (this.existedMode.get() == DelayMode.Seconds) {
            if (!this.existedList.contains(pos)) {
                return true;
            } else {
                double time = (this.existedList.getEndTime(pos) - System.currentTimeMillis()) / 1000.0;
                return time <= 0.35;
            }
        } else if (!this.existedTicksList.contains(pos)) {
            return true;
        } else {
            int ticks = this.existedTicksList.getTicksLeft(pos);
            return ticks <= 7;
        }
    }

    private boolean existedCheck(Entity entity) {
        BlockPos pos = entity.getBlockPos();
        return this.existedMode.get() == DelayMode.Seconds
                ? this.existedList.getEndTime(pos) - System.currentTimeMillis() < 250L
                : this.existedTicksList.getTicksLeft(pos) <= 5;
    }

    private boolean placeDelayCheck() {
        return this.placeDelayMode.get() == DelayMode.Seconds
                ? this.placeDelayList.getEndTime(this.placePos) - System.currentTimeMillis() < 250L
                : this.placeDelayTicksList.getTicksLeft(this.placePos) <= 5;
    }

    private void addExisted(BlockPos pos) {
        if (this.existedMode.get() == DelayMode.Seconds) {
            if (this.existed.get() > 0.0 && !this.existedList.contains(pos)) {
                this.existedList.add(pos, this.existed.get() + 0.25);
            }
        } else if (this.existedTicks.get() > 0 && !this.existedTicksList.contains(pos)) {
            this.existedTicksList.add(pos, this.existedTicks.get() + 5);
        }
    }

    private void addPlaceDelay(BlockPos pos) {
        if (this.placeDelayMode.get() == DelayMode.Seconds) {
            if (this.placeDelay.get() > 0.0 && !this.placeDelayList.contains(pos)) {
                this.placeDelayList.add(pos, this.placeDelay.get() + 0.25);
            }
        } else if (this.placeDelayTicks.get() > 0 && !this.placeDelayTicksList.contains(pos)) {
            this.placeDelayTicksList.add(pos, this.placeDelayTicks.get() + 5);
        }
    }

    private void updatePlacing(boolean canPlace) {
        if (this.canPlace()) {
            this.placing = true;
            if (!SettingUtils.shouldRotate(RotationType.Interact)
                    || this.rotateBlock(this.placePos.down(), this.crystalDir, this.getPlaceVec(this.placePos), RotationType.Interact, "placing")
                    || this.shouldRaytraceBypass(this.placePos) && this.raytraceLeft >= 0
                    || !this.requireRotation.get()) {
                if (canPlace) {
                    if (this.speedCheck()) {
                        if (this.placeDelayCheck()) {
                            if (this.antiPopMode.get() == AntiPopMode.Pause) {
                                this.calcDamage(
                                        new Vec3d(this.placePos.getX() + 0.5, this.placePos.getY(), this.placePos.getZ() + 0.5), false
                                );
                                if (this.selfDamage * this.selfPop.get() > this.selfHealth) {
                                    return;
                                }

                                if (this.friendDamage * this.friendPop.get() > this.friendHealth) {
                                    return;
                                }
                            }

                            boolean switched = false;
                            if (this.switchMode.get() != ACSwitchMode.Gapple && this.crystalResult.wasFound()) {
                                switched = this.switchMode.get().swap(this.crystalResult.slot());
                            }

                            if (this.crystalHand == null && !switched) {
                                this.placing = false;
                            } else {
                                this.place(this.placePos.down(), this.crystalDir, this.crystalHand);
                                if (this.predictAttacks.get() > 0) {
                                    this.sendPredictions(
                                            new Vec3d(this.placePos.getX() + 0.5, this.placePos.getY(), this.placePos.getZ() + 0.5)
                                    );
                                }

                                if (switched) {
                                    this.switchMode.get().swapBack();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendPredictions(Vec3d pos) {
        for (int i = 0; i <= this.predictAttacks.get(); i++) {
            this.attack(this.sentId + this.idStart.get() + i * this.predictStep.get(), pos, true);
        }

        this.sentId++;
    }

    private boolean gappleSwitch(boolean canPlace) {
        FindResult gapResult = this.switchMode.get().find(OLEPOSSUtils::isGapple);
        Item mainHandItem = BlackOut.mc.player.getMainHandStack().getItem();
        Item offHandItem = BlackOut.mc.player.getOffHandStack().getItem();
        boolean holdingGapples = mainHandItem == Items.GOLDEN_APPLE || mainHandItem == Items.ENCHANTED_GOLDEN_APPLE;
        boolean holdingCrystals = mainHandItem == Items.END_CRYSTAL;
        boolean gapplesInOffhand = offHandItem == Items.GOLDEN_APPLE || offHandItem == Items.ENCHANTED_GOLDEN_APPLE;
        boolean crystalsInOffhand = offHandItem == Items.END_CRYSTAL;
        if (BlackOut.mc.options.useKey.isPressed() && gapResult.wasFound()) {
            if (!holdingGapples && holdingCrystals && !gapplesInOffhand) {
                return this.switchMode.get().swap(gapResult.slot());
            }
        } else if (this.crystalResult.wasFound() && holdingGapples && !holdingCrystals && canPlace && !crystalsInOffhand) {
            return !this.switchMode.get().swap(this.crystalResult.slot());
        }

        return !holdingCrystals && !crystalsInOffhand;
    }

    private Vec3d getPlaceVec(BlockPos pos) {
        double y = pos.getY() - 1 + this.placeHeight.get();
        double x = 0.0;
        double z = 0.0;
        if (this.moveOffset.get()) {
            x = MathHelper.clamp(BlackOut.mc.player.getVelocity().x, -0.5, 0.5);
            z = MathHelper.clamp(BlackOut.mc.player.getVelocity().z, -0.5, 0.5);
        }

        return this.horizontalOffsetVec(pos.getX() + 0.5 + x, y, pos.getZ() + 0.5 + z);
    }

    private Vec3d getAttackVec(Vec3d feet) {
        double y = feet.y + this.attackHeight.get();
        double x = 0.0;
        double z = 0.0;
        if (this.moveOffset.get()) {
            x = MathHelper.clamp(BlackOut.mc.player.getVelocity().x, -0.5, 0.5);
            z = MathHelper.clamp(BlackOut.mc.player.getVelocity().z, -0.5, 0.5);
        }

        return this.horizontalOffsetVec(feet.x + x, y, feet.z + z);
    }

    private Vec3d horizontalOffsetVec(double x, double y, double z) {
        double ox = MathHelper.clamp(this.movement.x, -0.5, 0.5);
        double oz = MathHelper.clamp(this.movement.z, -0.5, 0.5);
        return new Vec3d(x - ox, y, z - oz);
    }

    private boolean speedCheck() {
        switch (this.placeSpeedMode.get()) {
            case Sync:
                if (this.placeSpeedLimit.get() > 0.0 && System.currentTimeMillis() - this.lastPlace < 1000.0 / this.placeSpeedLimit.get()) {
                    return false;
                } else {
                    if (!this.shouldSlow() && !this.isBlocked(this.placePos)) {
                        return true;
                    }

                    return System.currentTimeMillis() - this.lastPlace > 1000.0 / this.getPlaceSpeed(this.constantPlaceSpeed.get());
                }
            case Normal:
                return System.currentTimeMillis() - this.lastPlace > 1000.0 / this.getPlaceSpeed(this.placeSpeed.get());
            default:
                return true;
        }
    }

    private double getPlaceSpeed(double normal) {
        return this.shouldSlow() ? this.slowSpeed.get() : normal;
    }

    private boolean shouldSlow() {
        if (this.ignoreSlow.get() && this.shouldFacePlace()) {
            return false;
        } else {
            this.calcDamage(new Vec3d(this.placePos.getX() + 0.5, this.placePos.getY(), this.placePos.getZ() + 0.5), false);
            return this.placePos != null && this.enemyDamage <= this.slowDamage.get() && this.enemyHealth > this.slowHealth.get();
        }
    }

    private void place(BlockPos pos, Direction dir, Hand hand) {
        this.shouldCalc = true;
        this.lastPlace = System.currentTimeMillis();
        this.spawning.add(OLEPOSSUtils.getCrystalBox(pos.up()), 0.5);
        this.earthRender.add(pos, this.fadeTime.get() + this.renderTime.get());
        this.renderProgress = this.fadeTime.get() + this.renderTime.get();
        this.lastWasAttack = false;
        if (this.existedCheckMode.get() == ExistedMode.Server) {
            this.addExisted(pos.up());
        }

        this.interactBlock(hand, pos.toCenterPos(), dir, pos);
        if (this.placeSwing.get()) {
            this.clientSwing(this.placeHand.get(), hand);
        }

        if (this.ahd.get() && this.almostColliding(pos.up())) {
            int t = this.ahdTries.get();
            if (this.hitBoxDesyncList.containsKey(pos)) {
                t = this.hitBoxDesyncList.get(pos) - 1;
            }

            this.hitBoxDesyncList.removeKey(pos);
            this.hitBoxDesyncList.add(pos, t, this.ahdTime.get());
        }

        if (this.debugPlace.get()) {
            this.debug("placed after", System.currentTimeMillis() - this.lastAttack + "ms");
        }

        if (this.renderMode.get() == RenderMode.Confirm) {
            this.renderPos = pos;
        }

        this.end("placing");
    }

    private void updatePos() {
        this.updateTargets();
        this.shouldCalc = false;
        Suicide suicideModule = Suicide.getInstance();
        this.suicide = suicideModule.enabled && suicideModule.useCA.get();
        Box rangeBox = Managers.EXTRAPOLATION.extrapolate(BlackOut.mc.player, this.rangeExt.get());
        if (rangeBox == null) {
            this.rangePos = BlackOut.mc.player.getEyePos();
        } else {
            this.rangePos = new Vec3d(
                    (rangeBox.minX + rangeBox.maxX) / 2.0, rangeBox.minY, (rangeBox.minZ + rangeBox.maxZ) / 2.0
            );
        }

        BlockPos newPos = this.getPlacePos(
                BlockPos.ofFloored(this.rangePos.add(0.0, BlackOut.mc.player.getEyeHeight(BlackOut.mc.player.getPose()), 0.0)),
                (int) Math.ceil(SettingUtils.maxInteractRange())
        );
        if (!Objects.equals(newPos, this.placePos)) {
            this.lastChange = System.currentTimeMillis();
        }

        this.placePos = newPos;
        this.lastCalc = System.currentTimeMillis();
    }

    private boolean almostColliding(BlockPos pos) {
        Box blockBox = BoxUtils.crystalSpawnBox(pos);

        for (PlayerEntity player : this.targets) {
            Box box = player.getBoundingBox().expand(0.02);
            if (box.intersects(blockBox)) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldCalc() {
        if (!this.rotationFriendly.get()) {
            return true;
        } else if (!SettingUtils.shouldRotate(RotationType.Interact)) {
            return true;
        } else if (System.currentTimeMillis() - this.lastCalc > 100L) {
            return true;
        } else if (this.placePos == null) {
            return true;
        } else if (!this.crystalBlock(this.placePos)) {
            return true;
        } else {
            Direction dir = SettingUtils.getPlaceOnDirection(this.placePos.down());
            if (dir == null) {
                return true;
            } else if (!this.inPlaceRange(this.placePos.down()) || !SettingUtils.inAttackRange(OLEPOSSUtils.getCrystalBox(this.placePos))) {
                return true;
            } else if (this.intersects(this.placePos)) {
                return true;
            } else {
                this.calcDamage(new Vec3d(this.placePos.getX() + 0.5, this.placePos.getY(), this.placePos.getZ() + 0.5), false);
                return !this.placeDamageCheck();
            }
        }
    }

    private BlockPos getPlacePos(BlockPos center, int rad) {
        if (!this.place.get()) {
            return null;
        } else {
            BlockPos bestPos = null;
            boolean bestPop = false;
            this.selfHealth = this.getHealth(BlackOut.mc.player);
            double highest = 0.0;

            for (int x = -rad; x <= rad; x++) {
                for (int y = -rad - 1; y <= rad - 1; y++) {
                    for (int z = -rad; z <= rad; z++) {
                        BlockPos pos = center.add(x, y, z);
                        if (this.crystalBlock(pos)) {
                            Direction dir = SettingUtils.getPlaceOnDirection(pos.down());
                            if (dir != null
                                    && (!this.ahd.get() || !this.hitBoxDesyncList.contains((p, timer) -> p.equals(pos.down()) && timer.value <= 0))
                                    && this.inPlaceRange(pos.down())
                                    && this.inAttackRangePlacing(pos)) {
                                this.calcDamage(new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), false);
                                if ((!bestPop || this.isPop) && this.placeDamageCheck()) {
                                    double value = this.getPlaceValue(pos);
                                    if (!(value + (this.raytraceBypass.get() ? this.raytraceBypassValue.get() : 0.0) <= highest)) {
                                        boolean shouldRaytrace = this.shouldRaytraceBypass(pos);
                                        if (shouldRaytrace) {
                                            value += this.raytraceBypassValue.get();
                                        }

                                        if (!shouldRaytrace && (!SettingUtils.placeTrace(pos.down()) || !SettingUtils.attackTrace(BoxUtils.crystalSpawnBox(pos)))
                                        ) {
                                            value += this.wallValue.get();
                                        }

                                        if (!(value <= highest) && (!shouldRaytrace || this.raytraceRotation(pos, false) != null) && !this.intersects(pos)) {
                                            highest = value;
                                            bestPos = pos;
                                            bestPop = this.isPop;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return bestPos;
        }
    }

    private boolean inAttackRangePlacing(BlockPos pos) {
        switch (this.rangeExtMode.get()) {
            case Semi:
                if (this.inAttackRangePlacing(OLEPOSSUtils.getCrystalBox(pos), null)) {
                    return true;
                }

                if (this.rangeExt.get() > 0 && this.inAttackRangePlacing(OLEPOSSUtils.getCrystalBox(pos), this.rangePos)) {
                    return true;
                }
                break;
            case Full:
                if (this.inAttackRangePlacing(OLEPOSSUtils.getCrystalBox(pos), this.rangePos)) {
                    return true;
                }
        }

        return false;
    }

    private boolean inAttackRangePlacing(Box box, Vec3d from) {
        return this.raytraceBypass.get() && SettingUtils.inAttackRangeNoTrace(box, from) || !this.raytraceBypass.get() && SettingUtils.inAttackRange(box, from);
    }

    private boolean inPlaceRange(BlockPos pos) {
        return !this.raytraceBypass.get() ? SettingUtils.inInteractRange(pos) : SettingUtils.inInteractRangeNoTrace(pos);
    }

    private boolean intersects(BlockPos pos) {
        Box box = new Box(
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1,
                pos.getY() + (SettingUtils.cc() ? 1 : 2),
                pos.getZ() + 1
        );
        if (!this.ignoreItems.get() && EntityUtils.intersectsWithSpawningItem(pos)) {
            return true;
        } else if (EntityUtils.intersects(
                box, entity -> this.validForIntersects(entity, pos), this.flexibleHitbox.get() && pos.equals(this.placePos) ? null : this.boxMap
        )) {
            return true;
        } else if (this.noCollide.get() && this.spawningCollide.get()) {
            for (TimerList.Timer<Box> timer : this.spawning.getTimers()) {
                Box b = timer.value;
                if (b.intersects(box) && !pos.equals(BlockPos.ofFloored(BoxUtils.feet(b)))) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private boolean validForIntersects(Entity entity, BlockPos pos) {
        if (this.ignoreItems.get() && entity instanceof ItemEntity) {
            return false;
        } else if (!this.ignoreExp.get() || !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ExperienceBottleEntity)) {
            return (!(entity instanceof EndCrystalEntity crystal) || !this.canAttack(crystal, pos)) && (!(entity instanceof PlayerEntity) || !entity.isSpectator());
        } else {
            return false;
        }
    }

    private boolean canAttack(Entity entity, BlockPos placingPos) {
        boolean placing = placingPos != null;
        Box box = entity.getBoundingBox();
        if (placing) {
            if (!this.inAttackRangePlacing(box, null)) {
                return false;
            }
        } else if (!SettingUtils.inAttackRange(box)) {
            return false;
        }

        if (this.onlyOwn.get() && !((IEndCrystalEntity) entity).blackout_Client$isOwn()) {
            return false;
        } else if (!placing && this.inhibit.get() && this.inhibitList.contains(timer -> timer.value[0] == entity.getId() && timer.value[1] <= 0)) {
            return false;
        } else if (this.inhibit.get()
                && this.fullInhibit.get()
                && this.inhibitCollide.get()
                && this.fullInhibitList.contains(timer -> timer.value[0] == entity.getId() && timer.value[1] <= 0)) {
            return false;
        } else if (!placing && !this.almostExistedCheck(entity)) {
            return false;
        } else if (!placing && this.damageWait.get() && this.shouldDamageWait(entity)) {
            return false;
        } else if (placing && this.shouldNoCollide(entity.getId()) && !entity.getBlockPos().equals(placingPos)) {
            return false;
        } else {
            this.calcDamage(BoxUtils.feet(box), true);
            return this.attackDamageCheck(placing || this.placePos != null && box.intersects(BoxUtils.crystalSpawnBox(this.placePos)), placing);
        }
    }

    private boolean shouldNoCollide(int id) {
        if (!this.noCollide.get()) {
            return false;
        } else {
            return !this.attackCollide.get() || !this.attacked.contains(id);
        }
    }

    private boolean placeDamageCheck() {
        if (this.antiPopMode.get() == AntiPopMode.Change) {
            if (this.selfDamage * this.selfPop.get() > this.selfHealth) {
                return false;
            }

            if (this.friendDamage * this.friendPop.get() > this.friendHealth) {
                return false;
            }
        }

        if (this.enemyDamage * this.forcePop.get() > this.enemyHealth) {
            return true;
        } else {
            double minDmg = this.getMinDmg(this.minPlace);
            if (this.enemyDamage < minDmg) {
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
    }

    private boolean attackDamageCheck(boolean onlySelf, boolean placing) {
        if (placing && this.antiPopMode.get() == AntiPopMode.Pause) {
            if (this.selfDamage * this.selfPop.get() > this.selfHealth) {
                return false;
            }

            if (this.friendDamage > 0.0 && this.friendDamage * this.friendPop.get() > this.friendHealth) {
                return false;
            }
        }

        if (this.enemyDamage * this.forcePop.get() > this.enemyHealth) {
            return true;
        } else {
            if (!onlySelf) {
                double minDmg = this.getMinDmg(this.minAttack);
                if (this.checkEnemyAttack.get() && this.enemyDamage < minDmg) {
                    return false;
                }
            }

            if (this.checkSelfAttack.get()) {
                if (this.selfDamage > this.maxSelfAttack.get()) {
                    return false;
                }

                if (!onlySelf && this.checkEnemyAttack.get() && this.enemyDamage / this.selfDamage < this.minSelfAttackRatio.get()) {
                    return false;
                }
            }

            if (this.checkFriendAttack.get()) {
                return !(this.friendDamage > this.maxFriendAttack.get()) && (!onlySelf || !(this.friendDamage > 0.0) || !(this.enemyDamage / this.friendDamage < this.minFriendAttackRatio.get()));
            } else {
                return true;
            }
        }
    }

    private double getMinDmg(Setting<Double> normal) {
        return this.shouldFacePlace() ? this.facePlaceDamage.get() : normal.get();
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

    private double getAttackValue(EndCrystalEntity crystal) {
        double value = 0.0;
        Vec3d feet = crystal.getPos();
        if (SettingUtils.shouldRotate(RotationType.Attacking)) {
            value += this.rotationMod(feet);
        }

        if (crystal.getBlockPos().equals(this.placePos) && System.currentTimeMillis() - this.lastChange < this.antiJitterTime.get() * 1000.0) {
            value += this.antiJitter.get();
        }

        BlockPos collidePos = this.autoMineIgnore();
        if (collidePos != null && crystal.getBoundingBox().intersects(BoxUtils.get(collidePos))) {
            value += this.autoMineCollideValue.get();
        }

        value += this.moveMod(feet);
        value += this.enemyMod();
        value += this.selfMod();
        value += this.friendMod();
        value += this.distMod(SettingUtils.attackRangeTo(crystal.getBoundingBox(), feet));
        if (SettingUtils.shouldIgnoreRotations(crystal)) {
            value -= this.noRotateValue.get();
        }

        return value;
    }

    private double getPlaceValue(BlockPos pos) {
        double value = 0.0;
        Vec3d middle = pos.toCenterPos();
        if (SettingUtils.shouldRotate(RotationType.Interact)) {
            value += this.rotationMod(middle);
        }

        if (pos.equals(this.placePos) && System.currentTimeMillis() - this.lastChange < this.antiJitterTime.get() * 1000.0) {
            value += this.antiJitter.get();
        }

        BlockPos collidePos = this.autoMineIgnore();
        if (collidePos != null && BoxUtils.get(pos).intersects(BoxUtils.get(collidePos))) {
            value += this.autoMineCollideValue.get();
        }

        if (!this.valueBoxes.isEmpty()) {
            Box boxAt = BoxUtils.crystalSpawnBox(pos);

            for (Box box : this.valueBoxes) {
                if (box.intersects(boxAt)) {
                    value += this.hitboxValue.get();
                    break;
                }
            }
        }

        value += this.moveMod(new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5));
        value += this.enemyMod();
        value += this.selfMod();
        value += this.friendMod();
        return value + this.distMod(SettingUtils.placeRangeTo(pos));
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

    private double distMod(double range) {
        return Math.max(range - this.rangeStartDist.get(), 0.0) * -this.rangeValue.get() * this.moveModifier;
    }

    private double rotationMod(Vec3d pos) {
        double yawStep = 45.0;
        double pitchStep = 22.0;
        int yawSteps = (int) Math.ceil(Math.abs(RotationUtils.yawAngle(Managers.ROTATION.prevYaw, RotationUtils.getYaw(pos)) / yawStep));
        int pitchSteps = (int) Math.ceil(Math.abs(RotationUtils.pitchAngle(Managers.ROTATION.prevPitch, RotationUtils.getPitch(pos)) / pitchStep));
        int steps = Math.max(yawSteps, pitchSteps);
        return (3 - Math.min(steps, 3)) * this.rotationValue.get() / 3.0;
    }

    private double moveMod(Vec3d vec) {
        double val = 0.0;

        for (Entry<PlayerEntity, Float> entry : this.moveDirs.entrySet()) {
            PlayerEntity player = entry.getKey();
            double steps = Math.abs(RotationUtils.yawAngle(entry.getValue(), RotationUtils.getYaw(player.getPos(), vec, 0.0))) / 10.0;
            double valueMulti;
            if (!this.suicide && player == BlackOut.mc.player) {
                valueMulti = this.selfMoveValue.get();
            } else if (Managers.FRIENDS.isFriend(player)) {
                valueMulti = this.friendMoveValue.get();
            } else {
                valueMulti = this.moveValue.get();
            }

            double v = Math.max(3.0 - steps, 0.0);
            v *= valueMulti;
            v *= 1.0 - MathHelper.clamp(Math.abs(vec.getY() - player.getY()) - 1.0, 0.0, 1.0);
            val += v;
        }

        return val;
    }

    public boolean shouldAutoMineStop(Entity entity) {
        AutoMine autoMine = AutoMine.getInstance();
        if (autoMine.enabled
                && autoMine.started
                && this.autoMineAttack.get()
                && !(autoMine.getCurrentProgress() < this.autoMineAttackProgress.get())
                && autoMine.minePos != null
                && OLEPOSSUtils.solid2(autoMine.minePos)) {
            return switch (autoMine.mineType) {
                case Cev, TrapCev, SurroundCev ->
                        BlockPos.ofFloored(entity.getPos().add(0.0, -0.3, 0.0)).equals(autoMine.minePos);
                case SurroundMiner, AutoCity, AntiBurrow, Manual ->
                        BoxUtils.get(autoMine.minePos).intersects(entity.getBoundingBox());
                default -> false;
            };
        } else {
            return false;
        }
    }

    public void calcDamage(Vec3d vec, boolean attacking) {
        this.selfDamage = this.crystalDamage(
                BlackOut.mc.player, attacking ? BlackOut.mc.player.getBoundingBox() : this.extMap.get(BlackOut.mc.player), vec
        );
        this.enemyDamage = 0.0;
        this.friendDamage = 0.0;
        this.isPop = false;
        this.enemyHealth = 20.0;
        this.friendHealth = 20.0;
        this.target = null;
        if (this.suicide) {
            this.enemyDamage = this.selfDamage;
            this.selfDamage = 0.0;
            this.target = BlackOut.mc.player;
        } else {
            this.extMap.forEach((entity, box) -> {
                if (entity instanceof PlayerEntity player) {
                    if (!(player.getHealth() <= 0.0F) && player != BlackOut.mc.player) {
                        double dmg = this.crystalDamage(player, box, vec);
                        double health = this.getHealth(player);
                        boolean wouldPop = dmg * this.forcePop.get() > health;
                        if (Managers.FRIENDS.isFriend(player)) {
                            if (dmg > this.friendDamage) {
                                this.friendDamage = dmg;
                                this.friendHealth = health;
                            }
                        } else if (!this.isPop || wouldPop || !(this.forcePop.get() > 0.0)) {
                            if (wouldPop && !this.isPop && this.forcePop.get() > 0.0 || dmg > this.enemyDamage) {
                                this.enemyDamage = dmg;
                                this.enemyHealth = health;
                                this.target = player;
                                this.isPop = wouldPop;
                            }
                        }
                    }
                }
            });
        }
    }

    private double crystalDamage(PlayerEntity player, Box box, Vec3d vec) {
        return DamageUtils.crystalDamage(player, box, vec, this.autoMineIgnore());
    }

    private BlockPos autoMineIgnore() {
        AutoMine autoMine = AutoMine.getInstance();
        return autoMine.enabled && autoMine.started && autoMine.getCurrentProgress() >= this.prePlaceProgress.get() ? autoMine.minePos : null;
    }

    private double getHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private boolean crystalBlock(BlockPos pos) {
        Block block = this.getState(pos.down()).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
            return false;
        } else {
            return this.air(pos) && (!SettingUtils.oldCrystals() || this.air(pos.up()));
        }
    }

    private boolean air(BlockPos pos) {
        return this.getState(pos).getBlock() instanceof AirBlock;
    }

    private BlockState getState(BlockPos pos) {
        return Managers.BLOCK.blockState(pos);
    }

    private boolean shouldRaytraceBypass(BlockPos pos) {
        if (!this.raytraceBypass.get()) {
            return false;
        } else {
            return pos != null && !SettingUtils.interactTrace(pos.down()) && SettingUtils.placeRangeTo(pos) < SettingUtils.getAttackWallsRange();
        }
    }

    private Rotation raytraceRotation(BlockPos pos, boolean getBest) {
        Direction placeDir = SettingUtils.getPlaceOnDirection(pos.down());
        if (placeDir == null) {
            return null;
        } else {
            Vec3d vec = SettingUtils.getRotationVec(pos.down(), placeDir, this.getPlaceVec(pos), RotationType.Interact);
            Rotation rotation = SettingUtils.getRotation(vec);
            double minDist = BlackOut.mc.player.getEyePos().squaredDistanceTo(vec);
            ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, BlackOut.mc.player);
            float bestDist = 69420.0F;
            float bestPitch = -420.0F;
            boolean prevWas = false;

            for (float p = 90.0F; p >= -90.0F; p -= 10.0F) {
                float dist = Math.abs(rotation.pitch() - p);
                if (!(dist < this.raytraceAngle.get()) && !(dist > bestDist)) {
                    Vec3d pitchPos = RotationUtils.rotationVec(rotation.yaw(), p, BlackOut.mc.player.getEyePos(), 10.0);
                    ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(BlackOut.mc.player.getEyePos(), pitchPos);
                    BlockHitResult result = DamageUtils.raycast(DamageUtils.raycastContext, false);
                    boolean isHigher = BlackOut.mc.player.getEyePos().squaredDistanceTo(result.getPos()) > minDist;
                    if (isHigher && prevWas) {
                        if (!getBest) {
                            return new Rotation(rotation.yaw(), p);
                        }

                        bestDist = dist;
                        bestPitch = p;
                    }

                    prevWas = isHigher;
                }
            }

            return bestPitch == -420.0F ? null : new Rotation(rotation.yaw(), bestPitch);
        }
    }

    public enum ACSwitchMode {
        Disabled(false, false),
        Normal(true, false),
        Gapple(true, false),
        Silent(true, false),
        InvSwitch(true, true),
        PickSilent(true, true);

        public final boolean hotbar;
        public final boolean inventory;

        ACSwitchMode(boolean h, boolean i) {
            this.hotbar = h;
            this.inventory = i;
        }

        public void swapBack() {
            switch (this) {
                case Silent:
                    InvUtils.swapBack();
                    break;
                case InvSwitch:
                    InvUtils.invSwapBack();
                    break;
                case PickSilent:
                    InvUtils.pickSwapBack();
            }
        }

        public boolean swap(int slot) {
            return switch (this) {
                case Silent, Normal, Gapple -> {
                    InvUtils.swap(slot);
                    yield true;
                }
                case InvSwitch -> {
                    InvUtils.invSwap(slot);
                    yield true;
                }
                case PickSilent -> {
                    InvUtils.pickSwap(slot);
                    yield true;
                }
                default -> false;
            };
        }

        public FindResult find(Predicate<ItemStack> predicate) {
            return InvUtils.find(this.hotbar, this.inventory, predicate);
        }

        public FindResult find(Item item) {
            return InvUtils.find(this.hotbar, this.inventory, item);
        }
    }

    public enum ActionSpeedMode {
        Sync,
        Normal
    }

    public enum AnimationMode {
        Full,
        Up,
        Down,
        None
    }

    public enum AntiPopMode {
        Pause,
        Change
    }

    public enum AsyncMode {
        Disabled,
        Basic,
        Dumb,
        Heavy
    }

    public enum DelayMode {
        Seconds,
        Ticks
    }

    public enum ExistedMode {
        Client,
        Server
    }

    public enum RangeExtMode {
        Semi,
        Full
    }

    public enum RenderMode {
        Simple,
        Confirm,
        BlackOut,
        Earthhack
    }

    public enum SetDeadMode {
        Disabled,
        Render,
        Full
    }
}
