package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ExtrapolationMap;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.*;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
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
import java.util.function.Predicate;

// TODO: NEED PATCHES
@OnlyDev
public class AnchorAura extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgDamage = this.addGroup("Damage");
    public final SettingGroup sgExtrapolation = this.addGroup("Extrapolation");
    public final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Double> enemyDistance = this.sgGeneral.doubleSetting("Target Distance", 10.0, 0.0, 100.0, 1.0, "The maximum distance to scan for potential targets.");
    private final Setting<Double> placeSpeed = this.sgGeneral.doubleSetting("Place Speed", 4.0, 0.0, 20.0, 0.1, "How many anchors to place per second.");
    private final Setting<Double> interactSpeed = this.sgGeneral.doubleSetting("Load Speed", 2.0, 0.0, 20.0, 0.1, "How many Glowstone Dusts to put into anchors per second.");
    private final Setting<Double> explodeSpeed = this.sgGeneral.doubleSetting("Explode Speed", 2.0, 0.0, 20.0, 0.1, "How many anchors to trigger per second.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "The method used to swap to anchors and glowstone.");

    private final Setting<Double> minPlace = this.sgDamage.doubleSetting("Min Place Damage", 5.0, 0.0, 20.0, 0.1, "Minimum damage required to initiate anchor placement.");
    private final Setting<Boolean> checkSelfPlacing = this.sgDamage.booleanSetting("Self Safety Check", true, "Calculates potential self-damage when placing.");
    private final Setting<Double> maxSelfPlace = this.sgDamage.doubleSetting("Max Self Place", 10.0, 0.0, 20.0, 0.1, "Maximum allowed self-damage for placement.", this.checkSelfPlacing::get);
    private final Setting<Double> minSelfRatio = this.sgDamage.doubleSetting("Min Place Ratio", 2.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Self) for placing.", this.checkSelfPlacing::get);

    private final Setting<Boolean> checkFriendPlacing = this.sgDamage.booleanSetting("Friend Safety Check", true, "Prevents placing if it would significantly damage allies.");
    private final Setting<Double> maxFriendPlace = this.sgDamage.doubleSetting("Max Friend Place", 12.0, 0.0, 20.0, 0.1, "Max allowed damage to friends during placement.", this.checkFriendPlacing::get);
    private final Setting<Double> minFriendRatio = this.sgDamage.doubleSetting("Min Friend Place Ratio", 1.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Friend) for placing.", this.checkFriendPlacing::get);

    private final Setting<Boolean> checkEnemyExplode = this.sgDamage.booleanSetting("Enemy Explode Check", true, "Ensures the explosion deals enough damage to the target.");
    private final Setting<Double> minExplode = this.sgDamage.doubleSetting("Min Explode Damage", 5.0, 0.0, 20.0, 0.1, "Minimum damage required to trigger the explosion.", this.checkEnemyExplode::get);

    private final Setting<Boolean> checkSelfExplode = this.sgDamage.booleanSetting("Self Explode Check", true, "Calculates self-damage before triggering the explosion.");
    private final Setting<Double> maxSelfExplode = this.sgDamage.doubleSetting("Max Self Explode", 10.0, 0.0, 20.0, 0.1, "Maximum allowed self-damage for explosions.", this.checkSelfExplode::get);
    private final Setting<Double> minSelfExplodeRatio = this.sgDamage.doubleSetting("Min Explode Ratio", 2.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Self) for attacking.", this.checkSelfExplode::get);

    private final Setting<Boolean> checkFriendExplode = this.sgDamage.booleanSetting("Friend Explode Check", true, "Calculates allied damage before triggering the explosion.");
    private final Setting<Double> maxFriendExplode = this.sgDamage.doubleSetting("Max Friend Explode", 12.0, 0.0, 20.0, 0.1, "Max allowed damage to friends during explosions.", this.checkFriendExplode::get);
    private final Setting<Double> minFriendExplodeRatio = this.sgDamage.doubleSetting("Min Friend Explode Ratio", 1.0, 0.0, 20.0, 0.1, "Required damage ratio (Enemy / Friend) for attacking.", this.checkFriendExplode::get);

    private final Setting<Double> forcePop = this.sgDamage.doubleSetting("Force Pop", 0.0, 0.0, 5.0, 0.25, "Bypasses damage checks if the explosion is likely to pop a totem.");
    private final Setting<Double> selfPop = this.sgDamage.doubleSetting("Anti Self-Pop", 1.0, 0.0, 5.0, 0.25, "Strictness of self-pop prevention.");
    private final Setting<Double> friendPop = this.sgDamage.doubleSetting("Anti Friend-Pop", 0.0, 0.0, 5.0, 0.25, "Strictness of allied-pop prevention.");

    private final Setting<Integer> extrapolation = this.sgExtrapolation.intSetting("Enemy Prediction", 0, 0, 20, 1, "The amount of ticks to predict enemy movement.");
    private final Setting<Integer> selfExtrapolation = this.sgExtrapolation.intSetting("Self Prediction", 0, 0, 20, 1, "The amount of ticks to predict your own movement.");
    private final Setting<Integer> hitboxExtrapolation = this.sgExtrapolation.intSetting("Hitbox Prediction", 0, 0, 20, 1, "The amount of ticks to predict the entity hitbox size.");

    private final Setting<Boolean> placeSwing = this.sgRender.booleanSetting("Swing Animation", false, "Renders a hand swing animation when interacting.");
    private final Setting<SwingHand> placeHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand, "Which hand to perform the animation with.");
    private final Setting<RenderShape> renderShape = this.sgRender.enumSetting("Render Shape", RenderShape.Full, "Visual style of the target block highlights.");
    private final Setting<BlackOutColor> lineColor = this.sgRender.colorSetting("Outline Color", new BlackOutColor(255, 0, 0, 255), "The color of the box outlines.");
    private final Setting<BlackOutColor> sideColor = this.sgRender.colorSetting("Side Color", new BlackOutColor(255, 0, 0, 50), "The color of the box faces.");

    // TODO: target не используется
    private final ExtrapolationMap extMap = new ExtrapolationMap();
    private final ExtrapolationMap hitboxMap = new ExtrapolationMap();
    private final List<PlayerEntity> enemies = new ArrayList<>();
    private BlockPos placePos = null;
    private BlockPos explodePos = null;
    private LivingEntity target = null;
    private double selfHealth = 0.0;
    private double enemyHealth = 0.0;
    private double friendHealth = 0.0;
    private double selfDamage = 0.0;
    private double enemyDamage = 0.0;
    private double friendDamage = 0.0;
    private FindResult result = null;
    private long lastPlace = 0L;
    private long lastInteract = 0L;
    private long lastExplode = 0L;
    private int progress = 0;
    private int targetProgress = 0;
    private BlockPos calcBest = null;
    private double calcValue = 0.0;
    private int calcR = 0;
    private int targetCalcR = 0;
    private BlockPos calcMiddle = null;
    private BlockPos targetCalcBest = null;
    private double targetCalcValue = 0.0;
    private boolean bestIsLoaded = false;

    public AnchorAura() {
        super("Anchor Aura", "Automatically places, charges, and detonates Respawn Anchors for high offensive damage.", SubCategory.OFFENSIVE, true);
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
            this.calc(event.tickDelta);
            if (this.explodePos != null) {
                this.updateInteract();
                this.updateExplode();
            }

            if (this.placePos != null) {
                this.updatePlace();
                Render3DUtils.box(BoxUtils.get(this.placePos), this.sideColor.get(), this.lineColor.get(), this.renderShape.get());
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
        if (BlackOut.mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            if (SettingUtils.getPlaceOnDirection(pos) != null) {
                if (SettingUtils.inInteractRange(pos)) {
                    this.calcDamage(pos);
                    if (this.explodeDamageCheck()) {
                        double value = this.getExplodeValue(pos);
                        boolean isLoaded = BlackOut.mc.world.getBlockState(pos).get(Properties.CHARGES) > 0;
                        if (!(value <= this.targetCalcValue) && (!this.bestIsLoaded || isLoaded)) {
                            this.targetCalcBest = pos;
                            this.targetCalcValue = value;
                            this.bestIsLoaded = isLoaded;
                        }
                    }
                }
            }
        }
    }

    private void calcPos(BlockPos pos) {
        if (OLEPOSSUtils.replaceable(pos) || BlackOut.mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            PlaceData data = SettingUtils.getPlaceData(pos);
            if (this.inRangeToEnemies(pos)) {
                if (data.valid()) {
                    if (SettingUtils.getPlaceOnDirection(pos) != null) {
                        if (SettingUtils.inInteractRange(pos)) {
                            if (SettingUtils.inPlaceRange(data.pos())) {
                                this.calcDamage(pos);
                                if (this.placeDamageCheck()) {
                                    double value = this.getValue(pos);
                                    if (!(value <= this.calcValue)) {
                                        if (!EntityUtils.intersects(BoxUtils.get(pos), entity -> !(entity instanceof ItemEntity), this.hitboxMap.getMap())) {
                                            this.calcBest = pos;
                                            this.calcValue = value;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean inRangeToEnemies(BlockPos pos) {
        Vec3d vec = pos.toCenterPos();

        for (PlayerEntity player : this.enemies) {
            if (BoxUtils.middle(player.getBoundingBox()).distanceTo(vec) < 4.0) {
                return true;
            }
        }

        return false;
    }

    private void updatePlace() {
        if (OLEPOSSUtils.replaceable(this.placePos)) {
            if (!(System.currentTimeMillis() - this.lastPlace < 1000.0 / this.placeSpeed.get())) {
                this.place();
            }
        }
    }

    private void updateInteract() {
        BlockState state = BlackOut.mc.world.getBlockState(this.explodePos);
        if (state.getBlock() == Blocks.RESPAWN_ANCHOR && state.get(Properties.CHARGES) <= 0) {
            if (!(System.currentTimeMillis() - this.lastInteract < 1000.0 / this.interactSpeed.get())) {
                this.interact(this.explodePos);
            }
        }
    }

    private void updateExplode() {
        BlockState state = BlackOut.mc.world.getBlockState(this.explodePos);
        if (state.getBlock() == Blocks.RESPAWN_ANCHOR && state.get(Properties.CHARGES) > 0) {
            if (!(System.currentTimeMillis() - this.lastExplode < 1000.0 / this.explodeSpeed.get())) {
                this.explode(this.explodePos);
            }
        }
    }

    private void explode(BlockPos pos) {
        Direction dir = SettingUtils.getPlaceOnDirection(pos);
        if (dir != null) {
            Predicate<ItemStack> predicate = stack -> stack.getItem() != Items.GLOWSTONE;
            Hand hand = OLEPOSSUtils.getHand(predicate);
            this.result = this.switchMode.get().find(predicate);
            if (hand != null || this.result.wasFound()) {
                PlaceData data = SettingUtils.getPlaceData(pos);
                Vec3d placeVec = data != null && data.valid() ? data.pos().toCenterPos().offset(data.dir(), 0.5) : null;
                if (!SettingUtils.shouldRotate(RotationType.Interact) || this.rotateBlock(pos, dir, placeVec, RotationType.Interact, 0.1, "explode")) {
                    boolean switched = false;
                    if (hand != null || (switched = this.switchMode.get().swap(this.result.slot()))) {
                        this.interactBlock(hand, pos.toCenterPos(), dir, pos);
                        BlackOut.mc.world.setBlockState(pos, Blocks.AIR.getDefaultState());
                        this.lastExplode = System.currentTimeMillis();
                        this.explodePos = null;
                        if (this.placeSwing.get()) {
                            this.clientSwing(this.placeHand.get(), hand);
                        }

                        this.end("explode");
                        if (switched) {
                            this.switchMode.get().swapBack();
                        }
                    }
                }
            }
        }
    }

    private void interact(BlockPos pos) {
        Direction dir = SettingUtils.getPlaceOnDirection(pos);
        if (dir != null) {
            Hand hand = OLEPOSSUtils.getHand(Items.GLOWSTONE);
            this.result = this.switchMode.get().find(Items.GLOWSTONE);
            if (hand != null || this.result.wasFound()) {
                PlaceData data = SettingUtils.getPlaceData(pos);
                Vec3d placeVec = data != null && data.valid() ? data.pos().toCenterPos().offset(data.dir(), 0.5) : null;
                if (!SettingUtils.shouldRotate(RotationType.Interact) || this.rotateBlock(pos, dir, placeVec, RotationType.Interact, 0.05, "interact")) {
                    boolean switched = false;
                    if (hand != null || (switched = this.switchMode.get().swap(this.result.slot()))) {
                        this.interactBlock(hand, pos.toCenterPos(), dir, pos);
                        RespawnAnchorBlock.charge(BlackOut.mc.player, BlackOut.mc.world, pos, BlackOut.mc.world.getBlockState(pos));
                        this.blockPlaceSound(pos, this.result.stack());
                        this.lastInteract = System.currentTimeMillis();
                        if (this.placeSwing.get()) {
                            this.clientSwing(this.placeHand.get(), hand);
                        }

                        this.end("interact");
                        if (switched) {
                            this.switchMode.get().swapBack();
                        }
                    }
                }
            }
        }
    }

    private void place() {
        PlaceData data = SettingUtils.getPlaceData(this.placePos);
        if (data.valid()) {
            Hand hand = OLEPOSSUtils.getHand(Items.RESPAWN_ANCHOR);
            this.result = this.switchMode.get().find(Items.RESPAWN_ANCHOR);
            if (hand != null || this.result.wasFound()) {
                if (!SettingUtils.shouldRotate(RotationType.BlockPlace)
                        || this.rotateBlock(data, data.pos().toCenterPos().offset(data.dir(), 0.5), RotationType.BlockPlace, "placing")) {
                    boolean switched = false;
                    if (hand != null || (switched = this.switchMode.get().swap(this.result.slot()))) {
                        this.placeBlock(hand, data.pos().toCenterPos(), data.dir(), data.pos());
                        this.setBlock(hand, this.placePos);
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

    private void setBlock(Hand hand, BlockPos pos) {
        Item item;
        if (hand == null) {
            item = BlackOut.mc.player.getInventory().getStack(this.result.slot()).getItem();
        } else {
            item = OLEPOSSUtils.getItem(hand).getItem();
        }

        if (item instanceof BlockItem block) {
            Managers.PACKET.addToQueue(handler -> {
                BlackOut.mc.world.setBlockState(pos, block.getBlock().getDefaultState());
                this.blockPlaceSound(this.placePos, this.result.stack());
            });
        }
    }

    private void updatePos() {
        this.findTargets();
        this.extMap.update(player -> player == BlackOut.mc.player ? this.selfExtrapolation.get() : this.extrapolation.get());
        this.hitboxMap.update(player -> player == BlackOut.mc.player ? 0 : this.hitboxExtrapolation.get());
        this.placePos = this.calcBest;
        this.explodePos = this.targetCalcBest;
        this.startCalc();
    }

    private void findTargets() {
        Map<PlayerEntity, Double> map = new HashMap<>();

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && !(player.getHealth() <= 0.0F)) {
                double distance = BlackOut.mc.player.distanceTo(player);
                if (!(distance > this.enemyDistance.get())) {
                    if (map.size() < 3) {
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

        this.enemies.clear();
        map.forEach((playerx, d) -> this.enemies.add(playerx));
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
        this.bestIsLoaded = false;
        this.targetCalcR = (int) Math.ceil(SettingUtils.maxInteractRange());
        this.targetProgress = 0;
    }

    private double getExplodeValue(BlockPos pos) {
        double value = 0.0;
        if (SettingUtils.shouldRotate(RotationType.Interact)) {
            double yaw = Math.abs(RotationUtils.yawAngle(Managers.ROTATION.prevYaw, RotationUtils.getYaw(pos)));
            double per = Math.max(SettingUtils.yawStep(RotationType.Interact), 45.0);
            int steps = (int) Math.ceil(yaw / per);
            value += 180.0 / per - steps;
        }

        return value + (this.enemyDamage - this.selfDamage);
    }

    private double getValue(BlockPos pos) {
        double value = 0.0;
        if (SettingUtils.shouldRotate(RotationType.BlockPlace)) {
            double yaw = Math.abs(RotationUtils.yawAngle(Managers.ROTATION.prevYaw, RotationUtils.getYaw(pos)));
            double per = Math.max(SettingUtils.yawStep(RotationType.BlockPlace), 45.0);
            int steps = (int) Math.ceil(yaw / per);
            value += 180.0 / per - steps;
        }

        return value + (this.enemyDamage - this.selfDamage);
    }

    private boolean explodeDamageCheck() {
        if (this.selfDamage * this.selfPop.get() > this.selfHealth) {
            return false;
        } else if (this.friendDamage * this.friendPop.get() > this.friendHealth) {
            return false;
        } else if (this.enemyDamage * this.forcePop.get() > this.enemyHealth) {
            return true;
        } else if (this.checkEnemyExplode.get() && this.enemyDamage < this.minExplode.get()) {
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
        } else if (this.enemyDamage < this.minPlace.get()) {
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

    private void calcDamage(BlockPos pos) {
        Vec3d vec = pos.toCenterPos();
        this.target = null;
        this.selfDamage = DamageUtils.anchorDamage(BlackOut.mc.player, this.extMap.get(BlackOut.mc.player), vec, pos);
        this.enemyDamage = 0.0;
        this.friendDamage = 0.0;
        this.enemyHealth = 20.0;
        this.friendHealth = 20.0;
        this.enemies.forEach(player -> {
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
    }

    private double getHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }
}
