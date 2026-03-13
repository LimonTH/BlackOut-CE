package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.entities.PlayerModifier;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        boolean isLocalPlayer = (entity == BlackOut.mc.player);

        if (isLocalPlayer) {
            PlayerModifier modifier = PlayerModifier.getInstance();

            if (Managers.ROTATION.yawActive()) {
                float headYaw = Mth.rotLerp(tickDelta, Managers.ROTATION.prevRenderYaw, Managers.ROTATION.renderYaw);
                state.yRot = Mth.wrapDegrees(headYaw - state.bodyRot);
            }

            if (Managers.ROTATION.pitchActive()) {
                state.xRot = Mth.lerp(tickDelta, Managers.ROTATION.prevRenderPitch, Managers.ROTATION.renderPitch);
            }

            if (modifier.enabled) {
                if (modifier.noAnimations.get()) {
                    state.walkAnimationPos = 0.0F;
                    state.walkAnimationSpeed = 0.0F;
                }

                if (modifier.noSwing.get() && state instanceof HumanoidRenderState bipedState) {
                    bipedState.attackTime = 0.0F;
                }
            }
        }
    }
}