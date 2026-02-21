package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.ToggleCommand;
import net.minecraft.util.Formatting;

public class DisableCommand extends ToggleCommand {
    public DisableCommand() {
        super("disable", Formatting.RED);
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
