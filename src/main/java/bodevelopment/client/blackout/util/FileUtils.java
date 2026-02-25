package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Formatting;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;

public class FileUtils {
    public static File dir = null;

    public static void init() {
        (dir = new File(BlackOut.RUN_DIRECTORY, "bodevelopment/client/blackout")).mkdir();
        addFolder("fonts");
        addFile("friends.json");
        Managers.FRIENDS.read();
    }

    public static boolean exists(String... path) {
        return getFile(path).exists();
    }

    public static File getFile(String... path) {
        File file = dir;

        for (String string : path) {
            file = new File(file, string);
        }

        return file;
    }

    public static void addFile(String... path) {
        addFile(getFile(path));
    }

    public static void addFile(File file) {
        if (!file.exists()) {
            try {
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void addFolder(String... path) {
        File folder = getFile(path);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    public static void write(File file, JsonObject object) {
        write(file, object.toString());
    }

    public static void write(File file, String content) {
        try {
            Files.writeString(file.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            BOLogger.error("Failed to write file " + file + " with error: ", e);
        }
    }

    public static JsonObject read(String... path) {
        return read(getFile(path));
    }

    public static JsonObject read(File file) {
        JsonElement element = readElement(file);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return new JsonObject();
    }

    public static JsonElement readElement(String... path) {
        return readElement(getFile(path));
    }

    public static JsonElement readElement(File file) {
        return JsonParser.parseString(readString(file));
    }

    public static String readString(File file) {
        try {
            if (!file.exists()) return "";
            return Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    public static URL getResource(String... path) {
        return FileUtils.class.getResource(getPathToResource(path));
    }

    public static InputStream getResourceStream(String... path) {
        return FileUtils.class.getResourceAsStream(getPathToResource(path));
    }

    private static String getPathToResource(String... path) {
        StringBuilder builder = new StringBuilder();
        if (!path[0].equals("assets")) {
            builder.append("/assets/bodevelopment/client/blackout");
        }

        for (String d : path) {
            if (!builder.toString().endsWith("/")) builder.append("/");
            builder.append(d);
        }

        return builder.toString();
    }

    public static BufferedImage readResourceImage(String... path) {
        try (InputStream stream = getResourceStream(path)) {
            if (stream == null) return null;
            return ImageIO.read(stream);
        } catch (IOException e) {
            BOLogger.error("Failed to read resource image " + Arrays.toString(path) + " with error: ", e);
            return null;
        }
    }

    public static void openDirectory(File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(folder);
                return;
            } catch (IOException e) {
                BOLogger.error("Desktop API failed, trying manual launch...", e);
            }
        }

        String path = folder.getAbsolutePath();
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", path).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", path).start();
            } else {
                new ProcessBuilder("xdg-open", path).start();
            }
        } catch (IOException e) {
            BOLogger.error("Failed to open directory " + path, e);
        }
    }

    public static void openLink(String url) {
        try {
            URI uri = new java.net.URI(url);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("win")) {
                    pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
                } else if (os.contains("mac")) {
                    pb = new ProcessBuilder("open", url);
                } else {
                    pb = new ProcessBuilder("xdg-open", url);
                }
                pb.start();
            }
        } catch (Exception e) {
            BOLogger.error("Could not open link: " + url, e);
        }
    }
}
