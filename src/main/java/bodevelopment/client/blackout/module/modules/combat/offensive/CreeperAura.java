package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
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
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ExtrapolationMap;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.AirBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

// TODO: NEED PATCHES
// TODO: добавить проверку наличия spawn egg до расчётов/рендера.
// TODO: ограничить частоту calc() и размер области перебора (tick budget).
// TODO: добавить cooldown на повторное place при отказе rotateBlock.
// TODO: пересмотреть placeDamageCheck и учёт friend/self pop для edge cases.
// TODO: добавить контроль античита: рандомизация delay и лимит пакетов.
// TODO: синхронизировать target selection с extrapolationMap (избегать stale target).
// TODO: добавить защиту от багов при смерти цели/выходе из мира.
// TODO: интегрировать Suicide/anti-self logic так, чтобы не терять татем по ошибке.
// TODO: улучшить render fade/reset при потере placePos.
// TODO: обработать случаи, когда creeper spawn заблокирован регионом/правилами сервера.
@OnlyDev
public class CreeperAura extends Module {
    private static CreeperAura INSTANCE;

    private final SettingGroup sgPlace = this.addGroup("Place");
    private final SettingGroup sgSlow = this.addGroup("Slow");
    private final SettingGroup sgFacePlace = this.addGroup("Face Place");
    private final SettingGroup sgDamage = this.addGroup("Damage");
    private final SettingGroup sgExtrapolation = this.addGroup("Extrapolation");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final SettingGroup sgCalculation = this.addGroup("Calculations");

    private final Setting<Boolean> place = this.sgPlace.booleanSetting("Placement", true, "Enables automatic placement of Creeper Spawn Eggs.");
    private final Setting<Boolean> pauseEat = this.sgPlace.booleanSetting("Pause on Consume", false, "Suspends placement operations while the player is eating or drinking.");
    private final Setting<Double> placeSpeed = this.sgPlace.doubleSetting("Placement Speed", 20.0, 0.0, 20.0, 0.1, "The maximum number of creepers to spawn per second.");
    private final Setting<SwitchMode> switchMode = this.sgPlace.enumSetting("Switch Mode", SwitchMode.Silent, "The method used to switch to spawn eggs in the hotbar.");

    private final Setting<Double> slowDamage = this.sgSlow.doubleSetting("Throttling Threshold", 3.0, 0.0, 20.0, 0.1, "Reduces placement speed when target damage is below this value.");
    private final Setting<Double> slowSpeed = this.sgSlow.doubleSetting("Throttled Speed", 2.0, 0.0, 20.0, 0.1, "Placement speed applied when the throttling threshold is active.");
    private final Setting<Double> slowHealth = this.sgSlow.doubleSetting("Health Safety Margin", 10.0, 0.0, 20.0, 0.5, "Only applies throttled speed if the target's health is above this value.");

    private final Setting<KeyBind> holdFacePlace = this.sgFacePlace.keySetting("Face-Place Hotkey", "Forces placement at the target's head while this key is held.");
    private final Setting<Double> facePlaceHealth = this.sgFacePlace.doubleSetting("Face-Place Health", 0.0, 0.0, 10.0, 0.1, "Automatically face-places if the target's health falls below this value.");
    private final Setting<Double> armorFacePlace = this.sgFacePlace.doubleSetting("Armor Durability Trigger", 10.0, 0.0, 100.0, 1.0, "Face-places if any of the target's armor pieces fall below this durability percentage.");
    private final Setting<Double> facePlaceDamage = this.sgFacePlace.doubleSetting("Face-Place Min Damage", 0.0, 0.0, 10.0, 0.1, "The minimum required damage for placement during face-place operations.");
    private final Setting<Boolean> ignoreSlow = this.sgFacePlace.booleanSetting("Disable Throttling", true, "Ignores 'Slow' speed settings while face-placing.");

    private final Setting<Double> minPlace = this.sgDamage.doubleSetting("Min Target Damage", 5.0, 0.0, 20.0, 0.1, "Minimum damage the target must receive for placement to occur.");
    private final Setting<Boolean> checkSelfPlacing = this.sgDamage.booleanSetting("Self-Damage Safety", true, "Calculates potential self-harm before spawning a creeper.");
    private final Setting<Double> maxSelfPlace = this.sgDamage.doubleSetting("Max Self-Damage", 10.0, 0.0, 20.0, 0.1, "Maximum allowed damage to the player per placement.", this.checkSelfPlacing::get);
    private final Setting<Double> minSelfRatio = this.sgDamage.doubleSetting("Efficiency Ratio", 2.0, 0.0, 20.0, 0.1, "Required damage ratio (Target Damage / Self Damage) for placement.", this.checkSelfPlacing::get);
    private final Setting<Boolean> checkFriendPlacing = this.sgDamage.booleanSetting("Friendly Fire Safety", true, "Prevents placements that would significantly harm friends.");
    private final Setting<Double> maxFriendPlace = this.sgDamage.doubleSetting("Max Friend Damage", 12.0, 0.0, 20.0, 0.1, "Maximum allowed damage to friendly players.", this.checkFriendPlacing::get);
    private final Setting<Double> minFriendRatio = this.sgDamage.doubleSetting("Friend Efficiency Ratio", 1.0, 0.0, 20.0, 0.1, "Required damage ratio (Target / Friend) for placement.", this.checkFriendPlacing::get);
    private final Setting<Double> forcePop = this.sgDamage.doubleSetting("Lethal Threshold", 0.0, 0.0, 5.0, 0.25, "Ignores damage constraints if the explosion is guaranteed to pop a Totem.");
    private final Setting<Double> selfPop = this.sgDamage.doubleSetting("Totem Preservation", 1.0, 0.0, 5.0, 0.25, "Restricts placement if it risks popping the player's own Totem.");
    private final Setting<Double> friendPop = this.sgDamage.doubleSetting("Friend Totem Preservation", 0.0, 0.0, 5.0, 0.25, "Restricts placement if it risks popping a friend's Totem.");

