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

package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import com.mojang.blaze3d.audio.Library;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@PublicAPI
public class SoundUtils {
    public static ChannelAccess.ChannelHandle play(float pitch, float volume, String name) {
        return play(pitch, volume, 0.0, 0.0, 0.0, false, name);
    }

    public static ChannelAccess.ChannelHandle play(SoundInstance instance, String name) {
        return play(instance.getPitch(), instance.getVolume(), instance.getX(), instance.getY(), instance.getZ(), instance.isRelative(), name);
    }

    public static ChannelAccess.ChannelHandle play(float pitch, float volume, double x, double y, double z, boolean relative, String name) {
        return playInternal(pitch, volume, x, y, z, relative, false, name);
    }

    public static ChannelAccess.ChannelHandle playLooping(float pitch, float volume, String name) {
        return playInternal(pitch, volume, 0.0, 0.0, 0.0, false, true, name);
    }

    /**
     * Plays an OGG audio stream directly, e.g. from an addon JAR resource.
     * The caller is responsible for closing the stream.
     */
    public static ChannelAccess.ChannelHandle playStream(float pitch, float volume, InputStream inputStream) {
        return playStream(pitch, volume, inputStream, false);
    }

    /**
     * Plays an OGG audio stream with optional looping.
     */
    public static ChannelAccess.ChannelHandle playStream(float pitch, float volume, InputStream inputStream, boolean looping) {
        SoundEngine engine = BlackOut.mc.getSoundManager().soundEngine;
        ChannelAccess.ChannelHandle sourceManager = createSourceManager(engine, 5);
        if (sourceManager != null) {
            sourceManager.execute(source -> {
                source.setPitch(pitch);
                source.setVolume(volume);
                source.disableAttenuation();
                source.setLooping(looping);
                source.setRelative(false);
            });
            CompletableFuture.supplyAsync(() -> {
                try {
                    return new JOrbisAudioStream(inputStream);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, Util.backgroundExecutor()).thenAccept(stream -> sourceManager.execute(source -> {
                source.attachBufferStream(stream);
                source.play();
            }));
        }
        return sourceManager;
    }

    public static void stop(ChannelAccess.ChannelHandle handle) {
        if (handle != null) {
            handle.execute(source -> source.stop());
        }
    }

    private static ChannelAccess.ChannelHandle playInternal(float pitch, float volume, double x, double y, double z, boolean relative, boolean looping, String name) {
        InputStream inputStream = FileUtils.getResourceStream("sounds", name + ".ogg");
        SoundEngine engine = BlackOut.mc.getSoundManager().soundEngine;
        ChannelAccess.ChannelHandle sourceManager = createSourceManager(engine, 5);
        if (sourceManager != null) {
            Vec3 vec = new Vec3(x, y, z);
            sourceManager.execute(source -> {
                source.setPitch(pitch);
                source.setVolume(volume);
                source.disableAttenuation();
                source.setLooping(looping);
                source.setSelfPosition(vec);
                source.setRelative(relative);
            });
            CompletableFuture.supplyAsync(() -> {
                try {
                    return new JOrbisAudioStream(inputStream);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, Util.backgroundExecutor()).thenAccept(stream -> sourceManager.execute(source -> {
                source.attachBufferStream(stream);
                source.play();
            }));
        }
        return sourceManager;
    }

    private static ChannelAccess.ChannelHandle createSourceManager(SoundEngine engine, int i) {
        ChannelAccess.ChannelHandle sourceManager = engine.channelAccess.createHandle(Library.Pool.STREAMING).join();
        return sourceManager == null && i > 0 ? createSourceManager(engine, --i) : sourceManager;
    }
}
