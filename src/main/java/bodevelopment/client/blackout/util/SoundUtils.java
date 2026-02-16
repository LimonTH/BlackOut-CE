package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.client.sound.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

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
        SoundSystem engine = BlackOut.mc.getSoundManager().soundSystem;
        Channel.SourceManager sourceManager = createSourceManager(engine, 5);
        if (sourceManager != null) {
            Vec3d vec = new Vec3d(x, y, z);
            sourceManager.run(source -> {
                source.setPitch(pitch);
                source.setVolume(volume);
                source.disableAttenuation();
                source.setLooping(false);
                source.setPosition(vec);
                source.setRelative(relative);
            });
            CompletableFuture.supplyAsync(() -> {
                try {
                    return new OggAudioStream(inputStream);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, Util.getMainWorkerExecutor()).thenAccept(stream -> sourceManager.run(source -> {
                source.setStream(stream);
                source.play();
            }));
        }
    }

    private static Channel.SourceManager createSourceManager(SoundSystem engine, int i) {
        Channel.SourceManager sourceManager = engine.channel.createSource(SoundEngine.RunMode.STREAMING).join();
        return sourceManager == null && i > 0 ? createSourceManager(engine, --i) : sourceManager;
    }
}