    private final Setting<Integer> extrapolation = this.sgExtrapolation.intSetting("Target Prediction", 0, 0, 20, 1, "Predicts target movement in ticks for damage calculations.");
    private final Setting<Integer> selfExt = this.sgExtrapolation.intSetting("Self Prediction", 0, 0, 20, 1, "Predicts player movement in ticks for self-damage calculations.");

    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Placement Animation", false, "Visualizes the arm swing during placement.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Placement Arm", SwingHand.RealHand, "The arm used for placement animations.");
    private final Setting<Boolean> render = this.sgRender.booleanSetting("Placement Highlight", true, "Renders a visual box at the placement site.");
    private final Setting<Double> renderTime = this.sgRender.doubleSetting("Highlight Duration", 0.3, 0.0, 10.0, 0.1, "Duration the box remains at full opacity.", this.render::get);
    private final Setting<Double> fadeTime = this.sgRender.doubleSetting("Highlight Fade", 1.0, 0.0, 10.0, 0.1, "Duration of the opacity decay animation.", this.render::get);
    private final Setting<RenderShape> renderShape = this.sgRender.enumSetting("Highlight Geometry", RenderShape.Full, "The visual structure of the placement box.", this.render::get);
    private final Setting<BlackOutColor> lineColor = this.sgRender.colorSetting("Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the box edges.", this.render::get);
    private final Setting<BlackOutColor> sideColor = this.sgRender.colorSetting("Fill Color", new BlackOutColor(255, 0, 0, 50), "The color of the box faces.", this.render::get);

    private final Setting<Double> damageValue = this.sgCalculation.doubleSetting("Target Damage Weight", 1.0, -2.0, 2.0, 0.05, "Importance of target damage in placement logic.");
    private final Setting<Double> selfDmgValue = this.sgCalculation.doubleSetting("Self-Damage Weight", -1.0, -2.0, 2.0, 0.05, "Priority of self-preservation in placement logic.");
    private final Setting<Double> friendDmgValue = this.sgCalculation.doubleSetting("Friend Damage Weight", 0.0, -2.0, 2.0, 0.05, "Priority of friend safety in placement logic.");
    private final Setting<Double> rotationValue = this.sgCalculation.doubleSetting("Rotation Weight", 3.0, -5.0, 10.0, 0.1, "Penalty applied based on the rotation angle required.");
    private final Setting<Integer> maxTargets = this.sgCalculation.intSetting("Target Limit", 3, 1, 10, 1, "The maximum number of entities to evaluate simultaneously.");
    private final Setting<Double> enemyDistance = this.sgCalculation.doubleSetting("Scan Range", 10.0, 0.0, 100.0, 1.0, "The maximum distance to search for targets.");

    private final ExtrapolationMap extMap = new ExtrapolationMap();
    private final List<PlayerEntity> targets = new ArrayList<>();
    private BlockPos placePos = null;
    private double selfHealth = 0.0;
    private double enemyHealth = 0.0;
    private double friendHealth = 0.0;
    private double selfDamage = 0.0;
    private double enemyDamage = 0.0;
    private double friendDamage = 0.0;
    private LivingEntity target = null;
    private BlockPos calcBest = null;
    private double calcValue = 0.0;
    private int calcR = 0;
    private BlockPos calcMiddle = null;
    private int progress = 0;
    private long lastPlace = 0L;
    private boolean suicide = false;
    private boolean facePlacing = false;
    private BlockPos renderPos = null;
    private double renderProgress = 0.0;

    public CreeperAura() {
        super("Creeper Aura", "Spawns creepers using spawn eggs to deal massive explosive damage to nearby targets.", SubCategory.OFFENSIVE, true);
        INSTANCE = this;
    }

