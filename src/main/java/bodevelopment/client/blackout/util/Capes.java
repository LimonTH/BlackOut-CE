package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("resource")
public class Capes {
    private static final Map<String, Identifier> capes = new ConcurrentHashMap<>();
    private static final List<Identifier> loaded = new CopyOnWriteArrayList<>();
    private static final List<Pair<String, Identifier>> toLoad = new CopyOnWriteArrayList<>();
    private static volatile boolean loading = false;

    public static Identifier getCape(AbstractClientPlayerEntity player) {
        String uuid = player.getUuidAsString();
        Identifier identifier = capes.get(uuid);

        if (identifier == null) return null;

        if (!loaded.contains(identifier)) {
            if (!loading) {
                startLoad();
            }
            return null;
        }
        return identifier;
    }

    public static void requestCapes() {
        CompletableFuture.runAsync(() -> {
            try (InputStream stream = URI.create("https://raw.githubusercontent.com/LimonTH/Blackout-CE-capes/main/capes").toURL().openStream();
                 BufferedReader read = new BufferedReader(new InputStreamReader(stream))) {

                Map<String, Identifier> identifiers = new HashMap<>();
                read.lines().forEach(line -> readLine(line, identifiers));
            } catch (IOException e) {
                System.err.println("[BlackOut] Failed to fetch capes list");
            }
        });
    }

    private static synchronized void startLoad() {
        if (!toLoad.isEmpty() && !loading) {
            loading = true;
            Pair<String, Identifier> pair = toLoad.removeFirst();

            CompletableFuture.runAsync(() -> {
                new CapeTexture(pair.getLeft(), pair.getRight());
            });
        }
    }

    private static void readLine(String line, Map<String, Identifier> identifiers) {
        String[] parts = line.replace(" ", "").split(":");
        if (parts.length >= 3) {
            String uuid = parts[1];
            String capeName = parts[2];

            capes.put(uuid, identifiers.computeIfAbsent(capeName, name -> {
                Identifier id = Identifier.of("blackout", "textures/capes/" + name.toLowerCase() + ".png");
                toLoad.add(new Pair<>(name, id));
                return id;
            }));
        }
    }

    private static class CapeTexture extends AbstractTexture {
        public CapeTexture(String name, Identifier identifier) {
            try {
                BufferedImage image = ImageIO.read(URI.create("https://raw.githubusercontent.com/LimonTH/Blackout-CE-capes/main/textures/" + name + ".png").toURL());

                RenderSystem.recordRenderCall(() -> {
                    uploadAndRegister(name, identifier, image);
                });
            } catch (IOException e) {
                loading = false;
                startLoad();
            }
        }

        private void uploadAndRegister(String name, Identifier identifier, BufferedImage image) {
            try {
                this.glId = BOTextures.upload(image, false).id();

                TextureManager manager = BlackOut.mc.getTextureManager();
                manager.registerTexture(identifier, this);

                Capes.loaded.add(identifier);
                Capes.loading = false;

                Capes.startLoad();
            } catch (Exception e) {
                Capes.loading = false;
                Capes.startLoad();
            }
        }

        @Override
        public void load(ResourceManager manager) {
            // Пусто, так как загружаем из сети, а не из ассетов
        }
    }
}