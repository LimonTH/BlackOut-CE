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


    private final Setting<List<EntityType<?>>> entities = this.sgEntities.entityFilterdListSetting("Target Entities", "The entity types that will trigger effects when struck.", type -> type != EntityType.ITEM && type != EntityType.EXPERIENCE_ORB && type != EntityType.AREA_EFFECT_CLOUD && type != EntityType.MARKER && type != EntityType.POTION && type != EntityType.LLAMA_SPIT && type != EntityType.EYE_OF_ENDER && type != EntityType.DRAGON_FIREBALL && type != EntityType.FIREWORK_ROCKET && type != EntityType.ENDER_PEARL && type != EntityType.FISHING_BOBBER && type != EntityType.ARROW && type != EntityType.SPECTRAL_ARROW && type != EntityType.SNOWBALL && type != EntityType.SMALL_FIREBALL && type != EntityType.WITHER_SKULL && type != EntityType.FALLING_BLOCK && type != EntityType.TNT && type != EntityType.EVOKER_FANGS && type != EntityType.LIGHTNING_BOLT && type != EntityType.WIND_CHARGE && type != EntityType.BREEZE_WIND_CHARGE && !type.toString().contains("display"));
    private final Setting<Boolean> particle = this.sgParticles.booleanSetting("Enable Particles", false, "Spawns visual particles at the target's location upon a successful hit.");
    private final ParticleMultiSetting particles = ParticleMultiSetting.of(this.sgParticles, null, this.particle::get);
    private final Setting<Boolean> hitSound = this.sgHitSounds.booleanSetting("Enable Audio", false, "Plays a specific sound effect when an attack packet is sent.");
    public final Setting<Sound> sound = this.sgHitSounds.enumSetting("Audio Clip", Sound.NeverLose, "The sound profile to play upon hitting a target.", this.hitSound::get);
    private final Setting<Double> volume = this.sgHitSounds.doubleSetting("Audio Volume", 1.0, 0.0, 10.0, 0.1, "The playback intensity of the hit sound.", this.hitSound::get);
    private final Setting<Double> pitch = this.sgHitSounds.doubleSetting("Audio Pitch", 1.0, 0.0, 10.0, 0.1, "The frequency modulation of the hit sound.", this.hitSound::get);
    private final Setting<Boolean> hitMarker = this.sgHitMarker.booleanSetting("Enable Crosshair Marker", false, "Displays a visual hitmarker on the HUD when damage is dealt.");
    private final Setting<Integer> start = this.sgHitMarker.intSetting("Inner Offset", 5, 0, 25, 1, "The distance from the center of the screen where the marker lines begin.", this.hitMarker::get);
    private final Setting<Integer> end = this.sgHitMarker.intSetting("Outer Length", 15, 0, 50, 1, "The distance from the center where the marker lines terminate.", this.hitMarker::get);
    private final Setting<BlackOutColor> markerColor = this.sgHitMarker.colorSetting("Marker Appearance", new BlackOutColor(175, 175, 175, 200), "The color and transparency of the hitmarker lines.", this.hitMarker::get);

    private final MatrixStack stack = new MatrixStack();
    private long startedDraw = System.currentTimeMillis();
    public HitEffects() {
        super("Hit Effects", "Provides visual and auditory feedback, such as hitmarkers and custom sounds, when successfully attacking entities.", SubCategory.MISC, true);
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
