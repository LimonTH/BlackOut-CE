package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.RemoveEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.legit.HitCrystal;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.modules.movement.*;
import bodevelopment.client.blackout.util.SettingUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract Level getCommandSenderWorld();

    @Shadow
    public abstract boolean onGround();

    @Shadow
    public abstract float maxUpStep();

    @Shadow
    public abstract void load(CompoundTag nbt);

    @Shadow
    public abstract Vec3 calculateViewVector(float pitch, float yaw);

    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        if ((Object) this == BlackOut.mc.player) {
            BlackOut.EVENT_BUS.post(MoveEvent.Pre.get(movement, movementType));
            TargetStrafe strafe = TargetStrafe.getInstance();
            if (strafe.enabled) {
                strafe.onMove(movement);
            }
        }
    }

    @Inject(method = "move", at = @At("TAIL"))
    private void onMovePost(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        if ((Object) this == BlackOut.mc.player) {
            BlackOut.EVENT_BUS.post(MoveEvent.Post.get());
        }

        if (SettingUtils.grimPackets()) {
            HitCrystal.getInstance().onTick();
        }
    }

    @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float getMoveYaw(Entity instance) {
        if ((Object) this == BlackOut.mc.player) {
            SettingUtils.grimMovement();
        }
        return instance.getYRot();
    }

    @Inject(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void doStepStuff(Vec3 movement, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this != BlackOut.mc.player) return;

        Step step = Step.getInstance();
        HoleSnap holeSnap = HoleSnap.getInstance();
        TickShift tickShift = TickShift.getInstance();

        boolean isStepActive = step.enabled;
        boolean isHoleSnapActive = holeSnap.enabled && holeSnap.shouldStep();
        boolean isTickShiftActive = tickShift.enabled && tickShift.shouldStep();

        if (isStepActive || isHoleSnapActive || isTickShiftActive) {
            cir.setReturnValue(this.getStep(step, movement));
        }
    }

    @Redirect(
            method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;")
    )
    private AABB box(Entity instance) {
        return this.getBox();
    }

    @Unique
    private Vec3 getStep(Step step, Vec3 movement) {
        if (step.stepProgress > -1 && step.slow.get() && step.offsets != null) {
            if (movement.horizontalDistanceSqr() <= 0.0) {
                ((IVec3) movement).blackout_Client$setXZ(step.prevMovement.x, step.prevMovement.z);
            }

            step.prevMovement = movement.scale(1.0);
        }

        Entity entity = (Entity) (Object) this;
        AABB box = this.getBox();
        List<VoxelShape> list = this.getCommandSenderWorld().getEntityCollisions(entity, box.expandTowards(movement));
        Vec3 vec3d = movement.lengthSqr() == 0.0 ? movement : Entity.collideBoundingBox(entity, movement, box, this.getCommandSenderWorld(), list);
        boolean collidedX = movement.x != vec3d.x;
        boolean collidedY = movement.y != vec3d.y;
        boolean collidedZ = movement.z != vec3d.z;
        boolean collidedHorizontally = collidedX || collidedZ;
        boolean bl4 = this.onGround() || collidedY && movement.y < 0.0;
        double vanillaHeight = step.stepMode.get() == Step.StepMode.Vanilla ? step.height.get() : this.maxUpStep();
        Vec3 stepMovement = Entity.collideBoundingBox(
                entity, new Vec3(movement.x, vanillaHeight, movement.z), box, this.getCommandSenderWorld(), list
        );
        Vec3 stepMovementUp = Entity.collideBoundingBox(
                entity, new Vec3(0.0, vanillaHeight, 0.0), box.expandTowards(movement.x, 0.0, movement.z), this.getCommandSenderWorld(), list
        );
        if (vanillaHeight > 0.0 && bl4 && collidedHorizontally && (!step.slow.get() || step.stepProgress < 0)) {
            if (stepMovementUp.y < vanillaHeight) {
                Vec3 vec3d4 = Entity.collideBoundingBox(
                                entity, new Vec3(movement.x, 0.0, movement.z), box.move(stepMovementUp), this.getCommandSenderWorld(), list
                        )
                        .add(stepMovementUp);
                if (vec3d4.horizontalDistanceSqr() > stepMovement.horizontalDistanceSqr()) {
                    stepMovement = vec3d4;
                }
            }

            if (stepMovement.horizontalDistanceSqr() > vec3d.horizontalDistanceSqr()) {
                return stepMovement.add(
                        Entity.collideBoundingBox(
                                entity, new Vec3(0.0, -stepMovement.y + movement.y, 0.0), box.move(stepMovement), this.getCommandSenderWorld(), list
                        )
                );
            }
        }

        double height = step.height.get();
        stepMovement = Entity.collideBoundingBox(entity, new Vec3(movement.x, height, movement.z), box, this.getCommandSenderWorld(), list);
        stepMovementUp = Entity.collideBoundingBox(
                entity, new Vec3(0.0, height, 0.0), box.expandTowards(movement.x, 0.0, movement.z), this.getCommandSenderWorld(), list
        );
        if (height > 0.0
                && entity.onGround()
                && collidedHorizontally
                && (!step.slow.get() || step.stepProgress < 0 || step.offsets == null)
                && step.cooldownCheck()) {
            if (stepMovementUp.y < height) {
                Vec3 vec3d4 = Entity.collideBoundingBox(
                                entity, new Vec3(movement.x, 0.0, movement.z), box.move(stepMovementUp), this.getCommandSenderWorld(), list
                        )
                        .add(stepMovementUp);
                if (vec3d4.horizontalDistanceSqr() > stepMovement.horizontalDistanceSqr()) {
                    stepMovement = vec3d4;
                }
            }

            if (stepMovement.horizontalDistanceSqr() > vec3d.horizontalDistanceSqr()) {
                Vec3 vec3d3 = stepMovement.add(
                        Entity.collideBoundingBox(
                                entity, new Vec3(0.0, -stepMovement.y + movement.y, 0.0), box.move(stepMovement), this.getCommandSenderWorld(), list
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
                                    .getConnection()
                                    .send(
                                            new ServerboundMovePlayerPacket.Pos(
                                                    BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + y, BlackOut.mc.player.getZ(), false, BlackOut.mc.player.horizontalCollision)
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
                stepMovement = Entity.collideBoundingBox(entity, new Vec3(movement.x, 0.0, movement.z), box, this.getCommandSenderWorld(), list);
            } else {
                Vec3 m;
                if (step.stepMode.get() == Step.StepMode.UpdatedNCP) {
                    if (step.stepProgress == step.offsets.length) {
                        step.stepProgress++;
                        h = step.lastSlow;
                    } else {
                        h = 0.0;
                        step.stepProgress = -1;
                        step.offsets = null;
                    }

                    m = new Vec3(0.0, 0.0, 0.0);
                } else {
                    h = step.lastSlow;
                    step.stepProgress = -1;
                    step.offsets = null;
                    m = movement.with(Direction.Axis.Y, 0.0);
                }

                stepMovement = Entity.collideBoundingBox(entity, m, box, this.getCommandSenderWorld(), list);
            }

            return stepMovement.add(Entity.collideBoundingBox(entity, new Vec3(0.0, h, 0.0), box.move(stepMovement), this.getCommandSenderWorld(), list));
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

    @Inject(method = "getLookAngle()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void onGetRotationVector(CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this == BlackOut.mc.player) {
            if (SettingUtils.grimMovement()) {
                cir.setReturnValue(this.calculateViewVector(Managers.ROTATION.nextPitch, Managers.ROTATION.nextYaw));
                return;
            }

            ElytraFly elytraFly = ElytraFly.getInstance();
            if (elytraFly.enabled && elytraFly.isBouncing()) {
                cir.setReturnValue(this.calculateViewVector(elytraFly.getPitch(), this.getYRot()));
            }
        }
    }

    @Unique
    private AABB getBox() {
        CollisionShrink shrink = CollisionShrink.getInstance();
        return shrink.enabled ? shrink.getBox(this.getBoundingBox()) : this.getBoundingBox();
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
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
