package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.manager.Managers;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Usage: help");
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }

    @Override
    public String execute(String[] args) {
        StringBuilder sb = new StringBuilder("Available commands:\n");

        List<Command> sorted = new ArrayList<>(Managers.COMMANDS.getCommands());
        sorted.sort(Comparator.comparing(c -> c.name));

        for (Command cmd : sorted) {
            sb.append(ChatFormatting.GREEN).append(cmd.name)
              .append(ChatFormatting.GRAY).append(" - ").append(cmd.format);
            if (cmd != sorted.getLast()) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
