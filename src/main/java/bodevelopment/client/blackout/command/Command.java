package bodevelopment.client.blackout.command;

import java.util.Collections;
import java.util.List;

public class Command {
    public final String name;
    public final String usage;
    public final String format;

    public Command(String name, String format) {
        this.name = name;
        this.usage = name.toLowerCase();
        this.format = format;
    }

    public boolean canUseOutsideWorld() {
        return false;
    }

    public List<String> getSuggestions(String[] args) {
        return Collections.emptyList();
    }

    public String execute(String[] args) {
        return null;
    }
}
