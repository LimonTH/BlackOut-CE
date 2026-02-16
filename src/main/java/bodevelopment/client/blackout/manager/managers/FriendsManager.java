package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.FileUtils;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FriendsManager extends Manager {
    private final List<Friend> friends = new ArrayList<>();
    private boolean shouldSave = false;
    private long prevSave = 0L;

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.player == null || BlackOut.mc.world == null);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (this.shouldSave && System.currentTimeMillis() - this.prevSave > 10000L) {
            this.writeFriends();
        }

        Collection<PlayerListEntry> entries = BlackOut.mc.getNetworkHandler().getPlayerList();
        if (entries != null) {
            entries.forEach(entry -> {
                GameProfile profile = entry.getProfile();
                String name = profile.getName();
                UUID uuid = profile.getId();
                Friend friend = this.getAsFriend(name, uuid);
                if (friend != null) {
                    boolean nameEquals = friend.getName().equalsIgnoreCase(name);
                    if (!friend.seen() && nameEquals) {
                        friend.setUuid(uuid);
                    }

                    if (uuid.equals(friend.getUuid()) && !nameEquals) {
                        friend.setName(name);
                    }
                }
            });
        }
    }

    public String add(String name, UUID uuid) {
        File file = FileUtils.getFile("friends.json");
        FileUtils.addFile(file);
        Friend friend = new Friend(name, uuid);
        this.friends.add(friend);
        this.save();
        return friend.seen()
                ? String.format("added %s to friends list", name)
                : String.format("added %s to friends list, couldnt get UUID yet but it will be updated when you see them in game", name);
    }

    public String remove(String name) {
        for (Friend friend : this.friends) {
            if (friend.getName().equalsIgnoreCase(name)) {
                this.friends.remove(friend);
                this.save();
                return String.format("removed %s from friends list", friend.getName());
            }
        }

        return String.format("%s was not in friends list", name);
    }

    private void writeFriends() {
        File file = FileUtils.getFile("friends.json");
        FileUtils.addFile(file);
        JsonObject jsonObject = new JsonObject();
        this.friends.forEach(friend -> jsonObject.addProperty(friend.getName(), friend.seen() ? friend.getUuid().toString() : "<NULL>"));
        this.prevSave = System.currentTimeMillis();
        FileUtils.write(file, jsonObject);
    }

    private Friend getAsFriend(String name, UUID uuid) {
        for (Friend friend : this.friends) {
            if (friend.seen()) {
                if (uuid.equals(friend.getUuid())) {
                    return friend;
                }
            } else if (name.equalsIgnoreCase(friend.getName())) {
                return friend;
            }
        }

        return null;
    }

    public void read() {
        this.getFriends().clear();
        File file = FileUtils.getFile("friends.json");
        if (file.exists()) {
            if (FileUtils.readElement(file) instanceof JsonObject jsonObject) {
                jsonObject.entrySet().forEach(entry -> {
                    String uuidString = entry.getValue().getAsString();
                    this.friends.add(new Friend(entry.getKey(), uuidString.equals("<NULL>") ? null : UUID.fromString(uuidString)));
                });
            }
        }
    }

    public void save() {
        this.shouldSave = true;
    }

    public boolean isFriend(PlayerEntity player) {
        for (Friend friend : this.friends) {
            if (friend.getName().equalsIgnoreCase(player.getGameProfile().getName())) {
                return true;
            }
        }

        return false;
    }

    public List<Friend> getFriends() {
        return this.friends;
    }

    public static class Friend {
        private String name;
        private UUID uuid;

        private Friend(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
            Managers.FRIENDS.save();
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        public boolean seen() {
            return this.uuid != null;
        }
    }
}
