/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PlaySoundEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorAbstractSoundInstance;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.SoundUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SoundModifier extends Module {
    private final Map<ResourceLocation[], SoundSettingGroup> soundSettings = new HashMap<>();

    public SoundModifier() {
        super("Sound Modifier", "Customizes or mutes specific in-game sound effects such as explosions, hits, and totem pops.", SubCategory.MISC, true);
        this.put("Explosion", SoundEvents.GENERIC_EXPLODE.value());
        this.put(
                "Hit", SoundEvents.PLAYER_ATTACK_WEAK, SoundEvents.PLAYER_ATTACK_CRIT, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.PLAYER_ATTACK_SWEEP
        );
        this.put("Damage", SoundEvents.PLAYER_HURT, SoundEvents.PLAYER_HURT_DROWN, SoundEvents.PLAYER_HURT_FREEZE, SoundEvents.PLAYER_HURT_ON_FIRE, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH);
        this.put("Totem", SoundEvents.TOTEM_USE);
    }

    private void put(String name, SoundEvent... events) {
        this.soundSettings.put(this.getIdentifiers(events), this.addSSGroup(name));
    }

    private ResourceLocation[] getIdentifiers(SoundEvent[] events) {
        ResourceLocation[] identifiers = new ResourceLocation[events.length];

        for (int i = 0; i < events.length; i++) {
            identifiers[i] = events[i].location();
        }

        return identifiers;
    }

    private SoundSettingGroup addSSGroup(String name) {
        SettingGroup group = this.addGroup(name);
        Setting<Boolean> cancel = group.booleanSetting("Mute " + name, false, "Completely silences " + name + " sound effects.");
        Setting<Double> volume = group.doubleSetting(name + " Volume", 1.0, 0.0, 10.0, 0.1, "Adjusts the output volume of " + name + " sounds.", () -> !cancel.get());
        Setting<Double> pitch = group.doubleSetting(name + " Pitch", 1.0, 0.0, 10.0, 0.1, "Modifies the frequency (pitch) of " + name + " sounds.", () -> !cancel.get());
        Setting<SoundMode> soundMode = group.enumSetting(name + " Audio Override", SoundMode.Default, "Replaces the original game sound with a custom audio file.", () -> !cancel.get());
        return new SoundSettingGroup(cancel, volume, pitch, soundMode);
    }

    @Event
    public void onSound(PlaySoundEvent event) {
        SoundSettingGroup group = this.getGroup(event.sound.getLocation());
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

    private SoundSettingGroup getGroup(ResourceLocation identifier) {
        for (Entry<ResourceLocation[], SoundSettingGroup> entry : this.soundSettings.entrySet()) {
            if (this.contains(entry.getKey(), identifier)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private boolean contains(ResourceLocation[] identifiers, ResourceLocation identifier) {
        for (ResourceLocation id : identifiers) {
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
