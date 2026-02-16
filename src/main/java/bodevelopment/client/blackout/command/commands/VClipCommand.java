package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;

import java.util.Collections;
import java.util.List;

public class VClipCommand extends Command {
    public VClipCommand() {
        super("vclip", "Usage: vclip <ydist>");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            try {
                double value = Double.parseDouble(args[0].replace(",", "."));
                BlackOut.mc
                        .player
                        .setPosition(BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + value, BlackOut.mc.player.getZ());
                return "Teleported " + value + " blocks horizontally.";
            } catch (Exception e) {
                return "invalid amount";
            }
        }

        return this.format;
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of(
                    "<ydist>"
            );
        }
        return Collections.emptyList();
    }
}
