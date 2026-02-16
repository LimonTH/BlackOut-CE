package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.RemoveEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.module.modules.legit.HitCrystal;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.modules.movement.CollisionShrink;
import bodevelopment.client.blackout.module.modules.movement.Step;
import bodevelopment.client.blackout.module.modules.movement.TargetStrafe;
import bodevelopment.client.blackout.module.modules.movement.Velocity;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public abstract Box getBoundingBox();

    @Shadow
    public abstract World getEntityWorld();

    @Shadow
    public abstract boolean isOnGround();

    @Shadow
    public abstract float getStepHeight();

    @Shadow
    public abstract void readNbt(NbtCompound nbt);

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if ((Object) this == BlackOut.mc.player) {
            BlackOut.EVENT_BUS.post(MoveEvent.Pre.get(movement, movementType));
            TargetStrafe strafe = TargetStrafe.getInstance();
            if (strafe.enabled) {
                strafe.onMove(movement);
            }
        }
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void onMovePost(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        if ((Object) this == BlackOut.mc.player) {
            BlackOut.EVENT_BUS.post(MoveEvent.Post.get());
        }

        if (SettingUtils.grimPackets()) {
            HitCrystal.getInstance().onTick();
        }
    }

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F"))
    private float getMoveYaw(Entity instance) {
        if ((Object) this == BlackOut.mc.player) {
            SettingUtils.grimMovement();
        }
        return instance.getYaw();
    }

    @Inject(method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
    private void doStepStuff(Vec3d movement, CallbackInfoReturnable<Vec3d> cir) {
        if ((Object) this == BlackOut.mc.player) {
            Step step = Step.getInstance();
            if (step.isActive()) {
                cir.setReturnValue(this.getStep(step, movement));
                cir.cancel();
            }
        }
    }

    @Redirect(
            method = "adjustMovementForCollisions(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;")
    )
    private Box box(Entity instance) {
        return this.getBox();
    }

    @Unique
    private Vec3d getStep(Step step, Vec3d movement) {
        if (step.stepProgress > -1 && step.slow.get() && step.offsets != null) {
            if (movement.horizontalLengthSquared() <= 0.0) {
                ((IVec3d) movement).blackout_Client$setXZ(step.prevMovement.x, step.prevMovement.z);
            }

            step.prevMovement = movement.multiply(1.0);
        }

        Entity entity = (Entity) (Object) this;
        Box box = this.getBox();
        List<VoxelShape> list = this.getEntityWorld().getEntityCollisions(entity, box.stretch(movement));
        Vec3d vec3d = movement.lengthSquared() == 0.0 ? movement : Entity.adjustMovementForCollisions(entity, movement, box, this.getEntityWorld(), list);
        boolean collidedX = movement.x != vec3d.x;
        boolean collidedY = movement.y != vec3d.y;
        boolean collidedZ = movement.z != vec3d.z;
        boolean collidedHorizontally = collidedX || collidedZ;
        boolean bl4 = this.isOnGround() || collidedY && movement.y < 0.0;
        double vanillaHeight = step.stepMode.get() == Step.StepMode.Vanilla ? step.height.get() : this.getStepHeight();
        Vec3d stepMovement = Entity.adjustMovementForCollisions(
                entity, new Vec3d(movement.x, vanillaHeight, movement.z), box, this.getEntityWorld(), list
        );
        Vec3d stepMovementUp = Entity.adjustMovementForCollisions(
                entity, new Vec3d(0.0, vanillaHeight, 0.0), box.stretch(movement.x, 0.0, movement.z), this.getEntityWorld(), list
        );
        if (vanillaHeight > 0.0 && bl4 && collidedHorizontally && (!step.slow.get() || step.stepProgress < 0)) {
            if (stepMovementUp.y < vanillaHeight) {
                Vec3d vec3d4 = Entity.adjustMovementForCollisions(
                                entity, new Vec3d(movement.x, 0.0, movement.z), box.offset(stepMovementUp), this.getEntityWorld(), list
                        )
                        .add(stepMovementUp);
                if (vec3d4.horizontalLengthSquared() > stepMovement.horizontalLengthSquared()) {
                    stepMovement = vec3d4;
                }
            }

            if (stepMovement.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
                return stepMovement.add(
                        Entity.adjustMovementForCollisions(
                                entity, new Vec3d(0.0, -stepMovement.y + movement.y, 0.0), box.offset(stepMovement), this.getEntityWorld(), list
                        )
                );
            }
        }

        double height = step.height.get();
        stepMovement = Entity.adjustMovementForCollisions(entity, new Vec3d(movement.x, height, movement.z), box, this.getEntityWorld(), list);
        stepMovementUp = Entity.adjustMovementForCollisions(
                entity, new Vec3d(0.0, height, 0.0), box.stretch(movement.x, 0.0, movement.z), this.getEntityWorld(), list
        );
        if (height > 0.0
                && entity.isOnGround()
                && collidedHorizontally
                && (!step.slow.get() || step.stepProgress < 0 || step.offsets == null)
                && step.cooldownCheck()) {
            if (stepMovementUp.y < height) {
                Vec3d vec3d4 = Entity.adjustMovementForCollisions(
                                entity, new Vec3d(movement.x, 0.0, movement.z), box.offset(stepMovementUp), this.getEntityWorld(), list
                        )
                        .add(stepMovementUp);
                if (vec3d4.horizontalLengthSquared() > stepMovement.horizontalLengthSquared()) {
                    stepMovement = vec3d4;
                }
            }

            if (stepMovement.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
                Vec3d vec3d3 = stepMovement.add(
                        Entity.adjustMovementForCollisions(
                                entity, new Vec3d(0.0, -stepMovement.y + movement.y, 0.0), box.offset(stepMovement), this.getEntityWorld(), list
                        )
                );
                step.start(vec3d3.y);
                if (step.offsets != null) {
                    step.lastStep = System.currentTimeMillis();
                    if (!step.slow.get()) {
                        double y = 0.0;

                        for (double offset : step.offsets) {
                            y += offset;
                            BlackOut.mc
                                    .getNetworkHandler()
                                    .sendPacket(
                                            new PlayerMoveC2SPacket.PositionAndOnGround(
                                                    BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + y, BlackOut.mc.player.getZ(), false
                                            )
                                    );
                        }

                        return vec3d3;
                    }

                    step.stepProgress = 0;
                }
            }
        }

        if (step.stepProgress > -1 && step.slow.get() && step.offsets != null) {
            step.sinceStep = 0;
            if (step.useTimer.get()) {
                Timer.set(step.timer.get().floatValue());
                step.shouldResetTimer = true;
            }

            double h;
            if (step.stepProgress < step.offsets.length) {
                h = step.offsets[step.stepProgress];
                step.stepProgress++;
                stepMovement = Entity.adjustMovementForCollisions(entity, new Vec3d(movement.x, 0.0, movement.z), box, this.getEntityWorld(), list);
            } else {
                Vec3d m;
                if (step.stepMode.get() == Step.StepMode.UpdatedNCP) {
                    if (step.stepProgress == step.offsets.length) {
                        step.stepProgress++;
                        h = step.lastSlow;
                    } else {
                        h = 0.0;
                        step.stepProgress = -1;
                        step.offsets = null;
                    }

                    m = new Vec3d(0.0, 0.0, 0.0);
                } else {
                    h = step.lastSlow;
                    step.stepProgress = -1;
                    step.offsets = null;
                    m = movement.withAxis(Direction.Axis.Y, 0.0);
                }

                stepMovement = Entity.adjustMovementForCollisions(entity, m, box, this.getEntityWorld(), list);
            }

            return stepMovement.add(Entity.adjustMovementForCollisions(entity, new Vec3d(0.0, h, 0.0), box.offset(stepMovement), this.getEntityWorld(), list));
        } else {
            if (step.shouldResetTimer) {
                step.stepProgress = -1;
                step.offsets = null;
                Timer.reset();
                step.shouldResetTimer = false;
            }

            return vec3d;
        }
    }

    @Unique
    private Box getBox() {
        CollisionShrink shrink = CollisionShrink.getInstance();
        return shrink.enabled ? shrink.getBox(this.getBoundingBox()) : this.getBoundingBox();
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void pushAwayFromEntities(Entity entity, CallbackInfo ci) {
        if ((Object) this == BlackOut.mc.player) {
            Velocity velocity = Velocity.getInstance();
            if (velocity.enabled && velocity.entityPush.get() != Velocity.PushMode.Disabled) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(RemoveEvent.get((Entity) (Object) this, reason));
    }
}
