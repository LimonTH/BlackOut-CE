package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.ToggleCommand;
import net.minecraft.util.Formatting;

public class EnableCommand extends ToggleCommand {
    public EnableCommand() {
        super("enable", Formatting.GREEN);
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
