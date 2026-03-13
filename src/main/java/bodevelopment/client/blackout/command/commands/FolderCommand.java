package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.util.FileUtils;
import java.awt.*;
import java.io.File;
import net.minecraft.ChatFormatting;

public class FolderCommand extends Command {
    public int fakePlayerID = 0;

    public FolderCommand() {
        super("folder", "Usage: folder");
    }

    @Override
    public String execute(String[] args) {
        File folder = FileUtils.getFile("configs");
        try {
            FileUtils.openDirectory(folder);
            return "Opening folder: " + folder.getName();
        } catch (Exception e) {
            return String.format("Error: Could not open folder: " + e.getMessage(), ChatFormatting.RED.toString());
        }
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
