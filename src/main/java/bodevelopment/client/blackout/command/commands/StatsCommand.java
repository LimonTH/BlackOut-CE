package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.manager.Managers;

import java.util.Collections;
import java.util.List;

public class StatsCommand extends Command {
    public StatsCommand() {
        super("Stats", "Usage: Stats [reset]");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0 && args[0].equals("reset")) {
            Managers.STATS.reset();
            return "Successfully reset stats.";
        } else {
            return this.format;
        }
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of(
                    "reset"
            );
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
