package bodevelopment.client.blackout.rendering.texture;

import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.util.FileUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13C;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class BOTextures {
    private static final TextureRenderer logoRenderer = new TextureRenderer("logo");
    private static final TextureRenderer catRenderer = new TextureRenderer("catgirl");
    private static final TextureRenderer cat2Renderer = new TextureRenderer("catgirl2clear");
    private static final TextureRenderer bakrongRenderer = new TextureRenderer("bakrong");
    private static final TextureRenderer arrowRenderer = new TextureRenderer("arrow");
    private static final TextureRenderer closeRenderer = new TextureRenderer("close");
    private static final TextureRenderer infoIconRender = new TextureRenderer("info");
    private static final TextureRenderer youtubeIconRenderer = new TextureRenderer("youtube");
    private static final TextureRenderer discordIconRenderer = new TextureRenderer("discord");
    private static final TextureRenderer githubIconRenderer = new TextureRenderer("github");
    private static final TextureRenderer folderIconRenderer = new TextureRenderer("folder");
    private static final TextureRenderer enableIconRenderer = new TextureRenderer("enable");
    private static final TextureRenderer disableIconRenderer = new TextureRenderer("disable");
    private static final TextureRenderer infoIconRenderer = new TextureRenderer("info");
    private static final TextureRenderer alertIconRenderer = new TextureRenderer("alert");
    private static final TextureRenderer mewingIconRenderer = new TextureRenderer("mewing");
    private static final TextureRenderer combatIconRenderer = new TextureRenderer("combat");
    private static final TextureRenderer movementIconRenderer = new TextureRenderer("movement");
    private static final TextureRenderer visualIconRenderer = new TextureRenderer("visual");
    private static final TextureRenderer miscIconRenderer = new TextureRenderer("misc");
    private static final TextureRenderer ghostIconRenderer = new TextureRenderer("ghost");
    private static final TextureRenderer settingsIconRenderer = new TextureRenderer("settings");
    private static final TextureRenderer hudIconRenderer = new TextureRenderer("hud");
    private static final TextureRenderer bindsIconRenderer = new TextureRenderer("binds");
    private static final TextureRenderer trashIconRenderer = new TextureRenderer("trash");
    private static final TextureRenderer textEditIconRenderer = new TextureRenderer("textedit");
    private static final TextureRenderer copyIconRenderer = new TextureRenderer("copy");
    private static final TextureRenderer lockIconRenderer = new TextureRenderer("lock");
    private static final TextureRenderer lockOpenIconRenderer = new TextureRenderer("lockopen");
    private static final TextureRenderer resetIconRenderer = new TextureRenderer("reset");
    private static final TextureRenderer cloudIconRenderer = new TextureRenderer("cloud");
    private static final TextureRenderer cloud2IconRenderer = new TextureRenderer("cloud2");
    private static final TextureRenderer plusIconRenderer = new TextureRenderer("plus");
    private static final TextureRenderer consoleIconRenderer = new TextureRenderer("console");
    private static final TextureRenderer personIconRenderer = new TextureRenderer("person");
    private static final TextureRenderer minimalistLogoRenderer = new TextureRenderer("BoMinimalist");

    public static void init() {
        Arrays.stream(BOTextures.class.getDeclaredFields()).forEach(field -> {
            try {
                if (field.get(field) instanceof TextureRenderer renderer) {
                    renderer.load(upload("textures", renderer.getName() + ".png"));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static UploadData upload(String... path) {
        return upload(FileUtils.readResourceImage(path));
    }

    public static UploadData upload(BufferedImage image) {
        return upload(image, false);
    }

    public static UploadData upload(BufferedImage image, boolean font) {
        int id = GL13C.glGenTextures();
        ByteBuffer buffer = font ? writeFontToBuffer(image) : writeToBuffer(image);
        GL13C.glActiveTexture(33984);
        GL13C.glBindTexture(3553, id);
        GL13C.glTexParameteri(3553, 10242, 33071);
        GL13C.glTexParameteri(3553, 10243, 33071);
        GL13C.glTexParameteri(3553, 10240, 9729);
        GL13C.glTexParameteri(3553, 10241, 9729);
        GL13C.glTexImage2D(3553, 0, 32856, image.getWidth(), image.getHeight(), 0, 6408, 5121, buffer);
        GL13C.glBindTexture(3553, GlStateManager.TEXTURES[0].boundTexture);
        GL13C.glActiveTexture(33984 | GlStateManager.activeTexture);
        return new UploadData(id, image.getWidth(), image.getHeight());
    }

    public static ByteBuffer writeToBuffer(BufferedImage image) {
        int size = image.getWidth() * image.getHeight();
        ByteBuffer buffer = BufferUtils.createByteBuffer(size * 4);

        for (int pixel : image.getRGB(0, 0, image.getWidth(), image.getHeight(), new int[size], 0, image.getWidth())) {
            buffer.put((byte) (pixel >> 16 & 0xFF)).put((byte) (pixel >> 8 & 0xFF)).put((byte) (pixel & 0xFF)).put((byte) (pixel >> 24 & 0xFF));
        }

        return buffer.flip();
    }

    public static ByteBuffer writeFontToBuffer(BufferedImage image) {
        int size = image.getWidth() * image.getHeight();
        ByteBuffer buffer = BufferUtils.createByteBuffer(size * 4);
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), new int[size], 0, image.getWidth());

        for (int i = 0; i < pixels.length; i++) {
            byte val = (byte) getBlurredColor(i, image.getWidth(), image.getHeight(), pixels);
            buffer.put(val).put(val).put(val).put(val);
        }

        return buffer.flip();
    }

    private static int getBlurredColor(int i, int width, int height, int[] pixels) {
        int y = i / width;
        int x = i - y * width;
        if (pixels[i] >>> 24 == 255) {
            return 255;
        } else {
            MutableDouble total = new MutableDouble(0.0);
            MutableDouble totalWeight = new MutableDouble(0.0);
            getR(x, y, 1, 1, width, height, pixels, total, totalWeight, 0.3);
            getR(x, y, 1, -1, width, height, pixels, total, totalWeight, 0.3);
            getR(x, y, -1, 1, width, height, pixels, total, totalWeight, 0.3);
            getR(x, y, -1, -1, width, height, pixels, total, totalWeight, 0.3);
            getR(x, y, 1, 0, width, height, pixels, total, totalWeight, 0.5);
            getR(x, y, -1, 0, width, height, pixels, total, totalWeight, 0.5);
            getR(x, y, 0, 1, width, height, pixels, total, totalWeight, 0.5);
            getR(x, y, 0, -1, width, height, pixels, total, totalWeight, 0.5);
            getR(x, y, 2, 0, width, height, pixels, total, totalWeight, 0.25);
            getR(x, y, -2, 0, width, height, pixels, total, totalWeight, 0.25);
            getR(x, y, 0, 2, width, height, pixels, total, totalWeight, 0.25);
            getR(x, y, 0, -2, width, height, pixels, total, totalWeight, 0.25);
            getR(x, y, 2, 2, width, height, pixels, total, totalWeight, 0.2);
            getR(x, y, 2, -2, width, height, pixels, total, totalWeight, 0.2);
            getR(x, y, -2, 2, width, height, pixels, total, totalWeight, 0.2);
            getR(x, y, -2, -2, width, height, pixels, total, totalWeight, 0.2);
            getR(x, y, 3, 0, width, height, pixels, total, totalWeight, 0.2);
            getR(x, y, -3, 0, width, height, pixels, total, totalWeight, 0.2);
            getR(x, y, 0, 3, width, height, pixels, total, totalWeight, 0.2);
            getR(x, y, 0, -3, width, height, pixels, total, totalWeight, 0.2);
            return Math.min((int) Math.round(total.getValue() / totalWeight.getValue() * 1.16), 255);
        }
    }

    private static void getR(
            int x, int y, int offsetX, int offsetY, int width, int height, int[] pixels, MutableDouble total, MutableDouble totalWeight, double weight
    ) {
        x += offsetX;
        y += offsetY;
        if (x < width && x >= 0 && y >= 0 && y < height) {
            total.add((pixels[y * width + x] >>> 24) * weight);
            totalWeight.add(weight);
        }
    }

    public static TextureRenderer getLogoRenderer() {
        return logoRenderer;
    }

    public static TextureRenderer getCatRenderer() {
        return catRenderer;
    }

    public static TextureRenderer getCat2Renderer() {
        return cat2Renderer;
    }

    public static TextureRenderer getBakrongRenderer() {
        return bakrongRenderer;
    }

    public static TextureRenderer getArrowRenderer() {
        return arrowRenderer;
    }

    public static TextureRenderer getCloseRenderer() {
        return closeRenderer;
    }

    public static TextureRenderer getInfoIconRender() {
        return infoIconRender;
    }

    public static TextureRenderer getYoutubeIconRenderer() {
        return youtubeIconRenderer;
    }

    public static TextureRenderer getDiscordIconRenderer() {
        return discordIconRenderer;
    }

    public static TextureRenderer getGithubIconRenderer() {
        return githubIconRenderer;
    }

    public static TextureRenderer getFolderIconRenderer() {
        return folderIconRenderer;
    }

    public static TextureRenderer getEnableIconRenderer() {
        return enableIconRenderer;
    }

    public static TextureRenderer getDisableIconRenderer() {
        return disableIconRenderer;
    }

    public static TextureRenderer getInfoIconRenderer() {
        return infoIconRenderer;
    }

    public static TextureRenderer getAlertIconRenderer() {
        return alertIconRenderer;
    }

    public static TextureRenderer getMewingIconRenderer() {
        return mewingIconRenderer;
    }

    public static TextureRenderer getCombatIconRenderer() {
        return combatIconRenderer;
    }

    public static TextureRenderer getMovementIconRenderer() {
        return movementIconRenderer;
    }

    public static TextureRenderer getVisualIconRenderer() {
        return visualIconRenderer;
    }

    public static TextureRenderer getMiscIconRenderer() {
        return miscIconRenderer;
    }

    public static TextureRenderer getGhostIconRenderer() {
        return ghostIconRenderer;
    }

    public static TextureRenderer getSettingsIconRenderer() {
        return settingsIconRenderer;
    }

    public static TextureRenderer getHudIconRenderer() {
        return hudIconRenderer;
    }

    public static TextureRenderer getBindsIconRenderer() {
        return bindsIconRenderer;
    }

    public static TextureRenderer getTrashIconRenderer() {
        return trashIconRenderer;
    }

    public static TextureRenderer getTextEditIconRenderer() {
        return textEditIconRenderer;
    }

    public static TextureRenderer getCopyIconRenderer() {
        return copyIconRenderer;
    }

    public static TextureRenderer getLockIconRenderer() {
        return lockIconRenderer;
    }

    public static TextureRenderer getLockOpenIconRenderer() {
        return lockOpenIconRenderer;
    }

    public static TextureRenderer getResetIconRenderer() {
        return resetIconRenderer;
    }

    public static TextureRenderer getCloudIconRenderer() {
        return cloudIconRenderer;
    }

    public static TextureRenderer getCloud2IconRenderer() {
        return cloud2IconRenderer;
    }

    public static TextureRenderer getPlusIconRenderer() {
        return plusIconRenderer;
    }

    public static TextureRenderer getConsoleIconRenderer() {
        return consoleIconRenderer;
    }

    public static TextureRenderer getPersonIconRenderer() {
        return personIconRenderer;
    }

    public static TextureRenderer getMinimalistLogoRenderer() {
        return minimalistLogoRenderer;
    }

    public static class Texture {
        private int id;
        private int width;
        private int height;

        public void set(int id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }

        public int getId() {
            return this.id;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }
    }

    public record UploadData(int id, int width, int height) {
    }
}
