package bodevelopment.client.blackout.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Simulator {
    private static final double MAX_WATER_SPEED = 0.197;
    private static final double MAX_LAVA_SPEED = 0.0753;

    public static Box extrapolate(SimulationContext ctx) {
        ctx.onGround = ctx.isOnGround();
        ctx.prevOnGround = ctx.onGround;

        for (int i = 0; i < ctx.ticks; i++) {
            next(ctx, i);
        }

        return ctx.box;
    }

    private static void next(SimulationContext ctx, int i) {
        preMove(ctx);
        ctx.onTick.accept(ctx, i);
        handleMotion(ctx);
        handleCollisions(ctx);
        postMove(ctx);
        ctx.accept();
    }

    private static void handleMotion(SimulationContext ctx) {
        ctx.inWater = OLEPOSSUtils.inWater(ctx.box);
        ctx.inLava = OLEPOSSUtils.inLava(ctx.box);
        if (ctx.inFluid()) {
            ctx.jump = false;
        }

        if (!ctx.originalLava && ctx.inLava) {
            handleFluidMotion(ctx, 0.1, MAX_LAVA_SPEED);
        } else if (!ctx.originalWater && ctx.inWater) {
            handleFluidMotion(ctx, 0.04, MAX_WATER_SPEED);
        } else {
            handleRecover(ctx);
        }
    }

    private static void handleRecover(SimulationContext ctx) {
        approachMotionXZ(ctx, 0.05, ctx.originalMotion.horizontalLength());
    }

    private static void handleFluidMotion(SimulationContext ctx, double xz, double targetXZ) {
        approachMotionXZ(ctx, xz, targetXZ);
    }

    private static void approachMotionXZ(SimulationContext ctx, double xz, double targetXZ) {
        double length = Math.sqrt(ctx.motionX * ctx.motionX + ctx.motionZ * ctx.motionZ);
        double next;
        if (targetXZ > length) {
            next = Math.min(length + xz, targetXZ);
        } else {
            next = Math.max(length - xz, targetXZ);
        }

        if (length <= 0.0) {
            length = 0.03;
        }

        double ratio = next / length;
        ctx.motionX *= ratio;
        ctx.motionZ *= ratio;
    }

    private static void preMove(SimulationContext ctx) {
        ctx.prevOnGround = ctx.onGround;
        ctx.onGround = ctx.isOnGround();
        if (ctx.onGround && ctx.jump && ctx.motionY <= 0.0) {
            ctx.motionY = ctx.jumpHeight;
        } else if (ctx.onGround) {
            ctx.motionY = 0.0;
        }
    }

    private static void postMove(SimulationContext ctx) {
        if (ctx.inFluid() && !ctx.inFluidOriginal()) {
            ctx.motionY = MathHelper.clamp(ctx.motionY, -0.0784, 0.13);
        }

        if (ctx.inFluidOriginal() && ctx.inFluid()) {
            ctx.motionY *= 0.99;
        } else {
            ctx.motionY = (ctx.motionY - (ctx.inFluid() ? 0.005 : 0.08)) * 0.98;
        }
    }

    private static void handleCollisions(SimulationContext ctx) {
        ctx.updateCollisions();
        Vec3d movement = new Vec3d(ctx.motionX, shouldReverse(ctx) ? -ctx.reverseStep : ctx.motionY, ctx.motionZ);
        Vec3d collidedMovement = movement.lengthSquared() == 0.0 ? movement : ctx.collide(movement, ctx.box);
        boolean collidedHorizontally = collidedMovement.x != ctx.motionX || collidedMovement.z != ctx.motionZ;
        boolean collidingWithFloor = ctx.motionY < 0.0 && collidedMovement.y != ctx.motionY;
        if ((ctx.onGround || collidingWithFloor) && collidedHorizontally) {
            Vec3d vec2 = ctx.collide(new Vec3d(ctx.motionX, ctx.step, ctx.motionZ), ctx.box);
            Vec3d vec3 = ctx.collide(new Vec3d(0.0, ctx.step, 0.0), ctx.box.stretch(ctx.motionX, 0.0, ctx.motionZ));
            if (vec3.y < ctx.step) {
                Vec3d vec4 = ctx.collide(new Vec3d(movement.x, 0.0, movement.z), ctx.box.offset(vec3)).add(vec3);
                if (vec4.horizontalLengthSquared() > vec2.horizontalLengthSquared()) {
                    vec2 = vec4;
                }
            }

            if (vec2.horizontalLengthSquared() > collidedMovement.horizontalLengthSquared()) {
                Vec3d vec = vec2.add(ctx.collide(new Vec3d(0.0, -vec2.y + movement.y, 0.0), ctx.box.offset(vec2)));
                ctx.move(vec);
                ctx.setOnGround(true);
                return;
            }
        }

        ctx.move(collidedMovement);
    }

    private static boolean shouldReverse(SimulationContext ctx) {
        return ctx.reverseStep > 0.0
                && ctx.prevOnGround
                && !ctx.onGround
                && ctx.motionY <= 0.0
                && OLEPOSSUtils.inside(ctx.entity, ctx.box.stretch(0.0, -ctx.reverseStep, 0.0));
    }

    public static boolean isOnGround(Entity entity, Box box) {
        return OLEPOSSUtils.inside(entity, box.stretch(0.0, -0.02, 0.0));
    }
}
