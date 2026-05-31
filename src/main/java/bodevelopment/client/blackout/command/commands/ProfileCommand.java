package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.event.EventBus;

import java.util.Collections;
import java.util.List;

public class ProfileCommand extends Command {
    public ProfileCommand() {
        super("profile", "Usage: profile [on, off, report, reset, status]");
    }

    @Override
    public String execute(String[] args) {
        if (args.length == 0) {
            return "Profiling is " + (EventBus.profiling ? "ON" : "OFF") + ". Usage: " + this.format;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> {
                if (EventBus.profiling) return "Profiling is already ON.";
                EventBus.profiling = true;
                EventBus.resetProfileData();
                return "Profiling enabled. Use -profile report to view results.";
            }
            case "off" -> {
                if (!EventBus.profiling) return "Profiling is already OFF.";
                EventBus.profiling = false;
                return "Profiling disabled. Use -profile reset to clear collected data.";
            }
            case "report" -> {
                if (!EventBus.profiling) return "Profiling is OFF. Enable with -profile on first.";
                String report = EventBus.getProfileReport();
                return report != null ? report : "No profile data collected yet.";
            }
            case "reset" -> {
                EventBus.resetProfileData();
                return "Profile data reset.";
            }
            case "status" -> {
                return "Profiling is " + (EventBus.profiling ? "ON" : "OFF") + ". "
                        + EventBus.profileData.size() + " method(s) tracked.";
            }
            default -> {
                return "Unknown subcommand: " + args[0] + ". Usage: " + this.format;
            }
        }
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of("on", "off", "report", "reset", "status");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
