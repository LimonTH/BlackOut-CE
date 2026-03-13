package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.ToggleCommand;
import net.minecraft.ChatFormatting;

public class EnableCommand extends ToggleCommand {
    public EnableCommand() {
        super("enable", ChatFormatting.GREEN);
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
