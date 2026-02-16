package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.movement.ElytraFly;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput {
    @Shadow
    @Final
    private GameOptions settings;
    @Unique
    private boolean move = false;
    @Unique
    private int offset = 0;
    @Unique
    private boolean grim = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onMovement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        Managers.ROTATION.updateNext();
        Managers.ROTATION.moveLookYaw = MathHelper.wrapDegrees(Managers.ROTATION.nextYaw);
        this.grim = SettingUtils.grimMovement();
        float forward = this.getInput(this.settings.forwardKey.isPressed(), this.settings.backKey.isPressed());
        float strafing = this.getInput(this.settings.leftKey.isPressed(), this.settings.rightKey.isPressed());
        float yaw = this.inputYaw(forward, strafing);
        this.offset = Managers.ROTATION.updateMove(yaw, this.move);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyboardInput;getMovementMultiplier(ZZ)F", ordinal = 0))
    private float movementForward(boolean positive, boolean negative) {
        ElytraFly elytraFly = ElytraFly.getInstance();
        if (elytraFly.enabled && elytraFly.isBouncing()) {
            return 1.0F;
        } else {
            float forward = this.getInput(positive, negative);
            if (this.grim && this.move) {
                return switch (this.offset) {
                    case 0, 1, 7 -> 1.0F;
                    case 2, 6 -> 0.0F;
                    case 3, 4, 5 -> -1.0F;
                    default -> throw new IllegalStateException(
                            "Unexpected value in grim forward calculations: offset:" + this.offset + ", dir:" + Managers.ROTATION.moveLookYaw
                    );
                };
            } else {
                return forward;
            }
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/KeyboardInput;getMovementMultiplier(ZZ)F", ordinal = 1))
    private float movementStrafing(boolean positive, boolean negative) {
        ElytraFly elytraFly = ElytraFly.getInstance();
        if (elytraFly.enabled && elytraFly.isBouncing()) {
            return 0.0F;
        } else {
            float strafing = this.getInput(positive, negative);
            if (this.grim && this.move) {
                return switch (this.offset) {
                    case 0, 4 -> 0.0F;
                    case 1, 2, 3 -> -1.0F;
                    case 5, 6, 7 -> 1.0F;
                    default -> throw new IllegalStateException(
                            "Unexpected value in grim strafing calculations: offset:" + this.offset + ", dir:" + Managers.ROTATION.moveLookYaw
                    );
                };
            } else {
                return strafing;
            }
        }
    }

    @Unique
    private float inputYaw(float forward, float strafing) {
        float yaw = BlackOut.mc.player.getYaw();
        if (forward > 0.0F) {
            this.move = true;
            yaw += strafing > 0.0F ? -45.0F : (strafing < 0.0F ? 45.0F : 0.0F);
        } else if (forward < 0.0F) {
            this.move = true;
            yaw += strafing > 0.0F ? -135.0F : (strafing < 0.0F ? 135.0F : 180.0F);
        } else {
            this.move = strafing != 0.0F;
            yaw += strafing > 0.0F ? -90.0F : (strafing < 0.0F ? 90.0F : 0.0F);
        }

        return yaw;
    }

    @Unique
    private float getInput(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        } else {
            return positive ? 1.0F : -1.0F;
        }
    }
}
