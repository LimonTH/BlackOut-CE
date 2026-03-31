package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.movement.ElytraFly;
import bodevelopment.client.blackout.module.modules.movement.NoJumpDelay;
import bodevelopment.client.blackout.module.modules.movement.Speed;
import bodevelopment.client.blackout.module.modules.visual.entities.PlayerModifier;
import bodevelopment.client.blackout.util.CompatUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    public abstract void jumpFromGround();

    @Inject(method = "getSwimAmount", at = @At("HEAD"), cancellable = true)
    private void injectLeaning(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player) {
            PlayerModifier playerModifier = PlayerModifier.getInstance();
            if (playerModifier.enabled && playerModifier.setLeaning.get()) {
                cir.setReturnValue(playerModifier.getLeaning(player));
            }
        }
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(intValue = 10))
    private int modifyJumpDelay(int constant) {
        return NoJumpDelay.getInstance().enabled ? 0 : constant;
    }

    @WrapOperation(
            method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F")
    )
    private float wrapSprintJumpYaw(LivingEntity instance, Operation<Float> original) {
        if (instance != BlackOut.mc.player || !SettingUtils.grimMovement() || CompatUtils.isBaritonePathing()) {
            return original.call(instance);
        }
        return Managers.ROTATION.moveLookYaw;
    }
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F", ordinal = 0)
    )
    private float wrapYaw1(LivingEntity instance, Operation<Float> original) {
        return (Object) this != BlackOut.mc.player ? original.call(instance) : this.getModifiedYaw(instance, original);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F", ordinal = 1)
    )
    private float wrapYaw2(LivingEntity instance, Operation<Float> original) {
        return (Object) this != BlackOut.mc.player ? original.call(instance) : this.getModifiedYaw(instance, original);
    }

    @WrapOperation(
            method = "tickHeadTurn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F")
    )
    private float wrapTurnHeadYaw(LivingEntity instance, Operation<Float> original) {
        return (Object) this != BlackOut.mc.player ? original.call(instance) : this.getModifiedYaw(instance, original);
    }

    @Redirect(method = "getFrictionInfluencedSpeed(F)F", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getSpeed()F"))
    private float vanillaSpeed(LivingEntity instance) {
        if ((Object) this != BlackOut.mc.player) {
            return instance.getSpeed();
        } else {
            Speed speed = Speed.getInstance();
            return speed.enabled && speed.mode.get() == Speed.SpeedMode.Vanilla
                    ? speed.vanillaSpeed.get().floatValue() * instance.getSpeed()
                    : instance.getSpeed();
        }
    }

    @Redirect(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float redirectElytraPitch(LivingEntity instance) {
        if ((Object) this == BlackOut.mc.player && !CompatUtils.shouldBypassRotations()) {
            if (SettingUtils.grimMovement()) {
                return Managers.ROTATION.nextPitch;
            }

            ElytraFly elytraFly = ElytraFly.getInstance();
            if (elytraFly.enabled && elytraFly.isBouncing()) {
                return elytraFly.getPitch();
            }
        }

        return instance.getXRot();
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;jumpFromGround()V"))
    private void onJump(LivingEntity instance) {
        if ((Object) this == BlackOut.mc.player) {
            ElytraFly elytraFly = ElytraFly.getInstance();
            if (!elytraFly.enabled || !elytraFly.isBouncing()) {
                this.jumpFromGround();
            }
        }
    }

    @Unique
    private float getModifiedYaw(LivingEntity livingEntity, Operation<Float> original) {
        if (livingEntity == BlackOut.mc.player && !CompatUtils.isBaritonePathing() && Managers.ROTATION.yawActive()) {
            return Managers.ROTATION.renderYaw;
        }
        return original.call(livingEntity);
    }
}
