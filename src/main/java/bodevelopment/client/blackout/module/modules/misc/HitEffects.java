package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.ParticleMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.SoundUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public class HitEffects extends Module {
    private final SettingGroup sgEntities = this.addGroup("Entities");
    private final SettingGroup sgParticles = this.addGroup("Particles");
    private final SettingGroup sgHitSounds = this.addGroup("Hit Sounds");
    private final SettingGroup sgHitMarker = this.addGroup("Hit Marker");
    private final Setting<List<EntityType<?>>> entities = this.sgEntities.entityListSetting("Entities", ".", EntityType.PLAYER);
    private final Setting<Boolean> particle = this.sgParticles.booleanSetting("Draw Particles", false, ".");
    private final ParticleMultiSetting particles = ParticleMultiSetting.of(this.sgParticles, null, this.particle::get);
    private final Setting<Boolean> hitSound = this.sgHitSounds.booleanSetting("Hit Sound", false, ".");
    public final Setting<Sound> sound = this.sgHitSounds.enumSetting("Sound", Sound.NeverLose, ".", this.hitSound::get);
    private final Setting<Double> volume = this.sgHitSounds.doubleSetting("Volume", 1.0, 0.0, 10.0, 0.1, ".", this.hitSound::get);
    private final Setting<Double> pitch = this.sgHitSounds.doubleSetting("Pitch", 1.0, 0.0, 10.0, 0.1, ".", this.hitSound::get);
    private final Setting<Boolean> hitMarker = this.sgHitMarker.booleanSetting("Hit Marker", false, ".");
    private final Setting<Integer> start = this.sgHitMarker.intSetting("Start", 5, 0, 25, 1, ".", this.hitMarker::get);
    private final Setting<Integer> end = this.sgHitMarker.intSetting("End", 15, 0, 50, 1, ".", this.hitMarker::get);
    private final Setting<BlackOutColor> markerColor = this.sgHitMarker.colorSetting("Hit Marker Color", new BlackOutColor(175, 175, 175, 200), ".", this.hitMarker::get);
    private final MatrixStack stack = new MatrixStack();
    private long startedDraw = System.currentTimeMillis();

    public HitEffects() {
        super("Hit Effects", ",", SubCategory.MISC, true);
    }

    @Event
    public void onSend(PacketEvent.Sent event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (event.packet instanceof AccessorInteractEntityC2SPacket packet && packet.getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
                Entity target = BlackOut.mc.world.getEntityById(packet.getId());
                if (target == null) {
                    return;
                }

                if (!this.entities.get().contains(target.getType()) || target == BlackOut.mc.player) {
                    return;
                }

                this.playSounds(target);
                this.startedDraw = System.currentTimeMillis();
                if (this.particle.get()) {
                    this.particles.spawnParticles(BoxUtils.middle(target.getBoundingBox()));
                }
            }
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.drawHitMarker();
        }
    }

    private void playSounds(Entity target) {
        if (this.hitSound.get()) {
            switch (this.sound.get()) {
                case NeverLose:
                    SoundUtils.play(this.pitch.get().floatValue(), this.volume.get().floatValue(), "neverlose");
                    break;
                case Skeet:
                    SoundUtils.play(this.pitch.get().floatValue(), this.volume.get().floatValue(), "skeet");
                    break;
                case Waltuh:
                    SoundUtils.play(this.pitch.get().floatValue(), this.volume.get().floatValue(), "waltuh");
                    break;
                case Critical:
                    BlackOut.mc
                            .world
                            .playSound(
                                    target.getX(),
                                    target.getY() + 1.0,
                                    target.getZ(),
                                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                                    SoundCategory.PLAYERS,
                                    this.volume.get().floatValue(),
                                    this.pitch.get().floatValue(),
                                    true
                            );
            }
        }
    }

    private void drawHitMarker() {
        if (this.hitMarker.get()) {
            if (System.currentTimeMillis() - this.startedDraw <= 100L) {
                this.stack.push();
                RenderUtils.unGuiScale(this.stack);
                this.stack.translate(BlackOut.mc.getWindow().getWidth() / 2.0F - 1.0F, BlackOut.mc.getWindow().getHeight() / 2.0F - 1.0F, 0.0F);
                int s = this.start.get();
                int e = this.end.get();
                RenderUtils.fadeLine(this.stack, s, s, e, e, this.markerColor.get().getRGB());
                RenderUtils.fadeLine(this.stack, s, -s, e, -e, this.markerColor.get().getRGB());
                RenderUtils.fadeLine(this.stack, -s, s, -e, e, this.markerColor.get().getRGB());
                RenderUtils.fadeLine(this.stack, -s, -s, -e, -e, this.markerColor.get().getRGB());
                this.stack.pop();
            }
        }
    }

    public enum Sound {
        Skeet,
        NeverLose,
        Waltuh,
        Critical
    }
}
