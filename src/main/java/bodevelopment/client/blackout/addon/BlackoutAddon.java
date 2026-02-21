package bodevelopment.client.blackout.addon;

import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.command.Command;
import java.util.List;
import java.util.ArrayList;

public abstract class BlackoutAddon {
    private final String name;
    public final String modulePath;
    public final String commandPath;
    public final String hudPath;

    public final List<Module> modules = new ArrayList<>();
    public final List<Command> commands = new ArrayList<>();
    public final List<HudElement> hudElements = new ArrayList<>();

    protected BlackoutAddon(String name, String modulePath, String commandPath, String hudPath) {
        this.name = name;
        this.modulePath = modulePath;
        this.commandPath = commandPath;
        this.hudPath = hudPath;
    }

    public abstract void onInitialize();

    public String getName() { return name; }

    public String getAuthor() { return "Limon_TH"; }
    public String getDescription() { return "A simple addon for BlackOut Client."; }
    public String getVersion() { return "2.0.0"; }
}