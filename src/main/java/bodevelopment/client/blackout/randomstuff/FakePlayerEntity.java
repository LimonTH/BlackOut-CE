package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.managers.FakePlayerManager;
import bodevelopment.client.blackout.module.modules.client.settings.FakeplayerSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FakePlayerEntity extends AbstractClientPlayerEntity {
    private final List<PlayerPos> positions = new ArrayList<>();
    private final Random random = new Random();
    private final String name;
    public int progress;
    public int popped = 0;
    public int sinceSwap = 0;
    private PlayerPos currentPlayerPos = null;
    private int sinceEat = 0;

    public FakePlayerEntity(String name, List<PlayerPos> recordedPositions) {
        super(BlackOut.mc.world, new GameProfile(Uuids.getOfflinePlayerUuid(name), name));

        this.positions.addAll(recordedPositions);
        this.progress = 0;

        EntityAttributeInstance stepAttr = BlackOut.mc.player.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT);
        if (stepAttr != null) {
            stepAttr.setBaseValue(1.0);
        }
        this.noClip = true;
        this.name = name;
        PlayerPos ownPos = FakePlayerManager.getPlayerPos(BlackOut.mc.player);
        this.updatePosition(ownPos);
        this.updatePosition(ownPos);
        Byte playerModel = BlackOut.mc.player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
        this.dataTracker.set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
        this.getAttributes().setFrom(BlackOut.mc.player.getAttributes());
        this.cloneInv(BlackOut.mc.player.getInventory());
        this.setHealth(20.0F);
        this.setAbsorptionAmount(16.0F);
        this.unsetRemoved();

        BlackOut.mc.world.addEntity(this);

    }

    private void cloneInv(PlayerInventory inventory) {
        PlayerInventory ownInventory = this.getInventory();

        for (int i = 0; i < ownInventory.size(); i++) {
            ownInventory.setStack(i, inventory.getStack(i).copy());
        }

        ownInventory.selectedSlot = inventory.selectedSlot;
    }

    public boolean damage(DamageSource source, float amount) {
        try {
            return this.innerDamage(source, amount);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean innerDamage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (this.getAbilities().invulnerable && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            amount *= FakeplayerSettings.getInstance().damageMultiplier.get().floatValue();
            this.despawnCounter = 0;
            if (!this.isDead() && !(amount < 0.0F)) {
                if (source.isScaledWithDifficulty()) {
                    if (this.getEntityWorld().getDifficulty() == Difficulty.EASY) {
                        amount = Math.min(amount / 2.0F + 1.0F, amount);
                    }

                    if (this.getEntityWorld().getDifficulty() == Difficulty.HARD) {
                        amount = amount * 3.0F / 2.0F;
                    }
                }

                if (amount == 0.0F) {
                    return false;
                } else if (this.isInvulnerableTo(source)) {
                    return false;
                } else if (this.isDead()) {
                    return false;
                } else if (source.isIn(DamageTypeTags.IS_FIRE) && this.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                    return false;
                } else {
                    this.despawnCounter = 0;
                    boolean bl = false;
                    if (amount > 0.0F && this.blockedByShield(source)) {
                        this.damageShield(amount);
                        amount = 0.0F;
                        if (!source.isIn(DamageTypeTags.IS_PROJECTILE) && source.getSource() instanceof LivingEntity livingEntity) {
                            this.takeShieldHit(livingEntity);
                        }

                        bl = true;
                    }

                    if (source.isIn(DamageTypeTags.IS_FREEZING) && this.getType().isIn(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                        amount *= 5.0F;
                    }

                    this.limbAnimator.setSpeed(1.5F);
                    boolean bl2 = true;
                    if (this.timeUntilRegen > 10 && !source.isIn(DamageTypeTags.BYPASSES_COOLDOWN)) {
                        if (amount <= this.lastDamageTaken) {
                            return false;
                        }

                        this.applyDamage(source, amount - this.lastDamageTaken);
                        this.lastDamageTaken = amount;
                        bl2 = false;
                    } else {
                        this.lastDamageTaken = amount;
                        this.timeUntilRegen = 20;
                        this.applyDamage(source, amount);
                        this.maxHurtTime = 10;
                        this.hurtTime = this.maxHurtTime;
                    }

                    if (source.isIn(DamageTypeTags.DAMAGES_HELMET) && !this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                        this.damageHelmet(source, amount);
                        amount *= 0.75F;
                    }

                    Entity attacker = source.getAttacker();
                    if (attacker instanceof LivingEntity livingEntity && !source.isIn(DamageTypeTags.NO_ANGER)) {
                        this.setAttacker(livingEntity);
                    }

                    if (attacker instanceof PlayerEntity player) {
                        this.playerHitTimer = 100;
                        this.attackingPlayer = player;
                    } else if (attacker instanceof WolfEntity wolf && wolf.isTamed()) {
                        this.playerHitTimer = 100;
                        this.attackingPlayer = wolf.getOwner() instanceof PlayerEntity owner ? owner : null;
                    }

                    if (this.isDead()) {
                        if (!this.tryUseTotem(source)) {
                            SoundEvent soundEvent = this.getDeathSound();
                            if (bl2 && soundEvent != null) {
                                this.playSound(soundEvent, this.getSoundVolume(), this.getSoundPitch());
                            }
                        } else {
                            BlackOut.mc.particleManager.addEmitter(this, ParticleTypes.TOTEM_OF_UNDYING, 30);
                            BlackOut.mc
                                    .world
                                    .playSound(
                                            this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_TOTEM_USE, this.getSoundCategory(), 1.0F, 1.0F, true
                                    );
                        }
                    } else if (bl2) {
                        this.hurtTime = 10;
                        this.maxHurtTime = 10;
                        this.lastDamageTaken = amount;

                        this.getWorld().sendEntityStatus(this, (byte) 2);

                        SoundEvent soundEvent = this.getHurtSound(source);
                        if (soundEvent != null) {
                            this.playSound(soundEvent, this.getSoundVolume(), this.getSoundPitch());
                        }
                    }

                    boolean bl3 = !bl || amount > 0.0F;
                    if (bl3) {
                        this.lastDamageSource = source;
                        this.lastDamageTime = this.getEntityWorld().getTime();
                    }

                    return bl3;
                }
            } else {
                return false;
            }
        }
    }

    public boolean shouldRender(double distance) {
        return true;
    }

    @Override
    public void tick() {
        this.sinceEat++;
        FakeplayerSettings fakeplayerSettings = FakeplayerSettings.getInstance();

        if (!this.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            if (++this.sinceSwap > fakeplayerSettings.swapDelay.get()
                    && (this.popped < fakeplayerSettings.totems.get() || fakeplayerSettings.unlimitedTotems.get())) {
                this.swapToOffhand();
            }
        } else {
            this.sinceSwap = 0;
        }

        if (fakeplayerSettings.eating.get()) {
            if (!this.getMainHandStack().isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                this.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 64));
            }

            this.setCurrentHand(Hand.MAIN_HAND);

            if (this.sinceEat >= fakeplayerSettings.eatTime.get()) {
                this.consumeItem();
                this.sinceEat = 0;
            } else {
                if (this.sinceEat % 4 == 0) {
                    this.spawnConsumptionEffects(this.getMainHandStack(), 5);
                    this.playSound(this.getEatSound(this.getMainHandStack()), 0.5F + 0.5F * (float) this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }
            }
        } else {
            if (this.isUsingItem()) {
                this.stopUsingItem();
            }
            this.sinceEat = 0;
        }

        super.tick();
    }

    public void consumeItem() {
        ItemStack itemStack = this.getMainHandStack().finishUsing(this.getWorld(), this);

        if (itemStack != this.getMainHandStack()) {
            this.setStackInHand(Hand.MAIN_HAND, itemStack);
        }
    }

    public ItemStack getStack(World world, ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);

        if (food != null) {
            world.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    this.getEatSound(stack),
                    SoundCategory.NEUTRAL,
                    1.0F,
                    1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F
            );

            this.applyFoodEffects(stack);

            if (!this.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return stack;
    }

    public boolean clearStatusEffects() {
        if (this.getWorld().isClient) return false;

        return super.clearStatusEffects();
    }

    protected void onStatusEffectRemoved(StatusEffectInstance effect) {
        super.onStatusEffectRemoved(effect);

        effect.getEffectType().value().onRemoved(this.getAttributes());

        this.updateAttributes();
    }

    private void applyFoodEffects(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food != null) {
            for (FoodComponent.StatusEffectEntry entry : food.effects()) {
                if (this.random.nextFloat() < entry.probability()) {
                    this.addStatusEffect(new StatusEffectInstance(entry.effect()));
                }
            }
        }
    }

    private void swapToOffhand() {
        this.setStackInHand(Hand.OFF_HAND, Items.TOTEM_OF_UNDYING.getDefaultStack());
        this.sinceSwap = 0;
        this.popped++;
    }

    @Override
    public void tickMovement() {
        double dx = this.getX();
        double dz = this.getZ();

        this.tickRecord();

        dx = this.getX() - dx;
        dz = this.getZ() - dz;
        float distanceMoved = (float) Math.sqrt(dx * dx + dz * dz);

        this.prevStrideDistance = this.strideDistance;

        this.updateLimbs(distanceMoved > 0.01F);
        this.limbAnimator.updateLimbs(distanceMoved * 4.0F, 0.4F);

        this.tickHandSwing();

        float f = (this.isOnGround() && !this.isDead()) ? (float) Math.min(0.1, distanceMoved) : 0.0F;
        this.strideDistance = this.strideDistance + (f - this.strideDistance) * 0.4F;

        super.tickMovement();
    }

    public GameProfile getGameProfile() {
        return new GameProfile(Uuids.getOfflinePlayerUuid(this.name), this.name);
    }

    private void updatePosition(PlayerPos playerPos) {
        this.currentPlayerPos = playerPos;
        this.setPosition(this.currentPlayerPos.vec());
        this.setRotation(this.currentPlayerPos.yaw(), this.currentPlayerPos.pitch());
        this.setVelocity(this.currentPlayerPos.velocity());
        this.headYaw = this.currentPlayerPos.headYaw();
        this.bodyYaw = this.currentPlayerPos.bodyYaw();
    }

    public void tickRecord() {
        if (this.positions.isEmpty() || this.isDead()) return;

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

    public record PlayerPos(Vec3d vec, Vec3d velocity, EntityPose pose, float pitch, float yaw, float headYaw,
                            float bodyYaw) {
    }
}
