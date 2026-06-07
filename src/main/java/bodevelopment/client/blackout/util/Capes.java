package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IUUIDHolder;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("resource")
public class Capes {
    private static final Map<String, ResourceLocation> capes = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> loaded = new CopyOnWriteArrayList<>();
    private static final List<Tuple<String, ResourceLocation>> toLoad = new CopyOnWriteArrayList<>();
    private static final int HTTP_TIMEOUT_MS = 5000;
    private static volatile boolean loading = false;

    public static ResourceLocation getCape(PlayerRenderState state) {
        UUID uuidObj = ((IUUIDHolder) state).blackout$getUUID();
        if (uuidObj == null) return null;

        String uuid = uuidObj.toString();
        ResourceLocation identifier = capes.get(uuid);

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
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(
                        "https://raw.githubusercontent.com/LimonTH/Blackout-CE-capes/main/capes").toURL().openConnection();
                conn.setConnectTimeout(HTTP_TIMEOUT_MS);
                conn.setReadTimeout(HTTP_TIMEOUT_MS);
                try (InputStream stream = conn.getInputStream();
                     BufferedReader read = new BufferedReader(new InputStreamReader(stream))) {
                    Map<String, ResourceLocation> identifiers = new HashMap<>();
                    read.lines().forEach(line -> readLine(line, identifiers));
                }
            } catch (IOException e) {
                System.err.println("[BlackOut] Failed to fetch capes list (server unreachable or timeout)");
            }
        });
    }

    private static synchronized void startLoad() {
        if (!toLoad.isEmpty() && !loading) {
            loading = true;
            Tuple<String, ResourceLocation> pair = toLoad.removeFirst();

            CompletableFuture.runAsync(() -> {
                new CapeTexture(pair.getA(), pair.getB());
            });
        }
    }

    private static void readLine(String line, Map<String, ResourceLocation> identifiers) {
        String[] parts = line.replace(" ", "").split(":");
        if (parts.length >= 3) {
            String uuid = parts[1];
            String capeName = parts[2];

            capes.put(uuid, identifiers.computeIfAbsent(capeName, name -> {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("blackout", "textures/capes/" + name.toLowerCase() + ".png");
                toLoad.add(new Tuple<>(name, id));
                return id;
            }));
        }
    }

    private static class CapeTexture extends AbstractTexture {
        public CapeTexture(String name, ResourceLocation identifier) {
            try {
                HttpURLConnection conn = (HttpURLConnection) URI.create(
                        "https://raw.githubusercontent.com/LimonTH/Blackout-CE-capes/main/textures/" + name + ".png").toURL().openConnection();
                conn.setConnectTimeout(HTTP_TIMEOUT_MS);
                conn.setReadTimeout(HTTP_TIMEOUT_MS);
                BufferedImage image = ImageIO.read(conn.getInputStream());

                RenderSystem.recordRenderCall(() -> {
                    uploadAndRegister(name, identifier, image);
                });
            } catch (IOException e) {
                loading = false;
                startLoad();
            }
        }

        private void uploadAndRegister(String name, ResourceLocation identifier, BufferedImage image) {
            try {
                this.id = BOTextures.upload(image, false).id();

                TextureManager manager = BlackOut.mc.getTextureManager();
                manager.register(identifier, this);

                Capes.loaded.add(identifier);
                Capes.loading = false;

                Capes.startLoad();
            } catch (Exception e) {
                Capes.loading = false;
                Capes.startLoad();
            }
        }
    }
}