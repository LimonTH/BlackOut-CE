package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;

import java.util.Collections;
import java.util.List;

public class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip", "Usage: hclip <xdist>");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            try {
                double value = Double.parseDouble(args[0].replace(",", "."));
                double yaw = Math.toRadians(BlackOut.mc.player.getYaw() + 90.0F);
                BlackOut.mc.player.setPosition(
                        BlackOut.mc.player.getX() + Math.cos(yaw) * value,
                        BlackOut.mc.player.getY(),
                        BlackOut.mc.player.getZ() + Math.sin(yaw) * value
                );
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
                    "<xdist>"
            );
        }
        return Collections.emptyList();
    }
}
