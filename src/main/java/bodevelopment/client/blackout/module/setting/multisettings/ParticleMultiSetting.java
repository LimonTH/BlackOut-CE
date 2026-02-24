package bodevelopment.client.blackout.module.setting.multisettings;

import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class ParticleMultiSetting {
    private static final Function<Double, Vec3d> randomMotion = speed -> {
        double yaw = ThreadLocalRandom.current().nextDouble() * 2.0 * Math.PI;
        double pitch = ThreadLocalRandom.current().nextDouble() * 2.0 * Math.PI;
        double c = Math.abs(Math.cos(pitch));
        return new Vec3d(speed * Math.cos(yaw) * c, speed * -Math.sin(pitch), speed * Math.sin(yaw) * c);
    };
    public final Setting<ParticleMode> mode;
    public final Setting<Integer> particles;
    public final Setting<Double> velocity;
    public final Setting<Double> time;
    public final Setting<Double> friction;
    public final Setting<BlackOutColor> color;
    public final Setting<BlackOutColor> shadowColor;

    public ParticleMultiSetting(SettingGroup sg, String name, SingleOut<Boolean> visible) {
        if (name == null) {
            name = "";
        } else {
            name = name + " ";
        }

        this.mode = sg.enumSetting(name + "Particle Mode", ParticleMode.Normal, ".", visible);
        this.particles = sg.intSetting(name + "Particles", 25, 0, 100, 1, ".", visible);
        this.velocity = sg.doubleSetting(name + "Particle Velocity", 0.5, 0.0, 1.0, 0.01, ".", visible);
        this.time = sg.doubleSetting(name + "Particle Time", 1.0, 0.0, 5.0, 0.05, ".", visible);
        this.friction = sg.doubleSetting(
                name + "Particle Friction", 0.9, 0.0, 1.0, 0.01, ".", () -> this.mode.get() == ParticleMode.Normal && visible.get()
        );
        this.color = sg.colorSetting(name + "Particle Color", new BlackOutColor(255, 255, 255, 255), ".", visible);
        this.shadowColor = sg.colorSetting(name + "Particle Shadow Color", new BlackOutColor(255, 255, 255, 255), ".", visible);
    }

    public static ParticleMultiSetting of(SettingGroup sg) {
        return of(sg, null);
    }

    public static ParticleMultiSetting of(SettingGroup sg, String name) {
        return of(sg, name, () -> true);
    }

    public static ParticleMultiSetting of(SettingGroup sg, String name, SingleOut<Boolean> visible) {
        return new ParticleMultiSetting(sg, name, visible);
    }

    public void spawnParticles(Vec3d vec) {
        this.spawnParticles(vec, randomMotion);
    }

    public void spawnParticles(Vec3d vec, Function<Double, Vec3d> getMotion) {
        for (int i = 0; i < this.particles.get(); i++) {
            this.spawnParticle(vec, getMotion.apply(this.velocity.get() * (0.75 + ThreadLocalRandom.current().nextDouble() * 0.25)));
        }
    }

    private void spawnParticle(Vec3d vec, Vec3d motion) {
        switch (this.mode.get()) {
            case Normal:
                Managers.PARTICLE.addFriction(vec, motion, this.friction.get(), this.time.get(), this.color.get().getRGB(), this.shadowColor.get().getRGB());
                break;
            case Bouncy:
                Managers.PARTICLE.addBouncy(vec, motion, this.time.get(), this.color.get().getRGB(), this.shadowColor.get().getRGB());
        }
    }

    public enum ParticleMode {
        Normal,
        Bouncy
    }
}
