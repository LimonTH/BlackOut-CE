package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;
import net.minecraft.client.network.ServerInfo;

import java.util.Collections;
import java.util.List;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug", "Usage: debug [send]");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0 && args[0].equals("send")) {

            String ip = "Main Menu";

            if (BlackOut.mc.isInSingleplayer()) {
                ip = "Singleplayer";
            }
            if (!BlackOut.mc.isInSingleplayer() && BlackOut.mc.getNetworkHandler() != null) {
                ServerInfo serverInfo = BlackOut.mc.getNetworkHandler().getServerInfo();
                if (serverInfo != null) {
                    ip = serverInfo.address;
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
