package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.FriendsManager;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Collections;
import java.util.List;

public class FriendCommand extends Command {
    public FriendCommand() {
        super("friends", "Usage: friends [add, remove, list] <name>");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            String friendAction = args[0];
            switch (friendAction) {
                case "add":
                    if (args.length < 2) {
                        return "Who should be added?";
                    }

                    PlayerListEntry entry = this.getEntry(args[1]);
                    if (entry != null) {
                        return Managers.FRIENDS.add(entry.getProfile().getName(), entry.getProfile().getId());
                    }

                    return Managers.FRIENDS.add(args[1], null);
                case "remove":
                    if (args.length == 1) {
                        return "Who should be removed?";
                    }

                    return Managers.FRIENDS.remove(args[1]);
                case "list":
                    StringBuilder builder = new StringBuilder();
                    List<FriendsManager.Friend> friends = Managers.FRIENDS.getFriends();
                    int i = 0;

                    for (int length = friends.size(); i < length; i++) {
                        builder.append(friends.get(i).getName());
                        if (i < length - 1) {
                            builder.append("\n");
                        }
                    }

                    return builder.toString();
            }
        }

        return this.format;
    }

    private PlayerListEntry getEntry(String name) {
        for (PlayerListEntry entry : BlackOut.mc.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile().getName().equalsIgnoreCase(name)) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of(
                    "add",
                    "remove",
                    "list"
            );
        }
        if (args.length == 2) {
            return List.of(
                    "<name>"
            );
        }
        return Collections.emptyList();
    }
}
