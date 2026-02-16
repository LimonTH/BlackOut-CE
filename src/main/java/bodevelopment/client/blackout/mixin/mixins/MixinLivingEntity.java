package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.movement.ElytraFly;
import bodevelopment.client.blackout.module.modules.movement.NoJumpDelay;
import bodevelopment.client.blackout.module.modules.movement.Speed;
import bodevelopment.client.blackout.module.modules.visual.entities.PlayerModifier;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Shadow
    public abstract void remove(Entity.RemovalReason reason);

    @Shadow
    public abstract void jump();

    @Inject(method = "getLeaningPitch", at = @At("HEAD"), cancellable = true)
    private void injectLeaning(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            PlayerModifier playerModifier = PlayerModifier.getInstance();
            if (playerModifier.enabled && playerModifier.setLeaning.get()) {
                cir.setReturnValue(playerModifier.getLeaning(player));
            }
        }
    }

    @ModifyConstant(method = "tickMovement", constant = @Constant(intValue = 10))
    private int modifyJumpDelay(int constant) {
        return NoJumpDelay.getInstance().enabled ? 0 : constant;
    }

    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float sprintJumpYaw(LivingEntity instance) {
        return instance == BlackOut.mc.player && SettingUtils.grimMovement() ? Managers.ROTATION.moveLookYaw : instance.getYaw();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 0))
    private float yaw1(LivingEntity instance) {
        return (Object) this != BlackOut.mc.player ? instance.getYaw() : this.getModifiedYaw(instance);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F", ordinal = 1))
    private float yaw2(LivingEntity instance) {
        return (Object) this != BlackOut.mc.player ? instance.getYaw() : this.getModifiedYaw(instance);
    }

    @Redirect(method = "turnHead", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float yaw(LivingEntity instance) {
        return (Object) this != BlackOut.mc.player ? instance.getYaw() : this.getModifiedYaw(instance);
    }

    @Unique
    private float getModifiedYaw(LivingEntity livingEntity) {
        return livingEntity == BlackOut.mc.player && Managers.ROTATION.yawActive() ? Managers.ROTATION.renderYaw : livingEntity.getYaw();
    }

    @Redirect(method = "getMovementSpeed(F)F", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getMovementSpeed()F"))
    private float vanillaSpeed(LivingEntity instance) {
        if ((Object) this != BlackOut.mc.player) {
            return instance.getMovementSpeed();
        } else {
            Speed speed = Speed.getInstance();
            return speed.enabled && speed.mode.get() == Speed.SpeedMode.Vanilla
                    ? speed.vanillaSpeed.get().floatValue() * instance.getMovementSpeed()
                    : instance.getMovementSpeed();
        }
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
    private float redirectElytraPitch(LivingEntity instance) {
        if ((Object) this == BlackOut.mc.player) {
            if (SettingUtils.grimMovement()) {
                return Managers.ROTATION.nextPitch;
            }

            ElytraFly elytraFly = ElytraFly.getInstance();
            if (elytraFly.enabled && elytraFly.isBouncing()) {
                return elytraFly.getPitch();
            }
        }

        return instance.getPitch();
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d redirectRotationVec(LivingEntity instance) {
        if ((Object) this == BlackOut.mc.player) {
            SettingUtils.grimMovement();
        }
        return instance.getRotationVector();
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;jump()V"))
    private void onJump(LivingEntity instance) {
        if ((Object) this == BlackOut.mc.player) {
            ElytraFly elytraFly = ElytraFly.getInstance();
            if (!elytraFly.enabled || !elytraFly.isBouncing()) {
                this.jump();
            }
        }
    }
}
