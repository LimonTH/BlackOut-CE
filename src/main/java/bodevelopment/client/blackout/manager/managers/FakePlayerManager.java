package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.DamageUtils;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class FakePlayerManager extends Manager {
    public final List<FakePlayerEntity> fakePlayers = new ArrayList<>();
    private final List<FakePlayerEntity.PlayerPos> recorded = new ArrayList<>();
    private boolean recording = false;

    public static FakePlayerEntity.PlayerPos getPlayerPos(PlayerEntity player) {
        return new FakePlayerEntity.PlayerPos(
                player.getPos(),
                player.getVelocity(),
                player.getPose(),
                player.getPitch(),
                player.getYaw(),
                player.getHeadYaw(),
                player.getBodyYaw()
        );
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ExplosionS2CPacket packet) {
            this.fakePlayers.forEach(entity -> {
                Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
                Box box = entity.getBoundingBox();
                double q = 12.0;
                double dist = BoxUtils.feet(box).distanceTo(pos) / q;
                if (!(dist > 1.0)) {
                    double aa = DamageUtils.getExposure(pos, box, null);
                    double ab = (1.0 - dist) * aa;
                    float damage = (int) ((ab * ab + ab) * 3.5 * q + 1.0);
                    BlackOut.mc.execute(() -> entity.damage(BlackOut.mc.player.getDamageSources().explosion(null, null), damage));
                }
            });
        }
    }

    public void onAttack(FakePlayerEntity player) {
        this.playHitSound(player);
        player.damage(BlackOut.mc.player.getDamageSources().playerAttack(BlackOut.mc.player), this.getDamage(player));
    }

    private void playHitSound(FakePlayerEntity target) {
        if (!(this.getDamage(target) <= 0.0F) && !target.isDead()) {
            boolean bl = BlackOut.mc.player.getAttackCooldownProgress(0.5F) > 0.9F;
            boolean sprintHit = BlackOut.mc.player.isSprinting() && bl;
            if (sprintHit) {
                BlackOut.mc
                        .world
                        .playSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                                BlackOut.mc.player.getSoundCategory(),
                                1.0F,
                                1.0F,
                                true
                        );
            }

            boolean critical = bl
                    && BlackOut.mc.player.fallDistance > 0.0F
                    && !BlackOut.mc.player.isOnGround()
                    && !BlackOut.mc.player.isClimbing()
                    && !BlackOut.mc.player.isTouchingWater()
                    && !BlackOut.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                    && !BlackOut.mc.player.hasVehicle()
                    && !BlackOut.mc.player.isSprinting();
            double d = BlackOut.mc.player.horizontalSpeed - BlackOut.mc.player.prevHorizontalSpeed;
            boolean bl42 = bl
                    && !critical
                    && !sprintHit
                    && BlackOut.mc.player.isOnGround()
                    && d < BlackOut.mc.player.getMovementSpeed()
                    && BlackOut.mc.player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof SwordItem;
            if (bl42) {
                BlackOut.mc
                        .world
                        .playSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                                BlackOut.mc.player.getSoundCategory(),
                                1.0F,
                                1.0F,
                                true
                        );
            } else if (!critical) {
                SoundEvent soundEvent = bl ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG : SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;
                BlackOut.mc
                        .world
                        .playSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                soundEvent,
                                BlackOut.mc.player.getSoundCategory(),
                                1.0F,
                                1.0F,
                                true
                        );
            }

            if (critical) {
                BlackOut.mc
                        .world
                        .playSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                                BlackOut.mc.player.getSoundCategory(),
                                1.0F,
                                1.0F,
                                true
                        );
            }
        } else {
            BlackOut.mc
                    .world
                    .playSound(
                            BlackOut.mc.player.getX(),
                            BlackOut.mc.player.getY(),
                            BlackOut.mc.player.getZ(),
                            SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE,
                            BlackOut.mc.player.getSoundCategory(),
                            1.0F,
                            1.0F,
                            true
                    );
        }
    }

    private float getDamage(Entity target) {
        float baseDamage = (float) BlackOut.mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);

        DamageSource playerAttackSource = BlackOut.mc.player.getDamageSources().playerAttack(BlackOut.mc.player);

        float enchantmentDamage = EnchantmentHelper.getDamage(
                null,
                BlackOut.mc.player.getMainHandStack(),
                target,
                playerAttackSource,
                0.0F
        );

        float cooldown = BlackOut.mc.player.getAttackCooldownProgress(0.5F);

        float currentDamage = baseDamage * (0.2F + cooldown * cooldown * 0.8F);

        if (isCrit(cooldown, target)) {
            currentDamage *= 1.5F;
        }

        return currentDamage + (enchantmentDamage * cooldown);
    }

    private boolean isCrit(float cooldown, Entity target) {
        return cooldown > 0.9F
                && BlackOut.mc.player.fallDistance > 0.0F
                && !BlackOut.mc.player.isOnGround()
                && !BlackOut.mc.player.isClimbing()
                && !BlackOut.mc.player.isTouchingWater()
                && !BlackOut.mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !BlackOut.mc.player.hasVehicle()
                && target instanceof LivingEntity
                && !BlackOut.mc.player.isSprinting();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (this.recording && BlackOut.mc.player != null) {
            FakePlayerEntity.PlayerPos playerPos = new FakePlayerEntity.PlayerPos(
                    BlackOut.mc.player.getPos(),
                    BlackOut.mc.player.getVelocity(),
                    BlackOut.mc.player.getPose(),
                    BlackOut.mc.player.getPitch(),
                    BlackOut.mc.player.getYaw(),
                    BlackOut.mc.player.getHeadYaw(),
                    BlackOut.mc.player.bodyYaw
            );
            this.recorded.add(playerPos);

            if (this.recorded.size() > 1200) {
                this.endRecording();
            }
        }
    }

    public void restart() {
        this.fakePlayers.forEach(player -> player.progress = 0);
    }

    public void startRecording() {
        this.recorded.clear();
        this.recording = true;
    }

    public void endRecording() {
        this.recording = false;
    }

    public void add(String name) {
        List<FakePlayerEntity.PlayerPos> copy = new ArrayList<>(this.recorded);
        FakePlayerEntity player = new FakePlayerEntity(name, copy);

        this.recorded.clear();
        this.recording = false;
        this.fakePlayers.add(player);
    }

    public void clear() {
        this.fakePlayers.removeIf(player -> {
            player.remove(Entity.RemovalReason.DISCARDED);
            return true;
        });
    }
}
