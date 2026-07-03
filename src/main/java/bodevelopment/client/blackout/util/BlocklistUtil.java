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

import bodevelopment.client.blackout.annotations.Internal;

import com.google.common.hash.Hashing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Internal
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
            } catch (Exception ignored) {
            }
        });
    }

    public static boolean isBlocked(String address) {
        if (!loaded || address == null) return false;

        @SuppressWarnings("deprecation")
        String serverHash = Hashing.sha1()
                .hashString(address.toLowerCase(), StandardCharsets.ISO_8859_1)
                .toString();

        return BLOCKED_HASHES.contains(serverHash);
    }
}