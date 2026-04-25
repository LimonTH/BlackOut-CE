package bodevelopment.client.blackout.util;

import com.google.common.hash.Hashing;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BlocklistUtil {
    private static final Set<String> BLOCKED_HASHES = new HashSet<>();
    private static boolean loaded = false;

    public static void loadBlocklist() {
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = URI.create("https://sessionserver.mojang.com/blockedservers");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
                    reader.lines().forEach(line -> BLOCKED_HASHES.add(line.trim().toLowerCase()));
                }
                loaded = true;
            } catch (Exception ignored) { }
        });
    }

    public static boolean isBlocked(String address) {
        if (!loaded || address == null) return false;

        String serverHash = Hashing.sha256()
                .hashString(address.toLowerCase(), StandardCharsets.ISO_8859_1)
                .toString();

        return BLOCKED_HASHES.contains(serverHash);
    }
}