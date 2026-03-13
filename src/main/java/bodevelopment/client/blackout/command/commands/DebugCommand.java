package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.multiplayer.ServerData;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug", "Usage: debug [send]");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0 && args[0].equals("send")) {

            String ip = "Main Menu";

            if (BlackOut.mc.isLocalServer()) {
                ip = "Singleplayer";
            }
            if (!BlackOut.mc.isLocalServer() && BlackOut.mc.getConnection() != null) {
                ServerData serverInfo = BlackOut.mc.getConnection().getServerData();
                if (serverInfo != null) {
                    ip = serverInfo.ip;
                }
            }

            return BlackOut.NAME + " " + BlackOut.VERSION + " " + BlackOut.TYPE + " " + ip;
        } else {
            return this.format;
        }
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of("send");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
