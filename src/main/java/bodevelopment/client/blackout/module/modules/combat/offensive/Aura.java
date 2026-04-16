package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.*;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IClipContext;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.MoveUpdateModule;
import bodevelopment.client.blackout.annotations.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.modules.combat.misc.Teams;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.randomstuff.ExtrapolationMap;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Aura extends MoveUpdateModule {
    public static AbstractClientPlayer targetedPlayer = null;
    private static Aura INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgTeleport = this.addGroup("Teleport");
    private final SettingGroup sgBlocking = this.addGroup("Blocking");
    private final SettingGroup sgDelay = this.addGroup("Delay");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<TargetMode> targetMode = this.sgGeneral.enumSetting("Target Priority", TargetMode.Health, "Determines which entity is prioritized as the primary target.");
    private final Setting<Boolean> checkMaxHP = this.sgGeneral.booleanSetting("HP Threshold Check", false, "Enable to filter targets based on their current health.");
    private final Setting<Integer> maxHp = this.sgGeneral.intSetting("Maximum Health", 36, 0, 100, 1, "Only targets with total health below this value will be attacked.", this.checkMaxHP::get);
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Auto Switch", SwitchMode.Disabled, "Method for automatically switching to a combat weapon.");
    private final Setting<Boolean> onlyWeapon = this.sgGeneral.booleanSetting("Weapon Filter", true, "Restricts attacks to only occur when holding a valid weapon.");
    private final Setting<List<Item>> allowedItems = this.sgGeneral.itemFilteredListSetting("Allowed Items", "Whitelist of tools/weapons that are allowed for attacking (empty = any tool).", onlyWeapon::get, item -> {
                ItemStack stack = item.getDefaultInstance();

                if (stack.has(DataComponents.TOOL)) return true;
                return item instanceof SwordItem
                        || item == Items.TRIDENT
                        || item == Items.MACE;
                    },
            Items.WOODEN_SWORD,
            Items.STONE_SWORD,
            Items.GOLDEN_SWORD,
            Items.IRON_SWORD,
            Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD,
            Items.WOODEN_AXE,
            Items.STONE_AXE,
            Items.GOLDEN_AXE,
            Items.IRON_AXE,
            Items.DIAMOND_AXE,
            Items.NETHERITE_AXE,
            Items.TRIDENT,
            Items.MACE);
    private final Setting<Integer> maxTargets = this.sgGeneral.intSetting("Max Targets", 1, 1, 10, 1, "Maximum number of entities to attack simultaneously.");
    private final Setting<WeaponPreference> weaponPreference = this.sgGeneral.enumSetting("Weapon Preference", WeaponPreference.Default, "Determines which weapon type is prioritized when multiple are available.");
    private final Setting<Boolean> ignoreNaked = this.sgGeneral.booleanSetting("Ignore Unarmored", false, "Prevents attacking players who are not wearing any armor.");
    private final Setting<Boolean> tpDisable = this.sgGeneral.booleanSetting("TP Safety Disable", false, "Automatically disables the module upon server teleports or dimension changes.");
    private final Setting<RotationMode> rotationMode = this.sgGeneral.enumSetting("Rotation Logic", RotationMode.OnHit, "Defines when the client should look at the target.");
    private final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityFilterdListSetting("Entity Targets", "List of entity types that the aura is allowed to attack.", type -> type != EntityType.ITEM && type != EntityType.EXPERIENCE_ORB && type != EntityType.AREA_EFFECT_CLOUD && type != EntityType.MARKER && type != EntityType.POTION && type != EntityType.LLAMA_SPIT && type != EntityType.EYE_OF_ENDER && type != EntityType.DRAGON_FIREBALL && type != EntityType.FIREWORK_ROCKET && type != EntityType.ENDER_PEARL && type != EntityType.FISHING_BOBBER && type != EntityType.ARROW && type != EntityType.SPECTRAL_ARROW && type != EntityType.SNOWBALL && type != EntityType.SMALL_FIREBALL && type != EntityType.WITHER_SKULL && type != EntityType.FALLING_BLOCK && type != EntityType.TNT && type != EntityType.EVOKER_FANGS && type != EntityType.LIGHTNING_BOLT && type != EntityType.WIND_CHARGE && type != EntityType.BREEZE_WIND_CHARGE && !type.toString().contains("display"), EntityType.PLAYER);
    private final Setting<Double> hitChance = this.sgGeneral.doubleSetting("Accuracy Chance", 1.0, 0.0, 1.0, 0.01, "Percentage of intended hits that will actually be sent to the server.");
    private final Setting<Double> expand = this.sgGeneral.doubleSetting("Hitbox Expansion", 0.0, 0.0, 1.0, 0.01, "Artificially increases the size of the target's hitbox.");
    private final Setting<Integer> extrapolation = this.sgGeneral.intSetting("Movement Prediction", 1, 0, 3, 1, "The number of ticks to extrapolate the target's current movement.");
    private final Setting<Boolean> disableDead = this.sgGeneral.booleanSetting("Auto Disable on Death", false, "Disables the module when the player dies.");
    private final Setting<Boolean> ignoreRanges = this.sgGeneral.booleanSetting("Bypass Range Limits", false, "Ignores standard attack range checks, useful for specific combat scenarios.");
    private final Setting<Double> hitHeight = this.sgGeneral.doubleSetting("Strike Height", 0.8, 0.0, 1.0, 0.01, "The vertical offset on the target's hitbox to prioritize for hits.");
    private final Setting<Double> dynamicHeight = this.sgGeneral.doubleSetting("Eye Height Scaling", 0.5, 0.0, 1.0, 0.01, "Adjusts hit height based on your current eye level relative to the target.");
    private final Setting<Boolean> critSprint = this.sgGeneral.booleanSetting("Sprint Reset Crit", true, "Sends a stop-sprint packet before hitting to ensure a critical hit occurs.");
    private final Setting<Double> scanRange = this.sgGeneral.doubleSetting("Tracing Range", 0.0, 0.0, 10.0, 0.1, "Maximum distance to scan for targets with a direct line of sight.");
    private final Setting<Double> wallScanRange = this.sgGeneral.doubleSetting("Wall Tracing Range", 0.0, 0.0, 10.0, 0.1, "Maximum distance to scan for targets behind obstacles.");
    private final Setting<Boolean> pauseOnEat = this.sgGeneral.booleanSetting("Pause on Eat", true, "Pauses the aura while you are using an item (eating, drinking, etc.).");

    private final Setting<Boolean> teleport = this.sgTeleport.booleanSetting("Teleport Attacks", false, "Enables attacking targets outside of normal range by teleporting.");
    private final Setting<Integer> maxPackets = this.sgTeleport.intSetting("TP Packet Limit", 1, 1, 10, 1, "Max number of position packets sent to reach the target.");
    private final Setting<Double> maxDistance = this.sgTeleport.doubleSetting("TP Distance Limit", 5.0, 1.0, 50.0, 0.5, "Maximum total distance allowed for a teleportation attack.");
    private final Setting<Boolean> tpBack = this.sgTeleport.booleanSetting("Teleport Back", false, "Returns you to your original position after a teleport attack.");

    private final Setting<Boolean> blocking = this.sgBlocking.booleanSetting("Auto Shield/Block", false, "Automatically uses a shield or sword to block incoming damage.");
    private final Setting<BlockMode> block = this.sgBlocking.enumSetting("Blocking Logic", BlockMode.Hold, "Determines how the blocking action is performed.", this.blocking::get);
    private final Setting<BlockRenderMode> blockRender = this.sgBlocking.enumSetting("Block Animation", BlockRenderMode.Disabled, "Visual style for the blocking animation.", this.blocking::get);
    private final Setting<Double> speed = this.sgBlocking.doubleSetting("Animation Speed", 0.5, 0.0, 1.0, 0.01, "Speed of the visual blocking animation.", () -> this.blocking.get() && this.blockRender.get() == BlockRenderMode.Fan || this.blockRender.get() == BlockRenderMode.Float || this.blockRender.get() == BlockRenderMode.Slap);

    private final Setting<DelayMode> delayMode = this.sgDelay.enumSetting("Attack Timing", DelayMode.Smart, "Algorithm used to determine the interval between attacks.");
    private final Setting<RandomMode> randomise = this.sgDelay.enumSetting("CPS Variation", RandomMode.Random, "Adds randomization to the clicks per second for a more human-like behavior.", () -> this.delayMode.get() == DelayMode.Basic);
    private final Setting<Double> maxCps = this.sgDelay.doubleSetting("Maximum CPS", 12.0, 0.0, 20.0, 0.1, "Upper limit for clicks per second.", () -> this.delayMode.get() == DelayMode.Basic && this.randomise.get() != RandomMode.Disabled);
    private final Setting<Double> minCps = this.sgDelay.doubleSetting("Minimum CPS", 8.0, 0.0, 20.0, 0.1, "Lower limit for clicks per second.", () -> this.delayMode.get() == DelayMode.Basic && this.randomise.get() != RandomMode.Disabled);
    private final Setting<Double> cpsSetting = this.sgDelay.doubleSetting("Constant CPS", 15.0, 0.0, 20.0, 0.1, "The exact number of clicks per second when randomization is off.", () -> this.delayMode.get() == DelayMode.Basic && this.randomise.get() == RandomMode.Disabled);
    private final Setting<Boolean> fatigueSim = this.sgDelay.booleanSetting("Fatigue Simulation", false, "Gradually slows down CPS over time to mimic human tiredness.", () -> this.delayMode.get() == DelayMode.Basic);
    private final Setting<Integer> maxFatigue = this.sgDelay.intSetting("Fatigue Ceiling", 50, 0, 1000, 1, "Maximum delay in milliseconds added by fatigue.", () -> this.delayMode.get() == DelayMode.Basic && this.fatigueSim.get());
    private final Setting<Integer> fatigueRaise = this.sgDelay.intSetting("Fatigue Intensity", 5, 0, 1000, 1, "How much fatigue increases per attack.", () -> this.delayMode.get() == DelayMode.Basic && this.fatigueSim.get());
    private final Setting<Integer> fatigueDecrease = this.sgDelay.intSetting("Fatigue Recovery", 2, 0, 1000, 1, "How quickly fatigue dissipates when not attacking.", () -> this.delayMode.get() == DelayMode.Basic && this.fatigueSim.get());
    private final Setting<Double> charge = this.sgDelay.doubleSetting("Attack Charge", 1.0, 0.0, 1.0, 0.01, "Required cooldown percentage before attacking in Vanilla mode.", () -> this.delayMode.get() == DelayMode.Vanilla);
    private final Setting<Double> minDelay = this.sgDelay.doubleSetting("Minimum Delay", 0.5, 0.0, 1.0, 0.01, "Lowest possible time between attacks.", () -> this.delayMode.get() == DelayMode.Smart || this.delayMode.get() == DelayMode.Vanilla);
    private final Setting<Double> randomNegative = this.sgDelay.doubleSetting("Jitter Negative", 0.0, 0.0, 1.0, 0.01, "Maximum subtraction from the calculated attack delay.", () -> this.delayMode.get() == DelayMode.Smart || this.delayMode.get() == DelayMode.Vanilla);
    private final Setting<Double> randomPositive = this.sgDelay.doubleSetting("Jitter Positive", 0.0, 0.0, 1.0, 0.01, "Maximum addition to the calculated attack delay.", () -> this.delayMode.get() == DelayMode.Smart || this.delayMode.get() == DelayMode.Vanilla);
    private final Setting<Integer> packets = this.sgDelay.intSetting("Attack Packets", 1, 1, 10, 1, "Number of attack packets to send per strike.");
    private final Setting<Boolean> critSync = this.sgDelay.booleanSetting("Critical Synchronization", true, "Waits for the optimal falling moment to ensure a critical hit.");
    private final Setting<Double> critVelocity = this.sgDelay.doubleSetting("Crit Fall Velocity", 0.1, 0.0, 1.0, 0.01, "The downward velocity required to trigger a synchronized critical attack.", this.critSync::get);

    private final Setting<Boolean> hitParticles = this.sgRender.booleanSetting("Critical Particles", false, "Spawns particle effects when a target is successfully hit.");
    private final Setting<Boolean> swing = this.sgRender.booleanSetting("Hand Animation", true, "Renders the arm swing animation on the client.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Arm", SwingHand.RealHand, "Which arm to use for the swing animation.", this.swing::get);
    private final Setting<RenderMode> renderMode = this.sgRender.enumSetting("Target Rendering", RenderMode.Hit, "Method for highlighting the current target.");
    private final Setting<Double> renderTime = this.sgRender.doubleSetting("Visual Duration", 1.0, 0.0, 10.0, 0.1, "How long the target highlight remains visible.");
    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgRender, "Target Highlight");

    private final RenderList<AABB> renderBoxes = RenderList.getList(false);
    private final ExtrapolationMap extrapolationMap = new ExtrapolationMap();
    private final java.util.Map<Integer, Pair<Vec3, AABB>> expandCache = new java.util.HashMap<>();
    public boolean isBlocking = false;
    public boolean isAttacking = false;
    private boolean shouldRender = false;
    private long prevAttack = 0L;
    private long lastTargetUpdate = 0L;
    private long nextBlock = 0L;
    private double alwaysRenderTime = 0.0;
    private float f = 0.0F;
    private int fatigue = 0;
    private int timeOG = 0;
    private double random;
    private final List<Entity> targets = new ArrayList<>();
    private Entity target = null;
    private AABB renderBox = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    public Aura() {
        super("Aura", "Automatically engages nearby entities using advanced targeting and timing algorithms.", SubCategory.OFFENSIVE);
        INSTANCE = this;
    }

    public static Aura getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.rotation.end("attacking");
    }

    @Override
    public void onDisable() {
        if (this.isBlocking) {
            this.stopBlocking();
        }
        this.expandCache.clear();
        this.targets.clear();
        this.target = null;
    }

    @Override
    public String getInfo() {
        if (this.targets.isEmpty()) {
            return null;
        }
        if (this.targets.size() == 1) {
            Entity t = this.targets.getFirst();
            return t.getName().getString().length() > 16 ? "Attacking" : t.getName().getString();
        }
        return "Attacking " + this.targets.size();
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        this.expandCache.clear();
        if (this.tpDisable.get()) {
            this.disable(this.getDisplayName() + " was disabled due to server change/teleport", 5, Notifications.Type.Info);
        }
    }

    @Override
    public void onTickPre(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (BlackOut.mc.player.onGround()) {
                this.timeOG++;
            } else {
                this.timeOG = 0;
            }

            if (!this.holdingSword() && this.isBlocking) {
                this.stopBlocking();
            }

            if (this.enabled) {
                super.onTickPre(event);
                if (this.disableDead.get() && BlackOut.mc.screen instanceof DeathScreen) {
                    this.disable(this.getDisplayName() + " was disabled due to dying");
                } else if (this.holdingSword()) {
                    if (this.target == null) {
                        if (this.isBlocking) {
                            this.stopBlocking();
                        }
                    } else if (this.block.get() == BlockMode.Hold) {
                        if (this.isBlocking && System.currentTimeMillis() > this.nextBlock) {
                            this.nextBlock = System.currentTimeMillis() + 500L;
                            this.stopBlocking();
                            if (SettingUtils.grimPackets() && this.blocking.get()) {
                                this.startBlocking();
                            }
                        } else if (!this.isBlocking && this.blocking.get()) {
                            this.startBlocking();
                        }
                    }
                }
            }
        } else {
            this.isBlocking = false;
        }
    }

    @Event
    public void onSend(PacketEvent.Sent event) {
        if (!this.blocking.get()) {
            this.isBlocking = false;
        } else {
            if (event.packet instanceof ServerboundUseItemPacket && this.holdingSword()) {
                this.isBlocking = true;
            }

            if (event.packet instanceof ServerboundPlayerActionPacket packet && packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
                this.isBlocking = false;
            }
        }
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (PlayerUtils.isInGame()) {
            this.renderBoxes.update((box, delta, time) -> this.rendering.render(box, (float) (1.0 - delta), 1.0F));
            if (!this.enabled) {
                this.renderSingle(false, event.frameTime);
            } else {
                long now = System.currentTimeMillis();
                if (now - this.lastTargetUpdate >= 50L) {
                    this.updateTarget();
                    this.lastTargetUpdate = now;
                }
                if (this.target != null && this.shouldRender) {
                    this.renderBox = this.getBox(this.target);
                    Vec3 offset = this.target
                            .position()
                            .subtract(this.target.xo, this.target.yo, this.target.zo)
                            .scale(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
                    this.renderBox = this.renderBox.move(offset);
                    if (this.target instanceof AbstractClientPlayer player) {
                        targetedPlayer = player;
                    } else {
                        targetedPlayer = null;
                    }
                } else {
                    targetedPlayer = null;
                }

                this.renderSingle(this.shouldRender, event.frameTime);
            }
        }
    }

    private void renderSingle(boolean refresh, double frameTime) {
        if (this.renderMode.get() == RenderMode.Always && this.renderBox != null) {
            if (refresh) {
                this.alwaysRenderTime = this.renderTime.get();
            } else {
                this.alwaysRenderTime -= frameTime;
            }

            double progress = Mth.clamp(this.alwaysRenderTime / this.renderTime.get(), 0.0, 1.0);
            this.rendering.render(this.renderBox, (float) progress, (float) progress);
        }
    }

    @Override
    protected void update(boolean allowAction, boolean fakePos) {
        if (BlackOut.mc.player == null || (this.pauseOnEat.get() && BlackOut.mc.player.isUsingItem())) return;
        if (this.target == null) {
            this.fatigue = Math.max(this.fatigue - this.fatigueDecrease.get(), 0);
        }

        this.shouldRender = false;
        if (this.target != null && PlayerUtils.isInGame() && this.enabled) {
            int slot = this.bestSlot(this.switchMode.get().inventory);
            boolean holding = !this.onlyWeapon.get() || holdingAllowedWeapon();
            if (slot >= 0) {
                if (!this.onlyWeapon.get() || this.isAllowedWeapon(BlackOut.mc.player.getInventory().getItem(slot))) {
                    if (holding || this.switchMode.get() != SwitchMode.Disabled) {
                        this.shouldRender = true;
                        boolean rotated = this.rotationMode.get() != RotationMode.Constant
                                || !SettingUtils.shouldRotate(RotationType.Attacking)
                                || this.rotation.attackRotate(this.getBox(this.target), this.getRotationVec(), "attacking");
                        if (rotated && this.delayCheck()) {
                            if (this.rotationMode.get() != RotationMode.OnHit
                                    || !SettingUtils.shouldRotate(RotationType.Attacking)
                                    || this.rotation.attackRotate(this.getBox(this.target), this.getRotationVec(), "attacking")) {
                                if (this.inRange(this.target)) {
                                    if (allowAction) {
                                        boolean switched = false;
                                        if (holding || (switched = this.switchMode.get().swap(slot))) {
                                            this.fatigue = this.fatigue + this.fatigueRaise.get();
                                            this.attackTarget();
                                            if (switched) {
                                                this.switchMode.get().swapBack();
                                            }

                                            if (this.rotationMode.get() == RotationMode.OnHit) {
                                                this.rotation.end("attacking");
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
    }

    private Vec3 getRotationVec() {
        double x = Mth.clamp(this.target.getDeltaMovement().x + BlackOut.mc.player.getDeltaMovement().x, -0.3, 0.3);
        double y = Mth.clamp(this.getHitHeight(), this.target.getBoundingBox().minY, this.target.getBoundingBox().maxY);
        double z = Mth.clamp(this.target.getDeltaMovement().z + BlackOut.mc.player.getDeltaMovement().z, -0.3, 0.3);
        return new Vec3(this.target.getX() + x, y, this.target.getZ() + z);
    }

    private double getHitHeight() {
        double targetY = Mth.lerp(this.hitHeight.get(), this.target.getBoundingBox().minY, this.target.getBoundingBox().maxY);
        return Mth.lerp(this.dynamicHeight.get(), targetY, BlackOut.mc.player.getEyeY());
    }

    private boolean delayCheck() {
        if (this.critSync.get() && this.timeOG < 5 && this.shouldWaitCrit()) {
            return false;
        } else {
            double timeSince = (System.currentTimeMillis() - this.prevAttack) / 1000.0;
            return switch (this.delayMode.get()) {
                case Basic -> timeSince > this.getDelay();
                case Smart -> {
                    double delay = Math.max(1.0 / BlackOut.mc.player.getAttributeValue(Attributes.ATTACK_SPEED), this.minDelay.get());
                    yield timeSince > this.getRandom(delay - this.randomNegative.get(), delay + this.randomPositive.get());
                }
                case Vanilla -> timeSince >= this.minDelay.get()
                        && BlackOut.mc.player.attackStrengthTicker >= 20.0 / BlackOut.mc.player.getAttributeValue(Attributes.ATTACK_SPEED) * this.charge.get();
            };
        }
    }

    private double getRandom(double start, double end) {
        return Mth.lerp(this.random, start, end);
    }

    private boolean shouldWaitCrit() {
        return (this.critSprint.get() || !BlackOut.mc.player.isSprinting()) && (BlackOut.mc.player.onGround()
                || BlackOut.mc.player.fallDistance <= 0.0F
                || BlackOut.mc.player.getDeltaMovement().y >= -this.critVelocity.get());
    }

    private double getDelay() {
        double cps = this.randomise.get() == RandomMode.Disabled ? this.cpsSetting.get() : this.getRandom(this.minCps.get(), this.maxCps.get());
        return 1.0 / cps + this.getFatigue() / 1000.0;
    }

    private double getFatigue() {
        if (!this.fatigueSim.get()) {
            return 0.0;
        } else {
            long f = Math.min(this.fatigue, this.maxFatigue.get().longValue());
            return this.fatigueSim.get() ? f : 0.0;
        }
    }

    private void attackTarget() {
        if (this.targets.isEmpty()) return;

        this.isAttacking = true;
        if (this.holdingSword() && this.block.get() == BlockMode.Spam && this.blocking.get()) {
            this.stopBlocking();
        }

        this.prevAttack = System.currentTimeMillis();
        this.random = this.randomise.get().get();
        SettingUtils.swing(SwingState.Pre, SwingType.Attacking, InteractionHand.MAIN_HAND);

        List<Vec3> positions = null;
        if (this.target != null && !SettingUtils.inAttackRange(this.target.getBoundingBox()) && this.teleport.get()) {
            positions = this.getPath(this.target);
            if (positions != null) {
                positions.forEach(posx -> this.sendInstantly(new ServerboundMovePlayerPacket.Pos(posx.x(), posx.y(), posx.z(), false, BlackOut.mc.player.horizontalCollision)));
                if (this.tpBack.get()) {
                    BlackOut.mc.player.setPos(positions.getLast());
                }
            }
        }

        if (this.chanceCheck()) {
            if (Criticals.getInstance().enabled) {
                Criticals.getInstance().doCritLogic();
            }

            boolean shouldCritSprint = this.critSprint.get()
                    && !BlackOut.mc.player.onGround()
                    && BlackOut.mc.player.fallDistance > 0.0F
                    && BlackOut.mc.player.isSprinting();

            if (shouldCritSprint) {
                this.sendPacket(new ServerboundPlayerCommandPacket(BlackOut.mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }

            for (Entity currentTarget : this.targets) {
                if (!currentTarget.isAlive() || currentTarget.isRemoved()) {
                    continue;
                }

                if (currentTarget instanceof EndCrystal && Managers.ENTITY.isDead(currentTarget.getId())) {
                    continue;
                }

                for (int i = 0; i < this.packets.get(); i++) {
                    this.sendPacket(ServerboundInteractPacket.createAttackPacket(currentTarget, BlackOut.mc.player.isShiftKeyDown()));
                }

                if (currentTarget instanceof EndCrystal) {
                    Managers.ENTITY.setSemiDead(currentTarget.getId());
                }
            }

            if (shouldCritSprint) {
                this.sendPacket(new ServerboundPlayerCommandPacket(BlackOut.mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            }

            this.spawnParticles();
        }

        BlackOut.mc.player.attackStrengthTicker = 0;
        SettingUtils.swing(SwingState.Post, SwingType.Attacking, InteractionHand.MAIN_HAND);

        if (positions != null && this.tpBack.get()) {
            for (int i = positions.size() - 2; i >= 0; i--) {
                Vec3 pos = positions.get(i);
                this.sendInstantly(new ServerboundMovePlayerPacket.Pos(pos.x(), pos.y(), pos.z(), false, BlackOut.mc.player.horizontalCollision));
            }
            this.sendInstantly(new ServerboundMovePlayerPacket.Pos(BlackOut.mc.player.getX(), BlackOut.mc.player.getY(), BlackOut.mc.player.getZ(), false, BlackOut.mc.player.horizontalCollision));
        }

        if (this.holdingSword() && this.block.get() == BlockMode.Spam && this.blocking.get()) {
            this.startBlocking();
        }

        if (this.renderMode.get() == RenderMode.Hit && this.renderBox != null) {
            this.renderBoxes.add(this.renderBox, this.renderTime.get());
        }

        if (this.swing.get()) {
            this.clientSwing(this.swingHand.get(), InteractionHand.MAIN_HAND);
        }
        this.isAttacking = false;
    }

    private boolean holdingSword() {
        return Managers.PACKET.getStack().getItem() instanceof SwordItem;
    }

    private boolean holdingAllowedWeapon() {
        return isAllowedWeapon(Managers.PACKET.getStack());
    }

    private boolean isAllowedWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (this.allowedItems.get().isEmpty()) {
            return stack.has(DataComponents.TOOL);
        }

        return this.allowedItems.get().contains(stack.getItem());
    }

    private boolean chanceCheck() {
        return ThreadLocalRandom.current().nextDouble() <= this.hitChance.get();
    }

    private void spawnParticles() {
        if (this.hitParticles.get()) {
            BlackOut.mc.particleEngine.createTrackingEmitter(this.target, ParticleTypes.CRIT);
        }
    }

    private int bestSlot(boolean inventory) {
        int limit = inventory ? BlackOut.mc.player.getInventory().getContainerSize() + 1 : 9;
        int bestSlot = -1;
        double bestDamage = -1.0;

        for (int i = 0; i < limit; i++) {
            ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
            if (!this.onlyWeapon.get() || this.isAllowedWeapon(stack)) {
                double damage = DamageUtils.itemDamage(stack);
                boolean preferred = matchesPreference(stack, this.weaponPreference.get());

                if (this.weaponPreference.get() == WeaponPreference.Default ||
                    this.weaponPreference.get() == WeaponPreference.Damage) {
                    if (damage > bestDamage) {
                        bestSlot = i;
                        bestDamage = damage;
                    }
                } else {
                    if (preferred) {
                        if (damage > bestDamage) {
                            bestSlot = i;
                            bestDamage = damage;
                        }
                    } else if (bestSlot == -1) {
                        continue;
                    }
                }
            }
        }

        if (bestSlot == -1 && this.weaponPreference.get() != WeaponPreference.Default && 
            this.weaponPreference.get() != WeaponPreference.Damage) {
            bestDamage = -1.0;
            for (int i = 0; i < limit; i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
                if (!this.onlyWeapon.get() || this.isAllowedWeapon(stack)) {
                    double damage = DamageUtils.itemDamage(stack);
                    if (damage > bestDamage) {
                        bestSlot = i;
                        bestDamage = damage;
                    }
                }
            }
        }
        
        return bestSlot;
    }
    
    private boolean matchesPreference(ItemStack stack, WeaponPreference preference) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        
        return switch (preference) {
            case Sword -> item instanceof SwordItem;
            case Axe -> item instanceof AxeItem;
            case Mace -> item == Items.MACE;
            case Trident -> item == Items.TRIDENT;
            default -> true;
        };
    }

    private void updateTarget() {
        this.targets.clear();
        this.extrapolationMap.update(entity -> this.extrapolation.get());

        List<Pair<Entity, Double>> candidates = new ArrayList<>();
        
        BlackOut.mc.level.entitiesForRendering().forEach(entity -> {
            if (this.entities.get().contains(entity.getType()) && entity != BlackOut.mc.player) {
                if (entity instanceof ItemEntity ||
                        entity instanceof ExperienceOrb ||
                        entity instanceof Projectile ||
                        entity instanceof AreaEffectCloud) {
                    return;
                }

                double distance = BlackOut.mc.player.distanceTo(entity);
                if (this.teleport.get()) {
                    if (distance > this.maxPackets.get() * this.maxDistance.get()) {
                        return;
                    }
                } else if (distance > 10.0) {
                    return;
                }
                double val = switch (this.targetMode.get()) {
                    case Health ->
                            entity instanceof LivingEntity le ? 10000.0F - le.getHealth() - le.getAbsorptionAmount() : 50.0;
                    case Angle ->
                            10000.0 - Math.abs(RotationUtils.yawAngle(BlackOut.mc.player.getYRot(), RotationUtils.getYaw(entity)));
                    case Distance -> 10000.0 - BlackOut.mc.player.position().distanceTo(entity.position());
                };
                
                if (entity instanceof LivingEntity livingEntity) {
                    if (livingEntity.isRemoved() || !livingEntity.isAlive()) return;
                    if (livingEntity.getHealth() <= 0.0F) {
                        return;
                    }
                    if (livingEntity.isSpectator()) {
                        return;
                    }
                    if (!this.inScanRange(entity) && !this.inRange(entity)) {
                        return;
                    }
                }

                if (entity instanceof AbstractClientPlayer player) {
                    AntiBot antiBot = AntiBot.getInstance();
                    Teams teams = Teams.getInstance();
                    if (antiBot.enabled && antiBot.mode.get() == AntiBot.HandlingMode.Ignore && antiBot.getBots().contains(player)) {
                        return;
                    }
                    if (teams.enabled && teams.isTeammate(player)) {
                        return;
                    }
                    if (this.ignoreNaked.get() && !this.getArmor(player)) {
                        return;
                    }
                    if ((player.getHealth() + player.getAbsorptionAmount()) > this.maxHp.get().intValue()) {
                        if (this.checkMaxHP.get()) {
                            return;
                        }
                    }
                    if (Managers.FRIENDS.isFriend(player)) {
                        return;
                    }
                }

                candidates.add(new Pair<>(entity, val));
            }
        });

        candidates.sort((a, b) -> Double.compare(b.getB(), a.getB()));

        int limit = Math.min(candidates.size(), this.maxTargets.get());
        for (int i = 0; i < limit; i++) {
            this.targets.add(candidates.get(i).getA());
        }

        this.target = this.targets.isEmpty() ? null : this.targets.getFirst();
    }

    private boolean inScanRange(Entity entity) {
        return SettingUtils.attackRangeTo(entity.getBoundingBox(), null)
                <= (SettingUtils.attackTrace(entity.getBoundingBox()) ? this.scanRange.get() : this.wallScanRange.get());
    }

    private boolean inRange(Entity entity) {
        if (this.ignoreRanges.get()) {
            if (BlackOut.mc.player.distanceTo(entity) < 8.0F) {
                return true;
            }
        } else if (SettingUtils.inAttackRange(entity.getBoundingBox())) {
            return true;
        }

        return this.teleport.get() && this.canTeleport(entity);
    }

    private void startBlocking() {
        this.sendSequenced(s -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, s, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch));
    }

    private void stopBlocking() {
        this.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN, 0));
    }

    private AABB getBox(Entity entity) {
        AABB box = this.extrapolationMap.get(entity);
        return this.expand.get() > 0.0 ? this.expandHitbox(box, entity) : box;
    }

    private AABB expandHitbox(AABB box, Entity entity) {
        Vec3 pos = entity.position();
        Pair<Vec3, AABB> cached = this.expandCache.get(entity.getId());
        if (cached != null && cached.getA().equals(pos)) {
            return cached.getB();
        }

        for (int i = 0; i <= 20; i++) {
            box = this.expand(entity, box, 0.05, 0.0, 0.0);
            box = this.expand(entity, box, 0.0, 0.0, 0.05);
            box = this.expand(entity, box, 0.0, 0.05, 0.0);
            box = this.expand(entity, box, -0.05, 0.0, 0.0);
            box = this.expand(entity, box, 0.0, 0.0, -0.05);
            box = this.expand(entity, box, 0.0, -0.05, 0.0);
        }

        this.expandCache.put(entity.getId(), new Pair<>(pos, box));
        return box;
    }

    private AABB expand(Entity entity, AABB box, double x, double y, double z) {
        AABB newBox = box.expandTowards(x * this.expand.get(), y * this.expand.get(), z * this.expand.get());
        return BlockUtils.hasEntityCollision(entity, newBox) ? box : newBox;
    }

    public boolean blockTransform(PoseStack stack) {
        if (this.enabled && this.target != null && BlackOut.mc.player.getMainHandItem().getItem() instanceof SwordItem) {
            if (this.blockRender.get() != BlockRenderMode.Disabled && this.blocking.get()) {
                stack.pushPose();
                this.f = this.f + BlackOut.mc.getDeltaTracker().getGameTimeDeltaTicks() / 20.0F * this.speed.get().floatValue() * 5.0F;
                this.f = this.f - (int) this.f;
                float swingProgress = BlackOut.mc.player.getAttackAnim(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
                float d;
                if (this.target instanceof LivingEntity livingEntity) {
                    float hurt = livingEntity.hurtTime - BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                    d = 1.0F - this.boAnimate(1.0F - Math.max(hurt, 0.0F) / 10.0F);
                } else {
                    d = 0.0F;
                }

                switch (this.blockRender.get()) {
                    case BlackOut:
                        stack.translate(0.5, -0.5, -1.25);
                        stack.mulPose(Axis.XP.rotationDegrees(65.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(-60.0F + d * 75.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        break;
                    case KassuK:
                        stack.translate(0.5, -0.5, -1.25);
                        stack.mulPose(Axis.XP.rotationDegrees(65.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(-60.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(90.0F + d * 50.0F));
                        break;
                    case Retarded: {
                        float k = (float) Math.sin(System.currentTimeMillis() / 150.0);
                        stack.translate(0.3, -0.55 - 0.2 * k, -1.15);
                        stack.mulPose(Axis.XP.rotationDegrees(65.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(-45.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(100.0F + k * 50.0F));
                        break;
                    }
                    case KassuK2: {
                        float k = (float) Math.sin(System.currentTimeMillis() / 65.0);
                        stack.translate(0.3, -0.55 - 0.1 * k, -1.15);
                        stack.mulPose(Axis.XP.rotationDegrees(65.0F - k * 10.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(-45.0F - k * 10.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(90.0F + k * 10.0F));
                        break;
                    }
                    case KassuK3: {
                        float k = (float) Math.sin(System.currentTimeMillis() / 65.0);
                        stack.translate(0.3, -0.55 - 0.1 * k, -1.15);
                        stack.mulPose(Axis.XP.rotationDegrees(65.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(-45.0F + k * 10.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        break;
                    }
                    case Fan:
                        stack.translate(0.5, -0.5, -1.25);
                        stack.mulPose(Axis.XP.rotationDegrees(65.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(-360.0F * this.f));
                        stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        break;
                    case Float:
                        this.transformItem(stack, -0.1F, this.f);
                        stack.translate(0.5, -0.4, -0.2);
                        stack.mulPose(Axis.XP.rotationDegrees(-70.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(32.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(40.0F));
                        break;
                    case Slap:
                        this.transformItem(stack, 0.0F, this.f);
                        stack.translate(0.5, -0.2, -0.2);
                        stack.mulPose(Axis.XP.rotationDegrees(-80.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(60.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(30.0F));
                        break;
                    case GPT:
                        stack.translate(0.5, -0.3, -1.2);
                        stack.mulPose(Axis.XP.rotationDegrees(-45.0F));
                        stack.mulPose(Axis.YP.rotationDegrees(90.0F));
                        stack.mulPose(Axis.ZP.rotationDegrees(0.0F));
                        stack.translate(0.0, -0.5 * this.f, 0.0);
                        stack.mulPose(Axis.XP.rotationDegrees(-20.0F * this.f));
                        stack.mulPose(Axis.YP.rotationDegrees(5.0F * this.f));
                        stack.mulPose(Axis.ZP.rotationDegrees(10.0F * this.f));
                        stack.scale(1.0F + 0.1F * this.f, 1.0F + 0.1F * this.f, 1.0F + 0.1F * this.f);
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void transformItem(PoseStack stack, float equipProgress, float swingProgress) {
        stack.translate(0.0, 0.0, -0.72);
        stack.translate(0.0, equipProgress * -0.6, 0.0);
        stack.mulPose(Axis.YP.rotationDegrees(45.0F));
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        stack.mulPose(Axis.XP.rotationDegrees(f1 * -40.0F));
        stack.mulPose(Axis.YP.rotationDegrees(f * -10.0F));
        stack.mulPose(Axis.ZP.rotationDegrees(f1 * -10.0F));
    }

    private float boAnimate(float d) {
        if (d < 0.1) {
            return 1.0F - d * 10.0F;
        } else {
            float d2 = 1.0F - (d - 0.1F) / 0.9F;
            return 1.0F - d2 * d2 * d2;
        }
    }

    private boolean getArmor(Player entity) {
        for (int i = 0; i < 4; i++) {
            if (!entity.getInventory().getArmor(i).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean canTeleport(Entity entity) {
        double distance = BlackOut.mc.player.distanceTo(entity);
        return !(distance > this.maxPackets.get() * this.maxDistance.get()) && this.raycastCheck(entity.getBoundingBox());
    }

    private List<Vec3> getPath(Entity entity) {
        Vec3 diff = entity.position().subtract(BlackOut.mc.player.position());
        List<Vec3> path = new ArrayList<>();

        for (int i = 1; i <= this.maxPackets.get(); i++) {
            double delta = i / this.maxPackets.get().floatValue();
            path.add(BlackOut.mc.player.position().add(diff.scale(delta)));
        }

        return path;
    }

    private boolean raycastCheck(AABB f) {
        f = f.deflate(0.06);
        AABB b = BlackOut.mc.player.getBoundingBox().deflate(0.06);
        return this.raycast(new Vec3(b.minX, b.minY, b.minZ), new Vec3(f.minX, f.minY, f.minZ))
                && this.raycast(new Vec3(b.maxX, b.minY, b.minZ), new Vec3(f.maxX, f.minY, f.minZ))
                && this.raycast(new Vec3(b.minX, b.minY, b.maxZ), new Vec3(f.minX, f.minY, f.maxZ))
                && this.raycast(new Vec3(b.maxX, b.minY, b.maxZ), new Vec3(f.maxX, f.minY, f.maxZ))
                && this.raycast(new Vec3(b.minX, b.maxY, b.minZ), new Vec3(f.minX, f.maxY, f.minZ))
                && this.raycast(new Vec3(b.maxX, b.maxY, b.minZ), new Vec3(f.maxX, f.maxY, f.minZ))
                && this.raycast(new Vec3(b.minX, b.maxY, b.maxZ), new Vec3(f.minX, f.maxY, f.maxZ))
                && this.raycast(new Vec3(b.maxX, b.maxY, b.maxZ), new Vec3(f.maxX, f.maxY, f.maxZ));
    }

    private boolean raycast(Vec3 from, Vec3 to) {
        ((IClipContext) DamageUtils.raycastContext).blackout_Client$set(from, to);
        return DamageUtils.raycast(DamageUtils.raycastContext, false).getType() == HitResult.Type.MISS;
    }

    public enum BlockMode {
        Spam,
        Hold,
        Fake
    }

    public enum BlockRenderMode {
        BlackOut,
        KassuK,
        KassuK2,
        KassuK3,
        Disabled,
        Fan,
        Retarded,
        Float,
        Slap,
        GPT
    }

    public enum DelayMode {
        Basic,
        Smart,
        Vanilla
    }

    public enum RenderMode {
        None,
        Hit,
        Always
    }

    public enum RotationMode {
        OnHit,
        Constant
    }

    public enum TargetMode {
        Health,
        Angle,
        Distance
    }

    public enum WeaponPreference {
        Default("Default", "Uses current damage-based selection"),
        Damage("Damage", "Prioritizes highest damage weapon"),
        Sword("Sword", "Prefers swords over other weapons"),
        Axe("Axe", "Prefers axes over other weapons"),
        Mace("Mace", "Prefers maces over other weapons"),
        Trident("Trident", "Prefers tridents over other weapons");

        private final String name;
        private final String description;

        WeaponPreference(String name, String description) {
            this.name = name;
            this.description = description;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public String getDescription() {
            return this.description;
        }
    }
}
