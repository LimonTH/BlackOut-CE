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

package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.gui.menu.Account;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraft;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Persistable;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.EncryptionUtils;
import bodevelopment.client.blackout.util.FileUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.User;

import java.util.ArrayList;
import java.util.List;

@PublicAPI
public class AltManager extends Manager implements Persistable {
    private final List<Account> accounts = new ArrayList<>();
    public Account selected;
    public Account currentSession;
    private User originalSession;
    private long lastSave = 0L;
    private boolean shouldSave = false;

    private boolean wasInWorld = false;

    @Override
    public void init() {
        this.originalSession = BlackOut.mc.getUser();

        this.selected = new Account(BlackOut.mc.getUser());
        this.currentSession = this.selected;
        BlackOut.EVENT_BUS.subscribe(this, () -> false);

        String encryptedPath = "accounts.dat";
        String legacyPath = "accounts.json";

        if (FileUtils.exists(encryptedPath)) {
            this.loadEncrypted(encryptedPath);
        } else if (FileUtils.exists(legacyPath)) {
            this.loadLegacy(legacyPath);
        } else {
            FileUtils.addFile(encryptedPath);
        }

        this.save();
    }

    /**
     * Loads accounts from AES-GCM encrypted storage.
     */
    private void loadEncrypted(String path) {
        try {
            String encrypted = FileUtils.readString(FileUtils.getFile(path));
            if (encrypted == null || encrypted.isEmpty()) return;

            String json = EncryptionUtils.decryptString(encrypted);
            if (json == null || json.isEmpty()) return;

            JsonElement jsonElement = JsonParser.parseString(json);
            JsonObject jsonObject = jsonElement instanceof JsonNull ? new JsonObject() : (JsonObject) jsonElement;
            jsonObject.entrySet().forEach(entry -> {
                JsonElement element = entry.getValue();
                if (element instanceof JsonObject object) {
                    this.readData(object);
                } else {
                    this.getAccounts().add(new Account(entry.getKey(), null, null, "", null, null, User.Type.MOJANG));
                }
            });
        } catch (Exception e) {
            BOLogger.error("Failed to load encrypted accounts, starting fresh", e);
        }
    }

    /**
     * Migrates legacy plaintext accounts.json to encrypted accounts.dat.
     */
    private void loadLegacy(String path) {
        try {
            JsonElement jsonElement = FileUtils.readElement(FileUtils.getFile(path));
            JsonObject jsonObject = jsonElement instanceof JsonNull ? new JsonObject() : (JsonObject) jsonElement;
            jsonObject.entrySet().forEach(entry -> {
                JsonElement element = entry.getValue();
                if (element instanceof JsonObject object) {
                    this.readData(object);
                } else {
                    this.getAccounts().add(new Account(entry.getKey(), null, null, "", null, null, User.Type.MOJANG));
                }
            });

            // Delete the legacy plaintext file after successful migration
            FileUtils.getFile(path).delete();
            BOLogger.info("Migrated legacy accounts.json to encrypted accounts.dat");
        } catch (Exception e) {
            BOLogger.error("Failed to migrate legacy accounts", e);
        }
    }

    private void readData(JsonObject jsonObject) {
        this.add(new Account(jsonObject));
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        boolean inWorld = BlackOut.mc.level != null;

        if (!inWorld && wasInWorld) {
            switchToSelected();
            wasInWorld = false;
        }

        if (inWorld && !wasInWorld) {
            wasInWorld = true;
        }

        if (this.shouldSave && System.currentTimeMillis() - this.lastSave > 5000L) {
            this.shouldSave = false;
            JsonObject object = new JsonObject();
            this.getAccounts().forEach(account -> {
                JsonObject accountObject = account.asJson();
                if (accountObject != null) {
                    object.add(account.getScript(), accountObject);
                }
            });

            String json = object.toString();
            String encrypted = EncryptionUtils.encryptString(json);
            if (encrypted != null) {
                FileUtils.write(FileUtils.getFile("accounts.dat"), encrypted);
            } else {
                BOLogger.error("Failed to encrypt accounts data — skipping save to prevent data loss");
            }
            this.lastSave = System.currentTimeMillis();
        }
    }

    public void add(Account account) {
        this.getAccounts().add(account);
        this.save();
    }

    public void remove(Account account) {
        this.getAccounts().remove(account);
        this.save();
    }

    public void set(Account account) {
        this.selected = account;
        if (BlackOut.mc.level == null) {
            this.switchToSelected();
        }
        this.save();
    }

    public void switchToOriginal() {
        if (originalSession != null) {
            ((IMinecraft) BlackOut.mc).blackout_Client$setSession(
                    originalSession.getName(),
                    originalSession.getProfileId(),
                    originalSession.getAccessToken(),
                    originalSession.getXuid().orElse(null),
                    originalSession.getClientId().orElse(null),
                    originalSession.getType()
            );
        }
    }

    public void switchToSelected() {
        if (this.selected != null) {
            this.selected.setSession();
        }
    }

    @Override
    public void save() {
        this.shouldSave = true;
    }

    public List<Account> getAccounts() {
        return this.accounts;
    }
}
