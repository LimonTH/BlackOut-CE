package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ParticleManager extends Manager {
    private final TimerList<Particle> particles = new TimerList<>(true);

    private static int alphaMulti(int c, double alpha) {
        int r = ColorHelper.Argb.getRed(c);
        int g = ColorHelper.Argb.getGreen(c);
        int b = ColorHelper.Argb.getBlue(c);
        int a = ColorHelper.Argb.getAlpha(c);
        int alp = (int) Math.round(a * alpha);
        return (alp & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.world == null || BlackOut.mc.player == null);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.particles.forEach(timer -> timer.value.tick());
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        Vec3d cameraPos = BlackOut.mc.gameRenderer.getCamera().getPos();
        MatrixStack stack = Render3DUtils.matrices;
        stack.push();
        Render3DUtils.setRotation(stack);
        GlStateManager._disableDepthTest();
        GlStateManager._enableBlend();
        GlStateManager._disableCull();
        this.particles
                .forEach(
                        timer -> timer.value
                                .render(
                                        this.calcAlpha(MathHelper.clamp((System.currentTimeMillis() - timer.startTime) / 1000.0 / timer.time, 0.0, 1.0)), stack, cameraPos
                                )
                );
        stack.pop();
    }

    private float calcAlpha(double delta) {
        if (delta < 0.1) {
            return (float) (delta * 10.0);
        } else {
            return delta > 0.5 ? (float) (1.0 - (delta - 0.5) * 2.0) : 1.0F;
        }
    }

    public void addBouncy(Vec3d pos, Vec3d motion, double time, int color, int shadowColor) {
        this.particles.add(new BouncyParticle(pos, motion, color, shadowColor), time);
    }

    public void addFriction(Vec3d pos, Vec3d motion, double friction, double time, int color, int shadowColor) {
        this.particles.add(new FrictionParticle(pos, motion, friction, color, shadowColor), time);
    }

    private interface Particle {
        void tick();

        void render(double alpha, MatrixStack stack, Vec3d cameraPos);
    }

    private static class BouncyParticle implements Particle {
        private final int color;
        private final int shadowColor;
        private Vec3d pos;
        private Vec3d prev;
        private double motionX;
        private double motionY;
        private double motionZ;

        private BouncyParticle(Vec3d pos, Vec3d motion, int color, int shadowColor) {
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
            Box box = Box.of(this.pos, 0.05, 0.05, 0.05);
            if (OLEPOSSUtils.inside(BlackOut.mc.player, box.stretch(this.motionX, 0.0, 0.0))) {
                this.motionX = this.doTheBounciness(this.motionX);
            }

            if (OLEPOSSUtils.inside(BlackOut.mc.player, box.stretch(0.0, this.motionY, 0.0))) {
                this.motionY = this.doTheBounciness(this.motionY);
            }

            if (OLEPOSSUtils.inside(BlackOut.mc.player, box.stretch(0.0, 0.0, this.motionZ))) {
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
        public void render(double alpha, MatrixStack stack, Vec3d cameraPos) {
            double x = MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.prev.x, this.pos.x) - cameraPos.x;
            double y = MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.prev.y, this.pos.y) - cameraPos.y;
            double z = MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.prev.z, this.pos.z) - cameraPos.z;
            stack.push();
            stack.translate(x, y, z);
            stack.scale(0.02F, 0.02F, 0.02F);
            stack.multiply(BlackOut.mc.gameRenderer.getCamera().getRotation());
            stack.push();
            RenderUtils.rounded(
                    stack, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 5.0F, ParticleManager.alphaMulti(this.color, alpha), ParticleManager.alphaMulti(this.shadowColor, alpha)
            );
            stack.pop();
            stack.pop();
        }
    }

    private static class FrictionParticle implements Particle {
        private final double friction;
        private final int color;
        private final int shadowColor;
        private Vec3d pos;
        private Vec3d prev;
        private Vec3d motion;

        private FrictionParticle(Vec3d pos, Vec3d motion, double friction, int color, int shadowColor) {
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
            this.pos = this.pos.add(this.motion = this.motion.multiply(this.friction));
        }

        @Override
        public void render(double alpha, MatrixStack stack, Vec3d cameraPos) {
            double x = MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.prev.x, this.pos.x) - cameraPos.x;
            double y = MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.prev.y, this.pos.y) - cameraPos.y;
            double z = MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.prev.z, this.pos.z) - cameraPos.z;
            stack.push();
            stack.translate(x, y, z);
            stack.scale(0.02F, 0.02F, 0.02F);
            stack.multiply(BlackOut.mc.gameRenderer.getCamera().getRotation());
            stack.push();
            RenderUtils.rounded(
                    stack, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 5.0F, ParticleManager.alphaMulti(this.color, alpha), ParticleManager.alphaMulti(this.shadowColor, alpha)
            );
            stack.pop();
            stack.pop();
        }
    }
}
