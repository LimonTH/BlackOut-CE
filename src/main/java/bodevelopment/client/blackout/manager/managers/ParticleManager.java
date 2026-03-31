package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ParticleManager extends Manager {
    private final TimerList<Particle> particles = new TimerList<>(true);

    private static int alphaMulti(int c, double alpha) {
        int r = ARGB.red(c);
        int g = ARGB.green(c);
        int b = ARGB.blue(c);
        int a = ARGB.alpha(c);
        int alp = (int) Math.round(a * alpha);
        return (alp & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.level == null || BlackOut.mc.player == null);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.particles.forEach(timer -> timer.value.tick());
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        Vec3 cameraPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();
        PoseStack stack = Render3DUtils.matrices;
        stack.pushPose();
        Render3DUtils.setRotation(stack);
        GlStateManager._disableDepthTest();
        GlStateManager._enableBlend();
        GlStateManager._disableCull();
        this.particles
                .forEach(
                        timer -> timer.value
                                .render(
                                        this.calcAlpha(Mth.clamp((System.currentTimeMillis() - timer.startTime) / 1000.0 / timer.time, 0.0, 1.0)), stack, cameraPos
                                )
                );
        stack.popPose();
    }

    private float calcAlpha(double delta) {
        if (delta < 0.1) {
            return (float) (delta * 10.0);
        } else {
            return delta > 0.5 ? (float) (1.0 - (delta - 0.5) * 2.0) : 1.0F;
        }
    }

    public void addBouncy(Vec3 pos, Vec3 motion, double time, int color, int shadowColor) {
        this.particles.add(new BouncyParticle(pos, motion, color, shadowColor), time);
    }

    public void addFriction(Vec3 pos, Vec3 motion, double friction, double time, int color, int shadowColor) {
        this.particles.add(new FrictionParticle(pos, motion, friction, color, shadowColor), time);
    }

    private interface Particle {
        void tick();

        void render(double alpha, PoseStack stack, Vec3 cameraPos);
    }

    private static class BouncyParticle implements Particle {
        private final int color;
        private final int shadowColor;
        private Vec3 pos;
        private Vec3 prev;
        private double motionX;
        private double motionY;
        private double motionZ;

        private BouncyParticle(Vec3 pos, Vec3 motion, int color, int shadowColor) {
            this.pos = pos;
            this.prev = pos;
            this.motionX = motion.x;
            this.motionY = motion.y;
            this.motionZ = motion.z;
            this.color = color;
            this.shadowColor = shadowColor;
            this.tick();
        }

        @Override
        public void tick() {
            this.prev = this.pos;
            AABB box = AABB.ofSize(this.pos, 0.05, 0.05, 0.05);
            if (BlockUtils.hasEntityCollision(BlackOut.mc.player, box.expandTowards(this.motionX, 0.0, 0.0))) {
                this.motionX = this.doTheBounciness(this.motionX);
            }

            if (BlockUtils.hasEntityCollision(BlackOut.mc.player, box.expandTowards(0.0, this.motionY, 0.0))) {
                this.motionY = this.doTheBounciness(this.motionY);
            }

            if (BlockUtils.hasEntityCollision(BlackOut.mc.player, box.expandTowards(0.0, 0.0, this.motionZ))) {
                this.motionZ = this.doTheBounciness(this.motionZ);
            }

            this.pos = this.pos.add(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.98;
            this.motionZ *= 0.98;
            this.motionY = (this.motionY - 0.08) * 0.98;
        }

        private double doTheBounciness(double motion) {
            return motion * -0.7;
        }

        @Override
        public void render(double alpha, PoseStack stack, Vec3 cameraPos) {
            double x = Mth.lerp(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), this.prev.x, this.pos.x) - cameraPos.x;
            double y = Mth.lerp(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), this.prev.y, this.pos.y) - cameraPos.y;
            double z = Mth.lerp(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), this.prev.z, this.pos.z) - cameraPos.z;
            stack.pushPose();
            stack.translate(x, y, z);
            stack.scale(0.02F, 0.02F, 0.02F);
            stack.mulPose(BlackOut.mc.gameRenderer.getMainCamera().rotation());
            stack.pushPose();
            Render2DUtils.rounded(
                    stack, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 5.0F, ParticleManager.alphaMulti(this.color, alpha), ParticleManager.alphaMulti(this.shadowColor, alpha)
            );
            stack.popPose();
            stack.popPose();
        }
    }

    private static class FrictionParticle implements Particle {
        private final double friction;
        private final int color;
        private final int shadowColor;
        private Vec3 pos;
        private Vec3 prev;
        private Vec3 motion;

        private FrictionParticle(Vec3 pos, Vec3 motion, double friction, int color, int shadowColor) {
            this.pos = pos;
            this.prev = pos;
            this.motion = motion;
            this.friction = friction;
            this.color = color;
            this.shadowColor = shadowColor;
            this.tick();
        }

        @Override
        public void tick() {
            this.prev = this.pos;
            this.pos = this.pos.add(this.motion = this.motion.scale(this.friction));
        }

        @Override
        public void render(double alpha, PoseStack stack, Vec3 cameraPos) {
            double x = Mth.lerp(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), this.prev.x, this.pos.x) - cameraPos.x;
            double y = Mth.lerp(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), this.prev.y, this.pos.y) - cameraPos.y;
            double z = Mth.lerp(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), this.prev.z, this.pos.z) - cameraPos.z;
            stack.pushPose();
            stack.translate(x, y, z);
            stack.scale(0.02F, 0.02F, 0.02F);
            stack.mulPose(BlackOut.mc.gameRenderer.getMainCamera().rotation());
            stack.pushPose();
            Render2DUtils.rounded(
                    stack, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 5.0F, ParticleManager.alphaMulti(this.color, alpha), ParticleManager.alphaMulti(this.shadowColor, alpha)
            );
            stack.popPose();
            stack.popPose();
        }
    }
}
