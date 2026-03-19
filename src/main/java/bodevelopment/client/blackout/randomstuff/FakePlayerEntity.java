package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.managers.FakePlayerManager;
import bodevelopment.client.blackout.module.modules.client.settings.FakeplayerSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakePlayerEntity extends AbstractClientPlayer {
    private final List<PlayerPos> positions = new ArrayList<>();
    private final RandomSource random = this.level().getRandom();
    private final String name;
    public int progress;
    public int popped = 0;
    public int sinceSwap = 0;
    private PlayerPos currentPlayerPos = null;
    private int sinceEat = 0;

    public FakePlayerEntity(String name, List<PlayerPos> recordedPositions) {
        super(BlackOut.mc.level, new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name));

        this.positions.addAll(recordedPositions);
        this.progress = 0;

        AttributeInstance stepAttr = this.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(1.0);
        }
        this.noPhysics = true;
        this.name = name;
        PlayerPos ownPos = FakePlayerManager.getPlayerPos(BlackOut.mc.player);
        this.updatePosition(ownPos);
        this.updatePosition(ownPos);
        Byte playerModel = BlackOut.mc.player.getEntityData().get(Player.DATA_PLAYER_MODE_CUSTOMISATION);
        this.entityData.set(Player.DATA_PLAYER_MODE_CUSTOMISATION, playerModel);
        this.getAttributes().assignAllValues(BlackOut.mc.player.getAttributes());
        this.cloneInv(BlackOut.mc.player.getInventory());
        this.setHealth(20.0F);
        this.setAbsorptionAmount(16.0F);
        this.unsetRemoved();

        BlackOut.mc.level.addEntity(this);

    }

    private void cloneInv(Inventory inventory) {
        Inventory ownInventory = this.getInventory();

        for (int i = 0; i < ownInventory.getContainerSize(); i++) {
            ownInventory.setItem(i, inventory.getItem(i).copy());
        }

        ownInventory.setSelectedSlot(inventory.getSelectedSlot());
    }

    public boolean damage(DamageSource source, float amount) {
        try {
            return this.innerDamage(source, amount);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean innerDamage(DamageSource source, float amount) {
        if (this.level() instanceof ServerLevel serverWorld && this.isInvulnerableTo(serverWorld, source)) {
            return false;
        } else if (this.getAbilities().invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            amount *= FakeplayerSettings.getInstance().damageMultiplier.get().floatValue();
            this.noActionTime = 0;
            if (!this.isDeadOrDying() && !(amount < 0.0F)) {
                if (source.scalesWithDifficulty()) {
                    if (this.level().getDifficulty() == Difficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (this.level().getDifficulty() == Difficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                if (amount == 0.0F) {
                    return false;
                } else if (this.level() instanceof ServerLevel serverWorld && this.isInvulnerableTo(serverWorld, source)) {
                    return false;
                } else if (this.isDeadOrDying()) {
                    return false;
                } else if (source.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    return false;
                } else {
                    this.noActionTime = 0;
                    boolean bl = false;
                    if (amount > 0.0F && this.isBlocking()) {
                        amount = 0.0F;
                        bl = true;
                    }

                    if (source.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                        amount *= 5.0F;
                    }

                    this.walkAnimation.setSpeed(1.5F);
                    boolean bl2 = true;
                    if (this.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                        if (amount <= this.lastHurt) {
                            return false;
                        }
                        if (this.level() instanceof ServerLevel serverWorld) {
                            this.actuallyHurt(serverWorld, source, amount - this.lastHurt);
                        }
                        this.lastHurt = amount;
                        bl2 = false;
                    } else {
                        this.lastHurt = amount;
                        this.invulnerableTime = 20;
                        if (this.level() instanceof ServerLevel serverWorld) {
                            this.actuallyHurt(serverWorld, source, amount);
                        }
                        this.hurtDuration = 10;
                        this.hurtTime = this.hurtDuration;
                    }

                    if (source.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                        this.hurtHelmet(source, amount);
                        amount *= 0.75F;
                    }

                    Entity attacker = source.getEntity();
                    if (attacker instanceof LivingEntity livingEntity && !source.is(DamageTypeTags.NO_ANGER)) {
                        this.setLastHurtByMob(livingEntity);
                    }

                    if (attacker instanceof Player player) {
                        this.lastHurtByPlayerMemoryTime = 100;
                        this.lastHurtByPlayer = new EntityReference<>(player);
                    } else if (attacker instanceof Wolf wolf && wolf.isTame()) {
                        this.lastHurtByPlayerMemoryTime = 100;
                        Entity ownerEntity = wolf.getOwnerReference() != null ? wolf.getOwnerReference().getEntity(this.level(), LivingEntity.class) : null;
                        this.lastHurtByPlayer = ownerEntity instanceof Player p ? new EntityReference<>(p) : null;
                    }

                    if (this.isDeadOrDying()) {
                        if (!this.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                            this.setItemInHand(InteractionHand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultInstance());
                            this.sinceSwap = 0;
                            this.popped++;
                        }

                        if (!this.checkTotemDeathProtection(source)) {
                            SoundEvent soundEvent = this.getDeathSound();
                            if (bl2 && soundEvent != null) {
                                this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
                            }
                        } else {
                            BlackOut.mc.particleEngine.createTrackingEmitter(this, ParticleTypes.TOTEM_OF_UNDYING, 30);
                            BlackOut.mc
                                    .level
                                    .playLocalSound(
                                            this.getX(), this.getY(), this.getZ(), SoundEvents.TOTEM_USE, this.getSoundSource(), 1.0F, 1.0F, true
                                    );
                        }
                    } else if (bl2) {
                        this.hurtTime = 10;
                        this.hurtDuration = 10;
                        this.lastHurt = amount;

                        this.level().broadcastEntityEvent(this, (byte) 2);

                        SoundEvent soundEvent = this.getHurtSound(source);
                        if (soundEvent != null) {
                            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
                        }
                    }

                    boolean bl3 = !bl || amount > 0.0F;
                    if (bl3) {
                        this.lastDamageSource = source;
                        this.lastDamageStamp = this.level().getGameTime();
                    }

                    return bl3;
                }
            } else {
                return false;
            }
        }
    }

    private boolean checkTotemDeathProtection(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        ItemStack totemStack = null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = this.getItemInHand(hand);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                totemStack = stack.copy();
                if (!this.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                break;
            }
        }

        if (totemStack != null) {
            this.setHealth(1.0F);
            this.removeAllEffects();

            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));

            this.level().broadcastEntityEvent(this, (byte) 35);

            this.popped++;

            return true;
        }

        return false;
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        this.sinceEat++;
        FakeplayerSettings fakeplayerSettings = FakeplayerSettings.getInstance();

        // Логика тотемов (остается без изменений, если методы swapToOffhand на месте)
        if (!this.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            if (fakeplayerSettings.swapDelay.get() == 0) {
                if (this.popped < fakeplayerSettings.totems.get() || fakeplayerSettings.unlimitedTotems.get()) {
                    this.swapToOffhand();
                }
            } else {
                if (++this.sinceSwap > fakeplayerSettings.swapDelay.get()
                        && (this.popped < fakeplayerSettings.totems.get() || fakeplayerSettings.unlimitedTotems.get())) {
                    this.swapToOffhand();
                }
            }
        } else {
            this.sinceSwap = 0;
        }

        // Логика поедания
        if (fakeplayerSettings.eating.get()) {
            ItemStack handStack = this.getMainHandItem();
            if (!handStack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                handStack = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64);
                this.setItemInHand(InteractionHand.MAIN_HAND, handStack);
            }

            this.startUsingItem(InteractionHand.MAIN_HAND);

            if (this.sinceEat >= fakeplayerSettings.eatTime.get()) {
                this.completeUsingItem();
                this.sinceEat = 0;
            } else {
                if (this.sinceEat % 4 == 0) {
                    Consumable consumable = handStack.get(DataComponents.CONSUMABLE);
                    if (consumable != null) {
                        consumable.emitParticlesAndSounds(this.random, this, handStack, 5);
                    }
                }
            }
        } else {
            if (this.isUsingItem()) {
                this.releaseUsingItem();
            }
            this.sinceEat = 0;
        }

        super.tick();
    }

    public void completeUsingItem() {
        ItemStack itemStack = this.getMainHandItem().finishUsingItem(this.level(), this);

        if (itemStack != this.getMainHandItem()) {
            this.setItemInHand(InteractionHand.MAIN_HAND, itemStack);
        }
    }

    public ItemStack getStack(Level world, ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);

        if (food != null) {
            Consumable consumable = stack.get(DataComponents.CONSUMABLE);

            SoundEvent sound = SoundEvents.GENERIC_EAT.value();

            if (consumable != null) {
                sound = consumable.sound().value();
            }

            world.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    sound,
                    SoundSource.NEUTRAL,
                    1.0F,
                    1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F
            );

            this.applyFoodEffects(stack);

            if (!this.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    public boolean removeAllEffects() {
        if (this.level().isClientSide) return false;

        return super.removeAllEffects();
    }


    protected void onEffectsRemoved(Collection<MobEffectInstance> effects) {
        super.onEffectsRemoved(effects);

        for (MobEffectInstance effect : effects) {
            effect.getEffect().value().removeAttributeModifiers(this.getAttributes());
        }

        this.refreshDirtyAttributes();
    }

    private void applyFoodEffects(ItemStack stack) {
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            for (ConsumeEffect effect : consumable.onConsumeEffects()) {
                if (effect instanceof ApplyStatusEffectsConsumeEffect applyEffects) {
                    for (MobEffectInstance effectInstance : applyEffects.effects()) {
                        this.addEffect(new MobEffectInstance(effectInstance));
                    }
                }
            }
        }
    }

    private void swapToOffhand() {
        this.setItemInHand(InteractionHand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultInstance());
        this.sinceSwap = 0;
        this.popped++;
    }

    @Override
    public void aiStep() {
        double dx = this.getX();
        double dz = this.getZ();

        this.tickRecord();

        dx = this.getX() - dx;
        dz = this.getZ() - dz;
        float distanceMoved = (float) Math.sqrt(dx * dx + dz * dz);

        this.oBob = this.bob;

        this.calculateEntityAnimation(distanceMoved > 0.01F);
        this.walkAnimation.update(distanceMoved * 4.0F, 0.4F, 1.0F);

        this.updateSwingTime();

        float f = (this.onGround() && !this.isDeadOrDying()) ? (float) Math.min(0.1, distanceMoved) : 0.0F;
        this.bob = this.bob + (f - this.bob) * 0.4F;

        super.aiStep();
    }

    public GameProfile getGameProfile() {
        return new GameProfile(UUIDUtil.createOfflinePlayerUUID(this.name), this.name);
    }

    private void updatePosition(PlayerPos playerPos) {
        this.currentPlayerPos = playerPos;
        this.setPos(this.currentPlayerPos.vec());
        this.setRot(this.currentPlayerPos.yaw(), this.currentPlayerPos.pitch());
        this.setDeltaMovement(this.currentPlayerPos.velocity());
        this.yHeadRot = this.currentPlayerPos.headYaw();
        this.yBodyRot = this.currentPlayerPos.bodyYaw();
    }

    public void tickRecord() {
        if (this.positions.isEmpty() || this.isDeadOrDying()) return;

        if (this.progress >= this.positions.size()) {
            this.progress = 0;
        }

        if (this.progress >= 0) {
            PlayerPos playerPos = this.positions.get(this.progress);
            this.updatePosition(playerPos);

            this.progress++;
        }
    }

    public void set(List<PlayerPos> positions) {
        this.positions.clear();
        this.positions.addAll(positions);
        this.progress = 0;
    }

    public record PlayerPos(Vec3 vec, Vec3 velocity, Pose pose, float pitch, float yaw, float headYaw,
                            float bodyYaw) {
    }
}
