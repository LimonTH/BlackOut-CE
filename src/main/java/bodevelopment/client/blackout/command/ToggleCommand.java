package bodevelopment.client.blackout.command;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.util.Formatting;

public class ToggleCommand extends Command {
    private final String lowerCase;
    private final String color;

    public ToggleCommand(String action, Formatting formatting) {
        super(action, action.toLowerCase() + " <name>");
        this.lowerCase = action.toLowerCase();
        this.color = formatting.toString();
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            String built = String.join(" ", args);
            String idkRurAmogus = this.doStuff(built);
            Module module = this.getModule(idkRurAmogus);

            if (module == null) {
                Module similar = this.similar(idkRurAmogus);
                return similar != null
                        ? String.format("%s couldn't find %s from modules, did you mean %s", Formatting.RED, built, this.moduleNameString(similar))
                        : String.format("%s couldn't find %s from modules", Formatting.RED, built);
            }

            if (module.toggleable()) {
                if (this.lowerCase.equals("enable")) {
                    if (module.enabled) {
                        return Formatting.YELLOW + this.moduleNameString(module) + " is already enabled!";
                    }
                    module.enable();
                } else if (this.lowerCase.equals("disable")) {
                    if (!module.enabled) {
                        return Formatting.YELLOW + this.moduleNameString(module) + " is already disabled!";
                    }
                    module.disable();
                } else {
                    module.toggle();
                }

                return this.color + this.lowerCase + "d " + Formatting.WHITE + this.moduleNameString(module);
            } else {
                return String.format("%s%s%s is not toggleable", Formatting.GRAY, this.moduleNameString(module), Formatting.RED);
            }
        } else {
            return this.format;
        }
    }

    private String moduleNameString(Module module) {
        return module.name.equals(module.getDisplayName()) ? module.name : String.format("%s (%s)", module.getDisplayName(), module.name);
    }

    private Module similar(String input) {
        Module best = null;
        double highest = 0.0;

        for (Module module : Managers.MODULES.getModules()) {
            double similarity = Math.max(
                    OLEPOSSUtils.similarity(input, this.doStuff(module.name)),
                    OLEPOSSUtils.similarity(input, this.doStuff(module.getDisplayName()))
            );
            if (similarity > highest) {
                best = module;
                highest = similarity;
            }
        }
        return best;
    }

    private Module getModule(String name) {
        Module display = null;
        for (Module module : Managers.MODULES.getModules()) {
            if (name.equals(this.doStuff(module.name))) return module;
            if (name.equals(this.doStuff(module.getDisplayName()))) display = module;
        }
        return display;
    }

    private String doStuff(String string) {
        return string.toLowerCase().replace(" ", "");
    }
}
