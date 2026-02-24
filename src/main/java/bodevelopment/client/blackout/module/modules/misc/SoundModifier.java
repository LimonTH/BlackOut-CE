package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PlaySoundEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorAbstractSoundInstance;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.SoundUtils;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SoundModifier extends Module {
    private final Map<Identifier[], SoundSettingGroup> soundSettings = new HashMap<>();

    public SoundModifier() {
        super("Sound Modifier", "Modifies sounds.", SubCategory.MISC, true);
        this.put("Explosion", SoundEvents.ENTITY_GENERIC_EXPLODE.value());
        this.put(
                "Hit", SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP
        );
        this.put("Damage", SoundEvents.ENTITY_PLAYER_HURT, SoundEvents.ENTITY_PLAYER_HURT_DROWN, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundEvents.ENTITY_PLAYER_HURT_ON_FIRE, SoundEvents.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH);
        this.put("Totem", SoundEvents.ITEM_TOTEM_USE);
    }

    private void put(String name, SoundEvent... events) {
        this.soundSettings.put(this.getIdentifiers(events), this.addSSGroup(name));
    }

    private Identifier[] getIdentifiers(SoundEvent[] events) {
        Identifier[] identifiers = new Identifier[events.length];

        for (int i = 0; i < events.length; i++) {
            identifiers[i] = events[i].getId();
        }

        return identifiers;
    }

    private SoundSettingGroup addSSGroup(String name) {
        SettingGroup group = this.addGroup(name);
        Setting<Boolean> cancel = group.booleanSetting("Cancel " + name, false, "Doesn't play" + name + " sounds.");
        Setting<Double> volume = group.doubleSetting(name + " Volume", 1.0, 0.0, 10.0, 0.1, "Volume of " + name + " sounds.", () -> !cancel.get());
        Setting<Double> pitch = group.doubleSetting(name + " Pitch", 1.0, 0.0, 10.0, 0.1, "Pitch of " + name + " sounds.", () -> !cancel.get());
        Setting<SoundMode> soundMode = group.enumSetting(name + " Mode", SoundMode.Default, ".", () -> !cancel.get());
        return new SoundSettingGroup(cancel, volume, pitch, soundMode);
    }

    @Event
    public void onSound(PlaySoundEvent event) {
        SoundSettingGroup group = this.getGroup(event.sound.getId());
        if (group != null) {
            if (group.cancel.get()) {
                event.setCancelled(true);
            } else {
                float volume = group.volume.get().floatValue();
                float pitch = group.pitch.get().floatValue();
                SoundMode soundMode = group.soundMode.get();
                if (soundMode == SoundMode.Default) {
                    ((AccessorAbstractSoundInstance) event.sound).setVolume(volume);
                    ((AccessorAbstractSoundInstance) event.sound).setPitch(pitch);
                } else {
                    event.setCancelled(true);
                    SoundUtils.play(
                            pitch, volume, event.sound.getX(), event.sound.getY(), event.sound.getZ(), event.sound.isRelative(), soundMode.name
                    );
                }
            }
        }
    }

    private SoundSettingGroup getGroup(Identifier identifier) {
        for (Entry<Identifier[], SoundSettingGroup> entry : this.soundSettings.entrySet()) {
            if (this.contains(entry.getKey(), identifier)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private boolean contains(Identifier[] identifiers, Identifier identifier) {
        for (Identifier id : identifiers) {
            if (id.equals(identifier)) {
                return true;
            }
        }

        return false;
    }

    public enum SoundMode {
        Default,
        Power_Down,
        Disable,
        Enable,
        Explode,
        Hit,
        Hit2,
        Totem,
        Dig;

        private final String name = this.name().toLowerCase();
    }

    private record SoundSettingGroup(Setting<Boolean> cancel, Setting<Double> volume, Setting<Double> pitch,
                                     Setting<SoundMode> soundMode) {
    }
}
