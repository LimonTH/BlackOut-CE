package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.ToggleCommand;
import net.minecraft.ChatFormatting;

public class DisableCommand extends ToggleCommand {
    public DisableCommand() {
        super("disable", ChatFormatting.RED);
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
