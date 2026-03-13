package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.DamageUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FakePlayerManager extends Manager {
    public final List<FakePlayerEntity> fakePlayers = new ArrayList<>();
    private final List<FakePlayerEntity.PlayerPos> recorded = new ArrayList<>();
    private boolean recording = false;

    public static FakePlayerEntity.PlayerPos getPlayerPos(Player player) {
        return new FakePlayerEntity.PlayerPos(
                player.position(),
                player.getDeltaMovement(),
                player.getPose(),
                player.getXRot(),
                player.getYRot(),
                player.getYHeadRot(),
                player.getVisualRotationYInDegrees()
        );
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundExplodePacket packet) {
            this.fakePlayers.forEach(entity -> {
                Vec3 pos = new Vec3(packet.center().x(), packet.center().y(), packet.center().z());
                AABB box = entity.getBoundingBox();
                double q = 12.0;
                double dist = BoxUtils.feet(box).distanceTo(pos) / q;
                if (!(dist > 1.0)) {
                    double aa = DamageUtils.getExposure(pos, box, null);
                    double ab = (1.0 - dist) * aa;
                    float damage = (int) ((ab * ab + ab) * 3.5 * q + 1.0);
                    BlackOut.mc.execute(() -> entity.damage(BlackOut.mc.player.damageSources().explosion(null, null), damage));
                }
            });
        }
    }

    public void onAttack(FakePlayerEntity player) {
        this.playHitSound(player);
        player.damage(BlackOut.mc.player.damageSources().playerAttack(BlackOut.mc.player), this.getDamage(player));
    }

    private void playHitSound(FakePlayerEntity target) {
        if (!(this.getDamage(target) <= 0.0F) && !target.isDeadOrDying()) {
            boolean bl = BlackOut.mc.player.getAttackStrengthScale(0.5F) > 0.9F;
            boolean sprintHit = BlackOut.mc.player.isSprinting() && bl;
            if (sprintHit) {
                BlackOut.mc
                        .level
                        .playLocalSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                                BlackOut.mc.player.getSoundSource(),
                                1.0F,
                                1.0F,
                                true
                        );
            }

            boolean critical = bl
                    && BlackOut.mc.player.fallDistance > 0.0F
                    && !BlackOut.mc.player.onGround()
                    && !BlackOut.mc.player.onClimbable()
                    && !BlackOut.mc.player.isInWater()
                    && !BlackOut.mc.player.hasEffect(MobEffects.BLINDNESS)
                    && !BlackOut.mc.player.isPassenger()
                    && !BlackOut.mc.player.isSprinting();
            double horizontalSpeed = BlackOut.mc.player.getDeltaMovement().horizontalDistance();
            double prevHorizontalSpeed = Math.sqrt(
                    Math.pow(BlackOut.mc.player.getX() - BlackOut.mc.player.xo, 2) +
                            Math.pow(BlackOut.mc.player.getZ() - BlackOut.mc.player.zo, 2)
            );
            double d = horizontalSpeed - prevHorizontalSpeed;
            boolean bl42 = bl
                    && !critical
                    && !sprintHit
                    && BlackOut.mc.player.onGround()
                    && d < BlackOut.mc.player.getSpeed()
                    && BlackOut.mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof SwordItem;
            if (bl42) {
                BlackOut.mc
                        .level
                        .playLocalSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                SoundEvents.PLAYER_ATTACK_SWEEP,
                                BlackOut.mc.player.getSoundSource(),
                                1.0F,
                                1.0F,
                                true
                        );
            } else if (!critical) {
                SoundEvent soundEvent = bl ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK;
                BlackOut.mc
                        .level
                        .playLocalSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                soundEvent,
                                BlackOut.mc.player.getSoundSource(),
                                1.0F,
                                1.0F,
                                true
                        );
            }

            if (critical) {
                BlackOut.mc
                        .level
                        .playLocalSound(
                                BlackOut.mc.player.getX(),
                                BlackOut.mc.player.getY(),
                                BlackOut.mc.player.getZ(),
                                SoundEvents.PLAYER_ATTACK_CRIT,
                                BlackOut.mc.player.getSoundSource(),
                                1.0F,
                                1.0F,
                                true
                        );
            }
        } else {
            BlackOut.mc
                    .level
                    .playLocalSound(
                            BlackOut.mc.player.getX(),
                            BlackOut.mc.player.getY(),
                            BlackOut.mc.player.getZ(),
                            SoundEvents.PLAYER_ATTACK_NODAMAGE,
                            BlackOut.mc.player.getSoundSource(),
                            1.0F,
                            1.0F,
                            true
                    );
        }
    }

    private float getDamage(Entity target) {
        float damage = (float) DamageUtils.itemDamage(BlackOut.mc.player.getMainHandItem());

        float cooldown = BlackOut.mc.player.getAttackStrengthScale(0.5F);
        float currentDamage = damage * (0.2F + cooldown * cooldown * 0.8F);

        if (isCrit(cooldown, target)) {
            currentDamage *= 1.5F;
        }

        return currentDamage;
    }

    private boolean isCrit(float cooldown, Entity target) {
        return cooldown > 0.9F
                && BlackOut.mc.player.fallDistance > 0.0F
                && !BlackOut.mc.player.onGround()
                && !BlackOut.mc.player.onClimbable()
                && !BlackOut.mc.player.isInWater()
                && !BlackOut.mc.player.hasEffect(MobEffects.BLINDNESS)
                && !BlackOut.mc.player.isPassenger()
                && target instanceof LivingEntity
                && !BlackOut.mc.player.isSprinting();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (this.recording && BlackOut.mc.player != null) {
            FakePlayerEntity.PlayerPos playerPos = new FakePlayerEntity.PlayerPos(
                    BlackOut.mc.player.position(),
                    BlackOut.mc.player.getDeltaMovement(),
                    BlackOut.mc.player.getPose(),
                    BlackOut.mc.player.getXRot(),
                    BlackOut.mc.player.getYRot(),
                    BlackOut.mc.player.getYHeadRot(),
                    BlackOut.mc.player.yBodyRot
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
