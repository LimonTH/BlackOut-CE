package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
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

public class SoundUtils {
    public static void play(float pitch, float volume, String name) {
        play(pitch, volume, 0.0, 0.0, 0.0, false, name);
    }

    public static void play(SoundInstance instance, String name) {
        play(instance.getPitch(), instance.getVolume(), instance.getX(), instance.getY(), instance.getZ(), instance.isRelative(), name);
    }

    public static void play(float pitch, float volume, double x, double y, double z, boolean relative, String name) {
        InputStream inputStream = FileUtils.getResourceStream("sounds", name + ".ogg");
        SoundEngine engine = BlackOut.mc.getSoundManager().soundEngine;
        ChannelAccess.ChannelHandle sourceManager = createSourceManager(engine, 5);
        if (sourceManager != null) {
            Vec3 vec = new Vec3(x, y, z);
            sourceManager.execute(source -> {
                source.setPitch(pitch);
                source.setVolume(volume);
                source.disableAttenuation();
                source.setLooping(false);
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
    }

    private static ChannelAccess.ChannelHandle createSourceManager(SoundEngine engine, int i) {
        ChannelAccess.ChannelHandle sourceManager = engine.channelAccess.createHandle(Library.Pool.STREAMING).join();
        return sourceManager == null && i > 0 ? createSourceManager(engine, --i) : sourceManager;
    }
}
