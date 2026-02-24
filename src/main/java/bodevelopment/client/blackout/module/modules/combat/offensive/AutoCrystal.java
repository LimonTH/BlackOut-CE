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

public class AutoCrystal extends Module {
    private static AutoCrystal INSTANCE;
    private final SettingGroup sgPlace = this.addGroup("Place");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgIdPredict = this.addGroup("ID Predict", "Very unstable. Might kick or just not work");
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

    public final Setting<Boolean> raytraceBypass = this.sgRaytraceBypass.booleanSetting("Raytrace Bypass", false, ".");
    public final Setting<Integer> raytraceDelay = this.sgRaytraceBypass.intSetting("Raytrace Delay", 10, 0, 100, 1, ".", this.raytraceBypass::get);
    public final Setting<Integer> raytraceTime = this.sgRaytraceBypass.intSetting("Raytrace Time", 15, 0, 100, 1, ".", this.raytraceBypass::get);
    public final Setting<Integer> raytraceAngle = this.sgRaytraceBypass.intSetting("Raytrace Min Angle", 45, 0, 100, 1, ".", this.raytraceBypass::get);
    private final Setting<Double> raytraceBypassValue = this.sgCalculation.doubleSetting("Raytrace Bypass Value", -4.0, -5.0, 5.0, 0.1, ".", this.raytraceBypass::get);
    private final Setting<Boolean> place = this.sgPlace.booleanSetting("Place", true, "Places crystals.");
    private final Setting<Boolean> pauseEatPlace = this.sgPlace.booleanSetting("Pause Eat Place", false, "Pauses placing while eating.");
    private final Setting<ActionSpeedMode> placeSpeedMode = this.sgPlace.enumSetting("Place Speed Mode", ActionSpeedMode.Sync, ".");
    private final Setting<Double> placeSpeedLimit = this.sgPlace
            .doubleSetting(
                    "Place Speed Limit",
                    0.0,
                    0.0,
                    20.0,
                    0.1,
                    "Maximum amount of places every second. 0 = no limit",
                    () -> this.placeSpeedMode.get() == ActionSpeedMode.Sync
            );
    private final Setting<Double> constantPlaceSpeed = this.sgPlace
            .doubleSetting("Constant Place Speed", 10.0, 0.0, 20.0, 0.1, ".", () -> this.placeSpeedMode.get() == ActionSpeedMode.Sync);
    private final Setting<Double> placeSpeed = this.sgPlace
            .doubleSetting("Place Speed", 20.0, 0.0, 20.0, 0.1, ".", () -> this.placeSpeedMode.get() == ActionSpeedMode.Normal);
    private final Setting<DelayMode> placeDelayMode = this.sgPlace.enumSetting("Place Delay Mode", DelayMode.Ticks, ".");
    private final Setting<Double> placeDelay = this.sgPlace
            .doubleSetting("Place Delay", 0.0, 0.0, 1.0, 0.01, ".", () -> this.placeDelayMode.get() == DelayMode.Seconds);
    private final Setting<Integer> placeDelayTicks = this.sgPlace
            .intSetting("Place Delay Ticks", 0, 0, 20, 1, ".", () -> this.placeDelayMode.get() == DelayMode.Ticks);
    private final Setting<Boolean> ahd = this.sgPlace.booleanSetting("AHD", true, "");
    private final Setting<Integer> ahdTries = this.sgPlace.intSetting("AHD Tries", 3, 0, 20, 1, "", this.ahd::get);
    private final Setting<Integer> ahdTime = this.sgPlace.intSetting("AHD Time", 20, 0, 100, 1, "", this.ahd::get);
    private final Setting<Boolean> ignoreItems = this.sgPlace.booleanSetting("Ignore Items", true, "");
    private final Setting<Boolean> ignoreExp = this.sgPlace.booleanSetting("Ignore Exp", true, "");
    private final Setting<Boolean> requireRotation = this.sgPlace
            .booleanSetting("Require Rotation", true, "Places crystals.", () -> SettingUtils.shouldRotate(RotationType.Interact));
    private final Setting<Boolean> attack = this.sgAttack.booleanSetting("Attack", true, "Attacks crystals.");
    private final Setting<Integer> attackPackets = this.sgAttack
            .intSetting("Attack Packets", 1, 0, 5, 1, "Sends this many attack packets each hit. Probably useless but u could test some stuff.");
    private final Setting<Boolean> pauseEatAttack = this.sgAttack.booleanSetting("Pause Eat Attack", false, "Pauses attacking while eating.");
    private final Setting<Boolean> onlyOwn = this.sgAttack.booleanSetting("Only Own", false, "Only attacks crystals placed by you.");
    private final Setting<Boolean> antiWeakness = this.sgAttack.booleanSetting("Anti Weakness", true, ".");
    private final Setting<ExistedMode> existedCheckMode = this.sgAttack.enumSetting("Existed Check Mode", ExistedMode.Client, ".");
    private final Setting<DelayMode> existedMode = this.sgAttack
            .enumSetting("Existed Mode", DelayMode.Ticks, "Should crystal existed times be counted in seconds or ticks.");
    private final Setting<Double> existed = this.sgAttack
            .doubleSetting(
                    "Explode Delay",
                    0.0,
                    0.0,
                    1.0,
                    0.01,
                    "How many seconds should the crystal exist before attacking.",
                    () -> this.existedMode.get() == DelayMode.Seconds
            );
    private final Setting<Integer> existedTicks = this.sgAttack
            .intSetting(
                    "Explode Delay Ticks",
                    0,
                    0,
                    20,
                    1,
                    "How many ticks should the crystal exist before attacking.",
                    () -> this.existedMode.get() == DelayMode.Ticks
            );
    private final Setting<ActionSpeedMode> attackSpeedMode = this.sgAttack.enumSetting("Attack Speed Mode", ActionSpeedMode.Sync, ".");
    private final Setting<Double> attackSpeedLimit = this.sgAttack
            .doubleSetting(
                    "Attack Speed Limit",
                    0.0,
                    0.0,
                    20.0,
                    0.1,
                    "Maximum amount of attacks every second. 0 = no limit",
                    () -> this.attackSpeedMode.get() == ActionSpeedMode.Sync
            );
    private final Setting<Double> constantAttackSpeed = this.sgAttack
            .doubleSetting("Constant Attack Speed", 10.0, 0.0, 20.0, 0.1, ".", () -> this.attackSpeedMode.get() == ActionSpeedMode.Sync);
    private final Setting<Double> attackSpeed = this.sgAttack
            .doubleSetting("Attack Speed", 20.0, 0.0, 20.0, 0.1, ".", () -> this.attackSpeedMode.get() == ActionSpeedMode.Normal);
    private final Setting<SetDeadMode> setDead = this.sgAttack
            .enumSetting("Set Dead", SetDeadMode.Disabled, "Hides the crystal after hitting it. Not needed since the module already is smart enough.");
    private final Setting<Double> cpsTime = this.sgAttack.doubleSetting("Cps Time", 5.0, 1.0, 20.0, 0.1, "Average cps from past x seconds.");
    private final Setting<Integer> predictAttacks = this.sgIdPredict.intSetting("Predict Attacks", 0, 0, 10, 1, ".");
    private final Setting<Integer> idStart = this.sgIdPredict.intSetting("Id Start", 1, 1, 5, 1, ".");
    private final Setting<Integer> predictStep = this.sgIdPredict.intSetting("Predict Step", 1, 1, 5, 1, ".");
    private final Setting<Integer> predictFlexibility = this.sgIdPredict
            .intSetting("Predict Flexiblity", 2, 0, 10, 1, "Might wanna make high on higher ping and stable server.");
    private final Setting<Boolean> predictSwing = this.sgIdPredict.booleanSetting("Predict Swing", true, ".");
    private final Setting<Boolean> inhibit = this.sgInhibit.booleanSetting("Inhibit", true, "Ignores crystals after a certain amount of time or attacks.");
    private final Setting<Boolean> fullInhibit = this.sgInhibit
            .booleanSetting("Full Inhibit", true, "Ignores crystals after a certain amount of time or attacks.", this.inhibit::get);
    private final Setting<Integer> fullInhibitTicks = this.sgInhibit
            .intSetting("Full Inhibit Ticks", 100, 0, 400, 5, ".", () -> this.inhibit.get() && this.fullInhibit.get());
    private final Setting<Integer> fullInhibitAttacks = this.sgInhibit
            .intSetting("Full Inhibit Attacks", 2, 1, 10, 1, ".", () -> this.inhibit.get() && this.fullInhibit.get());
    private final Setting<Boolean> inhibitCollide = this.sgInhibit
            .booleanSetting("Inhibit Collide", false, "Doesn't allow place pos to collide with inhibit crystals.", () -> this.inhibit.get() && this.fullInhibit.get());
    private final Setting<Integer> inhibitTicks = this.sgInhibit.intSetting("Inhibit Ticks", 10, 0, 100, 1, ".");
    private final Setting<Integer> inhibitAttacks = this.sgInhibit.intSetting("Inhibit Attacks", 1, 1, 10, 1, ".");
    private final Setting<Double> slowDamage = this.sgSlow
            .doubleSetting("Slow Damage", 3.0, 0.0, 20.0, 0.1, "Switches to slow speed when the target would take under this amount of damage.");
    private final Setting<Double> slowSpeed = this.sgSlow
            .doubleSetting("Slow Speed", 2.0, 0.0, 20.0, 0.1, "How many times should the module place per second when damage is under slow damage.");
    private final Setting<Double> slowHealth = this.sgSlow.doubleSetting("Slow Health", 10.0, 0.0, 20.0, 0.5, "Only slow places if enemy has over x health.");
    private final Setting<KeyBind> holdFacePlace = this.sgFacePlace.keySetting("Hold Face Place", "Faceplaces when holding this key.");
    private final Setting<Double> facePlaceHealth = this.sgFacePlace
            .doubleSetting("Face Place Health", 0.0, 0.0, 10.0, 0.1, "Automatically face places if enemy has under this much health.");
    private final Setting<Double> armorFacePlace = this.sgFacePlace
            .doubleSetting("Armor Face Place", 10.0, 0.0, 100.0, 1.0, "Face places if enemy's any armor piece is under this durability.");
    private final Setting<Double> facePlaceDamage = this.sgFacePlace.doubleSetting("Face Place Damage", 0.0, 0.0, 10.0, 0.1, "Sets min place and min attack to this.");
    private final Setting<Boolean> ignoreSlow = this.sgFacePlace.booleanSetting("Ignore Slow", true, "Doesn't slow place when faceplacing.");
    private final Setting<Boolean> moveOffset = this.sgRotation.booleanSetting("Move Offset", true, ".");
    private final Setting<Double> placeHeight = this.sgRotation.doubleSetting("Place Height", 1.0, 0.0, 1.0, 0.01, "Height for place rotations.");
    private final Setting<Double> attackHeight = this.sgRotation.doubleSetting("Attack Height", 0.0, 0.0, 2.0, 0.01, "Height for attack rotations.");
    private final Setting<ACSwitchMode> switchMode = this.sgSwitch
            .enumSetting("Switch", ACSwitchMode.Disabled, "Mode for switching to crystal in main hand.");
    private final Setting<SwitchMode> antiWeaknessSwitch = this.sgSwitch.enumSetting("Anti-Weakness Switch", SwitchMode.Silent, ".");
    private final Setting<Double> placeSwitchPenalty = this.sgSwitch
            .doubleSetting("Place Switch Penalty", 0.0, 0.0, 1.0, 0.05, "Time to wait after switching before placing crystals.");
    private final Setting<Double> attackSwitchPenalty = this.sgSwitch
            .doubleSetting("Attack Switch Penalty", 0.0, 0.0, 1.0, 0.05, "Time to wait after switching before attacking crystals.");
    private final Setting<Double> minPlace = this.sgDamage.doubleSetting("Min Place", 5.0, 0.0, 20.0, 0.1, "Minimum damage to place.");
    private final Setting<Boolean> checkSelfPlacing = this.sgDamage.booleanSetting("Self Placing", true, "Checks self damage when placing.");
    private final Setting<Double> maxSelfPlace = this.sgDamage.doubleSetting("Max Place", 10.0, 0.0, 20.0, 0.1, "Max self damage for placing.", this.checkSelfPlacing::get);
    private final Setting<Double> minSelfRatio = this.sgDamage
            .doubleSetting("Min Place Ratio", 2.0, 0.0, 20.0, 0.1, "Min self damage ratio for placing (enemy / self).", this.checkSelfPlacing::get);
    private final Setting<Boolean> checkFriendPlacing = this.sgDamage.booleanSetting("Friend Placing", true, "Checks friend damage when placing.");
    private final Setting<Double> maxFriendPlace = this.sgDamage
            .doubleSetting("Max Friend Place", 12.0, 0.0, 20.0, 0.1, "Max friend damage for placing.", this.checkFriendPlacing::get);
    private final Setting<Double> minFriendRatio = this.sgDamage
            .doubleSetting("Min Friend Place Ratio", 1.0, 0.0, 20.0, 0.1, "Min friend damage ratio for placing (enemy / friend).", this.checkFriendPlacing::get);
    private final Setting<Boolean> checkEnemyAttack = this.sgDamage.booleanSetting("Enemy Attack", true, "Checks enemy damage when attacking.");
    private final Setting<Double> minAttack = this.sgDamage.doubleSetting("Min Attack", 5.0, 0.0, 20.0, 0.1, "Minimum damage to attack.", this.checkEnemyAttack::get);
    private final Setting<Boolean> checkSelfAttack = this.sgDamage.booleanSetting("Self Attack", true, "Checks self damage when attacking.");
    private final Setting<Double> maxSelfAttack = this.sgDamage
            .doubleSetting("Max Attack", 10.0, 0.0, 20.0, 0.1, "Max self damage for attacking.", this.checkSelfAttack::get);
    private final Setting<Double> minSelfAttackRatio = this.sgDamage
            .doubleSetting(
                    "Min Attack Ratio",
                    2.0,
                    0.0,
                    20.0,
                    0.1,
                    "Min self damage ratio for attacking (enemy / self).",
                    () -> this.checkSelfAttack.get() && this.checkEnemyAttack.get()
            );
    private final Setting<Boolean> checkFriendAttack = this.sgDamage.booleanSetting("Friend Attack", true, "Checks friend damage when attacking.");
    private final Setting<Double> maxFriendAttack = this.sgDamage
            .doubleSetting("Max Friend Attack", 12.0, 0.0, 20.0, 0.1, "Max friend damage for attacking.", this.checkFriendAttack::get);
    private final Setting<Double> minFriendAttackRatio = this.sgDamage
            .doubleSetting("Min Friend Attack Ratio", 1.0, 0.0, 20.0, 0.1, "Min friend damage ratio for attacking (enemy / friend).", this.checkFriendAttack::get);
    private final Setting<Double> forcePop = this.sgDamage.doubleSetting("Force Pop", 0.0, 0.0, 5.0, 0.25, "Ignores damage checks if any enemy will be popped in x hits.");
    private final Setting<Double> selfPop = this.sgDamage.doubleSetting("Anti Pop", 1.0, 0.0, 5.0, 0.25, "Ignores damage checks if any enemy will be popped in x hits.");
    private final Setting<Double> friendPop = this.sgDamage
            .doubleSetting("Anti Friend Pop", 0.0, 0.0, 5.0, 0.25, "Ignores damage checks if any enemy will be popped in x hits.");
    private final Setting<AntiPopMode> antiPopMode = this.sgDamage.enumSetting("Anti Pop Mode", AntiPopMode.Change, ".");
    private final Setting<Integer> extrapolation = this.sgExtrapolation
            .intSetting("Extrapolation", 0, 0, 20, 1, "How many ticks of movement should be predicted for enemy damage checks.");
    private final Setting<Integer> selfExt = this.sgExtrapolation
            .intSetting("Self Extrapolation", 0, 0, 20, 1, "How many ticks of movement should be predicted for self damage checks.");
    private final Setting<RangeExtMode> rangeExtMode = this.sgExtrapolation.enumSetting("Range Extrapolation Mode", RangeExtMode.Semi, ".");
    private final Setting<Integer> rangeExt = this.sgExtrapolation
            .intSetting("Range Extrapolation", 0, 0, 20, 1, "How many ticks of movement should be predicted for attack ranges before placing.");
    private final Setting<Double> hitboxExpand = this.sgExtrapolation.doubleSetting("Hitbox Expand", 1.0, 0.0, 2.0, 0.02, "");
    private final Setting<Boolean> flexibleHitbox = this.sgExtrapolation.booleanSetting("Flexible Hitbox", false, ".", () -> this.hitboxExpand.get() > 0.0);
    private final Setting<Boolean> extrapolateHitbox = this.sgExtrapolation.booleanSetting("Extrapolate Hitbox", false, "");
    private final Setting<Double> preferHitboxExpand = this.sgExtrapolation.doubleSetting("Value Hitbox Expand", 2.0, 0.0, 2.0, 0.02, "");
    private final Setting<Double> hitboxValue = this.sgExtrapolation.doubleSetting("Hitbox Value", -8.0, -10.0, 10.0, 0.2, ".");
    private final Setting<Boolean> damageWait = this.sgDamageWait.booleanSetting("Damage Wait", false, ".");
    private final Setting<Integer> waitStartExt = this.sgDamageWait.intSetting("Wait Start Extrapolation", 2, 0, 20, 1, ".");
    private final Setting<Integer> waitEndExt = this.sgDamageWait.intSetting("Wait End Extrapolation", 5, 0, 20, 1, ".");
    private final Setting<Double> minDifference = this.sgDamageWait.doubleSetting("Min Difference", 0.0, 0.0, 10.0, 0.1, ".");
    private final Setting<Integer> maxWait = this.sgDamageWait.intSetting("Max Wait", 3, 0, 20, 1, ".");
    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Place Swing", false, "Renders swing animation when placing a crystal.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Place Hand", SwingHand.RealHand, "Which hand should be swung.");
    private final Setting<Boolean> attackSwing = this.sgRender.booleanSetting("Attack Swing", false, "Renders swing animation when attacking a crystal.");
    private final Setting<SwingHand> attackHand = this.sgRender.enumSetting("Attack Hand", SwingHand.RealHand, "Which hand should be swung.");
    private final Setting<Boolean> render = this.sgRender.booleanSetting("Render", true, "Renders box on placement.");
    private final Setting<RenderMode> renderMode = this.sgRender
            .enumSetting("Render Mode", RenderMode.BlackOut, "What should the render look like.", this.render::get);
    private final Setting<Double> textScale = this.sgRender
            .doubleSetting(
                    "Text Size",
                    0.3,
                    0.0,
                    1.0,
                    0.01,
                    "Current size of rendering damage text.",
                    () -> this.renderMode.get() == RenderMode.Earthhack
                            || this.renderMode.get() == RenderMode.BlackOut
                            || this.renderMode.get() == RenderMode.Simple
                            || this.renderMode.get() == RenderMode.Confirm
            );
    private final Setting<Double> renderTime = this.sgRender
            .doubleSetting(
                    "Render Time",
                    0.3,
                    0.0,
                    10.0,
                    0.1,
                    "How long the box should remain in full alpha value.",
                    () -> this.renderMode.get() == RenderMode.Earthhack
                            || this.renderMode.get() == RenderMode.Simple
                            || this.renderMode.get() == RenderMode.Confirm
            );
    private final Setting<Double> fadeTime = this.sgRender
            .doubleSetting(
                    "Fade Time",
                    1.0,
                    0.0,
                    10.0,
                    0.1,
                    "How long the fading should take.",
                    () -> this.renderMode.get() == RenderMode.Earthhack
                            || this.renderMode.get() == RenderMode.Simple
                            || this.renderMode.get() == RenderMode.Confirm
            );
    private final Setting<Double> animMoveSpeed = this.sgRender
            .doubleSetting("Move Speed", 2.0, 0.0, 10.0, 0.1, "How fast should blackout mode box move.", () -> this.renderMode.get() == RenderMode.BlackOut);
    private final Setting<Double> animMoveExponent = this.sgRender
            .doubleSetting("Move Exponent", 3.0, 0.0, 10.0, 0.1, "Moves faster when longer away from the target.", () -> this.renderMode.get() == RenderMode.BlackOut);
    private final Setting<Double> animSizeExponent = this.sgRender
            .doubleSetting(
                    "Animation Size Exponent",
                    3.0,
                    0.0,
                    10.0,
                    0.1,
                    "How fast should blackout mode box grow.",
                    () -> this.renderMode.get() == RenderMode.BlackOut
            );
    private final Setting<AnimationMode> animationMode = this.sgRender.enumSetting("Animation Size Mode", AnimationMode.Full, ".");
    private final BoxMultiSetting renderSetting = BoxMultiSetting.of(this.sgRender, "Box");
    private final Setting<Boolean> renderDamage = this.sgRender.booleanSetting("Render Damage", true, ".");
    private final Setting<Boolean> renderExt = this.sgRender.booleanSetting("Render Extrapolation", false, "Renders boxes at players' predicted positions.");
    private final Setting<Boolean> renderBoxExt = this.sgRender.booleanSetting("Render Box Extrapolation", false, "Renders boxes at players' predicted positions.");
    private final Setting<Boolean> renderSelfExt = this.sgRender.booleanSetting("Render Self Extrapolation", false, "Renders box at your predicted position.");
    private final Setting<Double> damageValue = this.sgCalculation.doubleSetting("Damage Value", 1.0, -5.0, 5.0, 0.1, ".");
    private final Setting<Double> selfDmgValue = this.sgCalculation.doubleSetting("Self Damage Value", -1.0, -5.0, 5.0, 0.05, ".");
    private final Setting<Double> friendDmgValue = this.sgCalculation.doubleSetting("Friend Damage Value", 0.0, -5.0, 5.0, 0.05, ".");
    private final Setting<Double> moveValue = this.sgCalculation
            .doubleSetting("Move Dir Value", 0.0, -5.0, 5.0, 0.1, "Adds x value if enemy is moving towards the position.");
    private final Setting<Double> selfMoveValue = this.sgCalculation
            .doubleSetting("Self Move Value", 0.0, -5.0, 5.0, 0.1, "Adds x value if enemy is moving towards the position.");
    private final Setting<Double> friendMoveValue = this.sgCalculation.doubleSetting("Friend Move Value", 0.0, -5.0, 5.0, 0.1, ".");
    private final Setting<Double> rotationValue = this.sgCalculation.doubleSetting("Rotation Value", 0.0, -5.0, 5.0, 0.1, ".");
    private final Setting<Double> wallValue = this.sgCalculation.doubleSetting("Wall Value", 0.0, -5.0, 5.0, 0.1, ".");
    private final Setting<Double> noRotateValue = this.sgCalculation.doubleSetting("No Rotate Value", 0.0, -5.0, 5.0, 0.1, ".", SettingUtils::rotationIgnoreEnabled);
    private final Setting<Integer> maxTargets = this.sgCalculation.intSetting("Max Targets", 3, 1, 10, 1, ".");
    private final Setting<Boolean> noCollide = this.sgCalculation.booleanSetting("No Collide", false, "Doesn't place if any crystal is half inside the pos.");
    private final Setting<Boolean> spawningCollide = this.sgCalculation
            .booleanSetting("Spawning Collide", false, "Doesn't place if any spawning crystal is half inside the pos.", this.noCollide::get);
    private final Setting<Boolean> attackCollide = this.sgCalculation.booleanSetting("Attack Collide", false, ".", this.noCollide::get);
    private final Setting<Double> antiJitter = this.sgCalculation.doubleSetting("Anti Jitter", 0.5, 0.0, 5.0, 0.1, "Doesn't place if any crystal is half inside the pos.");
    private final Setting<Double> antiJitterTime = this.sgCalculation.doubleSetting("Anti Jitter Time", 0.2, 0.0, 1.0, 0.01, ".", () -> this.antiJitter.get() != 0.0);
    private final Setting<Double> autoMineCollideValue = this.sgCalculation.doubleSetting("Auto Mine Collide Value", 0.0, -5.0, 5.0, 0.1, ".");
    private final Setting<AsyncMode> async = this.sgCalculation.enumSetting("Async", AsyncMode.Basic, "");
    private final Setting<Boolean> rotationFriendly = this.sgCalculation.booleanSetting("Rotation Friendly", true, ".");
    private final Setting<Double> rangeValue = this.sgCalculation.doubleSetting("Range Value", 1.0, -5.0, 5.0, 0.1, ".");
    private final Setting<Double> rangeStartDist = this.sgCalculation.doubleSetting("Range Start Dist", 0.0, 0.0, 6.0, 0.1, ".", () -> this.rangeValue.get() != 0.0);
    private final Setting<Boolean> eco = this.sgCalculation.booleanSetting("Eco", false, ".");
    private final Setting<Double> prePlaceProgress = this.sgCompatibility.doubleSetting("Pre Place Progress", 0.9, 0.0, 1.0, 0.01, ".");
    private final Setting<Boolean> autoMineAttack = this.sgCompatibility.booleanSetting("Auto Mine Attack", true, ".");
    private final Setting<Double> autoMineAttackProgress = this.sgCompatibility
            .doubleSetting("Auto Mine Attack Progress", 0.75, 0.0, 1.0, 0.01, ".", this.autoMineAttack::get);
    private final Setting<Boolean> debugPlace = this.sgDebug.booleanSetting("Debug Place", false, ".");
    private final Setting<Boolean> debugAttack = this.sgDebug.booleanSetting("Debug Attack", false, ".");
    private final Setting<Boolean> removeTime = this.sgDebug.booleanSetting("Remove Time", false, ".");
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
        super("Auto Crystal", "Places and attacks crystals.", SubCategory.OFFENSIVE, true);
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
