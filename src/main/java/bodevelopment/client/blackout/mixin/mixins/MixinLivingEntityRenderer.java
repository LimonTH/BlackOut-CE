package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.visual.entities.PlayerModifier;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Unique
    private boolean isLocalPlayer = false;

    @Inject(method = "updateRenderState*", at = @At("TAIL"))
    private void onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        this.isLocalPlayer = (entity == BlackOut.mc.player);

        if (this.isLocalPlayer) {
            PlayerModifier modifier = PlayerModifier.getInstance();

            if (Managers.ROTATION.yawActive()) {
                float headYaw = MathHelper.lerpAngleDegrees(tickDelta, Managers.ROTATION.prevRenderYaw, Managers.ROTATION.renderYaw);
                state.yawDegrees = MathHelper.wrapDegrees(headYaw - state.bodyYaw);
            }

            if (Managers.ROTATION.pitchActive()) {
                state.pitch = MathHelper.lerp(tickDelta, Managers.ROTATION.prevRenderPitch, Managers.ROTATION.renderPitch);
            }

            if (modifier.enabled) {
                if (modifier.noAnimations.get()) {
                    state.limbFrequency = 0.0F;
                    state.limbAmplitudeMultiplier = 0.0F;
                }

                if (modifier.noSwing.get() && state instanceof BipedEntityRenderState bipedState) {
                    bipedState.handSwingProgress = 0.0F;
                }
            }
        }
    }
}