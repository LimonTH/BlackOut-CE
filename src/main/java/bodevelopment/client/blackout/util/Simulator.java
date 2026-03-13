package bodevelopment.client.blackout.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Simulator {
    private static final double MAX_WATER_SPEED = 0.197;
    private static final double MAX_LAVA_SPEED = 0.0753;

    public static AABB extrapolate(SimulationContext ctx) {
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
        approachMotionXZ(ctx, 0.05, ctx.originalMotion.horizontalDistance());
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
            ctx.motionY = Mth.clamp(ctx.motionY, -0.0784, 0.13);
        }

        if (ctx.inFluidOriginal() && ctx.inFluid()) {
            ctx.motionY *= 0.99;
        } else {
            ctx.motionY = (ctx.motionY - (ctx.inFluid() ? 0.005 : 0.08)) * 0.98;
        }
    }

    private static void handleCollisions(SimulationContext ctx) {
        ctx.updateCollisions();
        Vec3 movement = new Vec3(ctx.motionX, shouldReverse(ctx) ? -ctx.reverseStep : ctx.motionY, ctx.motionZ);
        Vec3 collidedMovement = movement.lengthSqr() == 0.0 ? movement : ctx.collide(movement, ctx.box);
        boolean collidedHorizontally = collidedMovement.x != ctx.motionX || collidedMovement.z != ctx.motionZ;
        boolean collidingWithFloor = ctx.motionY < 0.0 && collidedMovement.y != ctx.motionY;
        if ((ctx.onGround || collidingWithFloor) && collidedHorizontally) {
            Vec3 vec2 = ctx.collide(new Vec3(ctx.motionX, ctx.step, ctx.motionZ), ctx.box);
            Vec3 vec3 = ctx.collide(new Vec3(0.0, ctx.step, 0.0), ctx.box.expandTowards(ctx.motionX, 0.0, ctx.motionZ));
            if (vec3.y < ctx.step) {
                Vec3 vec4 = ctx.collide(new Vec3(movement.x, 0.0, movement.z), ctx.box.move(vec3)).add(vec3);
                if (vec4.horizontalDistanceSqr() > vec2.horizontalDistanceSqr()) {
                    vec2 = vec4;
                }
            }

            if (vec2.horizontalDistanceSqr() > collidedMovement.horizontalDistanceSqr()) {
                Vec3 vec = vec2.add(ctx.collide(new Vec3(0.0, -vec2.y + movement.y, 0.0), ctx.box.move(vec2)));
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
                && OLEPOSSUtils.inside(ctx.entity, ctx.box.expandTowards(0.0, -ctx.reverseStep, 0.0));
    }

    public static boolean isOnGround(Entity entity, AABB box) {
        return OLEPOSSUtils.inside(entity, box.expandTowards(0.0, -0.02, 0.0));
    }
}
