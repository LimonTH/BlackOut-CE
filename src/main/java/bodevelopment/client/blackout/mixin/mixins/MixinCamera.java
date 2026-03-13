package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.module.modules.visual.misc.CameraModifier;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.modules.visual.misc.Spectate;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.client.Camera;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    private boolean initialized;
    @Shadow
    private BlockGetter level;
    @Shadow
    private Entity entity;
    @Shadow
    private boolean detached;
    @Shadow
    private float yRot;
    @Shadow
    private float xRot;
    @Shadow
    private float eyeHeightOld;
    @Shadow
    private float eyeHeight;
    @Shadow
    private float partialTickTime;
    @Unique
    private long prevTime = 0L;
    @Unique
    private Vec3 prevPos = Vec3.ZERO;

    @Shadow
    protected abstract float getMaxZoom(float distance);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void move(float x, float y, float z);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract float getXRot();

    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    private void cameraClip(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        ci.cancel();
        CameraModifier modifier = CameraModifier.getInstance();
        Spectate spectate = Spectate.getInstance();
        FreeCam freecam = FreeCam.getInstance();
        this.initialized = true;
        this.level = area;
        this.entity = focusedEntity;
        this.detached = thirdPerson;
        this.partialTickTime = tickDelta;
        double delta = (System.currentTimeMillis() - this.prevTime) / 1000.0;
        this.prevTime = System.currentTimeMillis();
        this.setRotation(focusedEntity.getViewYRot(tickDelta), focusedEntity.getViewXRot(tickDelta));
        this.setPosition(
                Mth.lerp(tickDelta, focusedEntity.xo, focusedEntity.getX()),
                Mth.lerp(tickDelta, focusedEntity.yo, focusedEntity.getY())
                        + Mth.lerp(tickDelta, this.eyeHeightOld, this.eyeHeight),
                Mth.lerp(tickDelta, focusedEntity.zo, focusedEntity.getZ())
        );
        if (modifier.enabled) {
            modifier.updateDistance(thirdPerson, delta);
        }

        Entity spectateEntity = spectate != null && spectate.enabled ? spectate.getEntity() : null;
        if (!freecam.enabled) {
            freecam.pos = this.getPosition();
        }

        boolean movedPrev;
        if (modifier.enabled) {
            if (modifier.shouldSmooth(thirdPerson)) {
                movedPrev = true;
                this.movePos(this.getPosition(), delta, modifier);
            } else {
                movedPrev = false;
            }

            Vec3 pos = this.getPosition();
            this.setPosition(
                    pos.x(),
                    modifier.lockY.get() ? Mth.clamp(pos.y(), modifier.minY.get(), modifier.maxY.get()) : pos.y(),
                    pos.z()
            );
        } else {
            movedPrev = false;
        }

        if (!movedPrev) {
            this.prevPos = this.getPosition();
        }

        if (!freecam.enabled) {
            ((IVec3d) freecam.velocity).blackout_Client$set(0.0, 0.0, 0.0);
        }

        if (spectateEntity != null) {
            this.setPosition(
                    OLEPOSSUtils.getLerpedPos(spectateEntity, tickDelta).add(0.0, spectateEntity.getEyeHeight(spectateEntity.getPose()), 0.0)
            );
            this.setRotation(spectateEntity.getViewYRot(tickDelta), spectateEntity.getViewXRot(tickDelta));
        } else if (freecam.enabled) {
            this.setPosition(freecam.getPos(this.getYRot(), this.getXRot()));
        } else if (thirdPerson) {
            if (inverseView) {
                this.setRotation(this.yRot + 180.0F, -this.xRot);
            }

            float distance = modifier.enabled ? (float) modifier.getCameraDistance() : 4.0F;
            this.move(-(modifier.clip.get() && modifier.enabled ? distance : this.getMaxZoom(distance)), 0.0F, 0.0F);
        } else if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
            Direction direction = ((LivingEntity) focusedEntity).getBedOrientation();
            this.setRotation(direction != null ? direction.toYRot() - 180.0F : 0.0F, 0.0F);
            this.move(0.0F, 0.3F, 0.0F);
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void onIsThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.getInstance().enabled) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void movePos(Vec3 to, double delta, CameraModifier modifier) {
        double dist = this.prevPos.distanceTo(to);
        double movement = dist * modifier.smoothSpeed.get() * delta;
        double newDist = Mth.clamp(dist - movement, 0.0, dist);
        double f = dist == 0.0 && newDist == 0.0 ? 1.0 : newDist / dist;
        Vec3 offset = to.subtract(this.prevPos);
        Vec3 m = offset.scale(1.0 - f);
        this.prevPos = this.prevPos.add(m);
        this.setPosition(this.prevPos);
    }
}