    public static CreeperAura getInstance() {
        return INSTANCE;
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
            this.updateRender();
            if (this.placePos != null && this.place.get() && !this.paused()) {
                this.renderPos = this.placePos;
                this.renderProgress = 0.0;
                this.updatePlace();
            } else {
                this.renderProgress = Math.min(this.renderProgress + event.frameTime, this.renderTime.get() + this.fadeTime.get());
            }
        }
    }

    private void updateRender() {
        if (this.render.get() && this.renderPos != null && this.renderProgress < this.renderTime.get() + this.fadeTime.get()) {
            this.render(this.renderPos, this.lineColor, this.sideColor, this.renderShape, this.getAlpha(this.renderProgress, this.renderTime, this.fadeTime));
        }
    }

    private double getAlpha(double time, Setting<Double> rt, Setting<Double> ft) {
        return 1.0 - Math.max(time - rt.get(), 0.0) / ft.get();
    }

    private void render(BlockPos feetPos, Setting<BlackOutColor> lines, Setting<BlackOutColor> sides, Setting<RenderShape> shape, double alpha) {
        Box box = this.getBoxAt(feetPos);
        Render3DUtils.box(box, sides.get().alphaMulti(alpha), lines.get().alphaMulti(alpha), shape.get());
    }

    private Box getBoxAt(BlockPos pos) {
        return new Box(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.55, pos.getZ() + 1
        );
    }

    private boolean paused() {
        return this.pauseEat.get() && BlackOut.mc.player.isUsingItem();
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
        this.calcDamage(this.placePos);
        if (this.ignoreSlow.get() && this.shouldFacePlace()) {
            return false;
        } else {
            return !(this.enemyHealth < this.slowHealth.get()) && this.enemyDamage <= this.slowDamage.get();
        }
    }

    private void place() {
        PlaceData data = SettingUtils.getPlaceData(this.placePos);
        if (data.valid()) {
            Hand hand = OLEPOSSUtils.getHand(Items.CREEPER_SPAWN_EGG);
            FindResult result = this.switchMode.get().find(Items.CREEPER_SPAWN_EGG);
            if (hand != null || result.wasFound()) {
                if (!SettingUtils.shouldRotate(RotationType.Interact)
                        || this.rotateBlock(data, data.pos().toCenterPos().offset(data.dir(), 0.5), RotationType.BlockPlace, "placing")) {
                    boolean switched = false;
                    if (hand != null || (switched = this.switchMode.get().swap(result.slot()))) {
                        this.interactBlock(hand, data.pos().toCenterPos(), data.dir(), data.pos());
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

    private void calc(float tickDelta) {
        if (this.calcMiddle != null) {
            int d = this.calcR * 2 + 1;
            int target = d * d * d;

            for (int i = this.progress; i < target * tickDelta; i++) {
                this.progress = i;
                int x = i % d - this.calcR;
                int y = i / d % d - this.calcR;
                int z = i / d / d % d - this.calcR;
                this.calcPos(this.calcMiddle.add(x, y, z));
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
        if (BlackOut.mc.world.getBlockState(pos).getBlock() instanceof AirBlock) {
            if (this.inRangeToEnemies(pos)) {
                PlaceData data = SettingUtils.getPlaceData(pos);
                if (data.valid()) {
                    if (SettingUtils.inInteractRange(data.pos())) {
                        this.calcDamage(pos);
                        if (this.placeDamageCheck()) {
                            double value = this.getValue(pos, true);
                            if (!(value <= this.calcValue)) {
                                this.calcBest = pos;
                                this.calcValue = value;
                            }
                        }
                    }
                }
            }
        }
    }

    private void updatePos() {
        Suicide suicideModule = Suicide.getInstance();
        this.suicide = suicideModule.enabled && suicideModule.useCreeper.get();
        this.findTargets();
        this.extMap.update(player -> player == BlackOut.mc.player ? this.selfExt.get() : this.extrapolation.get());
        this.placePos = this.calcBest;
        this.startCalc();
    }

    private void startCalc() {
        this.selfHealth = this.getHealth(BlackOut.mc.player);
        this.calcBest = null;
        this.calcValue = -42069.0;
        this.progress = 0;
        this.calcR = (int) Math.ceil(SettingUtils.maxInteractRange());
        this.calcMiddle = BlockPos.ofFloored(BlackOut.mc.player.getEyePos());
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
        this.selfDamage = DamageUtils.creeperDamage(BlackOut.mc.player, this.extMap.get(BlackOut.mc.player), vec, pos);
        this.enemyDamage = 0.0;
        this.friendDamage = 0.0;
        if (this.suicide) {
            this.enemyDamage = this.selfDamage;
            this.selfDamage = 0.0;
            this.friendDamage = 0.0;
            this.enemyHealth = 20.0;
            this.friendHealth = 36.0;
        } else {
            this.enemyHealth = 20.0;
            this.friendHealth = 20.0;
            this.targets.forEach(player -> {
                Box box = this.extMap.get(player);
                if (!(player.getHealth() <= 0.0F) && player != BlackOut.mc.player) {
                    double dmg = DamageUtils.creeperDamage(player, box, vec, pos);
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
        }
    }

    private double getHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }
}
