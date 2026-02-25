package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.module.modules.visual.misc.CameraModifier;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.modules.visual.misc.Spectate;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    private boolean ready;
    @Shadow
    private BlockView area;
    @Shadow
    private Entity focusedEntity;
    @Shadow
    private boolean thirdPerson;
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;
    @Shadow
    private float lastCameraY;
    @Shadow
    private float cameraY;
    @Shadow
    private float lastTickDelta;
    @Unique
    private long prevTime = 0L;
    @Unique
    private Vec3d prevPos = Vec3d.ZERO;

    @Shadow
    protected abstract float clipToSpace(float distance);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void moveBy(float x, float y, float z);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    public abstract Vec3d getPos();

    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void cameraClip(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        ci.cancel();
        CameraModifier modifier = CameraModifier.getInstance();
        Spectate spectate = Spectate.getInstance();
        FreeCam freecam = FreeCam.getInstance();
        this.ready = true;
        this.area = area;
        this.focusedEntity = focusedEntity;
        this.thirdPerson = thirdPerson;
        this.lastTickDelta = tickDelta;
        double delta = (System.currentTimeMillis() - this.prevTime) / 1000.0;
        this.prevTime = System.currentTimeMillis();
        this.setRotation(focusedEntity.getYaw(tickDelta), focusedEntity.getPitch(tickDelta));
        this.setPos(
                MathHelper.lerp(tickDelta, focusedEntity.prevX, focusedEntity.getX()),
                MathHelper.lerp(tickDelta, focusedEntity.prevY, focusedEntity.getY())
                        + MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY),
                MathHelper.lerp(tickDelta, focusedEntity.prevZ, focusedEntity.getZ())
        );
        if (modifier.enabled) {
            modifier.updateDistance(thirdPerson, delta);
        }

        Entity spectateEntity = spectate != null && spectate.enabled ? spectate.getEntity() : null;
        if (!freecam.enabled) {
            freecam.pos = this.getPos();
        }

        boolean movedPrev;
        if (modifier.enabled) {
            if (modifier.shouldSmooth(thirdPerson)) {
                movedPrev = true;
                this.movePos(this.getPos(), delta, modifier);
            } else {
                movedPrev = false;
            }

            Vec3d pos = this.getPos();
            this.setPos(
                    pos.getX(),
                    modifier.lockY.get() ? MathHelper.clamp(pos.getY(), modifier.minY.get(), modifier.maxY.get()) : pos.getY(),
                    pos.getZ()
            );
        } else {
            movedPrev = false;
        }

        if (!movedPrev) {
            this.prevPos = this.getPos();
        }

        if (!freecam.enabled) {
            ((IVec3d) freecam.velocity).blackout_Client$set(0.0, 0.0, 0.0);
        }

        if (spectateEntity != null) {
            this.setPos(
                    OLEPOSSUtils.getLerpedPos(spectateEntity, tickDelta).add(0.0, spectateEntity.getEyeHeight(spectateEntity.getPose()), 0.0)
            );
            this.setRotation(spectateEntity.getYaw(tickDelta), spectateEntity.getPitch(tickDelta));
        } else if (freecam.enabled) {
            this.setPos(freecam.getPos(this.getYaw(), this.getPitch()));
        } else if (thirdPerson) {
            if (inverseView) {
                this.setRotation(this.yaw + 180.0F, -this.pitch);
            }

            float distance = modifier.enabled ? (float) modifier.getCameraDistance() : 4.0F;
            this.moveBy(-(modifier.clip.get() && modifier.enabled ? distance : this.clipToSpace(distance)), 0.0F, 0.0F);
        } else if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
            Direction direction = ((LivingEntity) focusedEntity).getSleepingDirection();
            this.setRotation(direction != null ? direction.asRotation() - 180.0F : 0.0F, 0.0F);
            this.moveBy(0.0F, 0.3F, 0.0F);
        }
    }

    @Unique
    private void movePos(Vec3d to, double delta, CameraModifier modifier) {
        double dist = this.prevPos.distanceTo(to);
        double movement = dist * modifier.smoothSpeed.get() * delta;
        double newDist = MathHelper.clamp(dist - movement, 0.0, dist);
        double f = dist == 0.0 && newDist == 0.0 ? 1.0 : newDist / dist;
        Vec3d offset = to.subtract(this.prevPos);
        Vec3d m = offset.multiply(1.0 - f);
        this.prevPos = this.prevPos.add(m);
        this.setPos(this.prevPos);
    }
}
