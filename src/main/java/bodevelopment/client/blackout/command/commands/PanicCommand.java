package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PanicCommand extends Command {
    private final Map<Module, Boolean> states = new HashMap<>();
    private boolean isOn = false;

    public PanicCommand() {
        super("panic", "Usage: panic [on, off]");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            String panicAction = args[0];
            switch (panicAction) {
                case "on":
                    if (this.isOn) {
                        return "already panicking";
                    }

                    this.states.clear();
                    Managers.MODULES.getModules().forEach(m -> {
                        this.states.put(m, m.enabled);
                        if (m.enabled) {
                            m.disable();
                        }
                    });
                    this.isOn = true;
                    return "started panicking";
                case "off":
                    if (!this.isOn) {
                        return "already stopped panicking";
                    }

                    this.states.forEach((m, s) -> {
                        if (m.enabled != s) {
                            m.toggle();
                        }
                    });
                    this.isOn = false;
                    return "stopped panicking";
            }
        }
        return this.format;
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of(
                    "on",
                    "off"
            );
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
