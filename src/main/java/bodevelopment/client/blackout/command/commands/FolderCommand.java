package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.util.FileUtils;

import java.awt.*;

public class FolderCommand extends Command {
    public int fakePlayerID = 0;

    public FolderCommand() {
        super("folder", "Usage: folder");
    }

    @Override
    public String execute(String[] args) {
        java.io.File folder = FileUtils.getFile("configs");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String path = folder.getAbsolutePath();
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", path).start();
            } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                String command = os.contains("mac") ? "open" : "xdg-open";
                new ProcessBuilder(command, path).start();
            } else {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(folder);
                }
            }
            return "Opening folder: " + folder.getName();
        } catch (Exception e) {
            return "§cError: Could not open folder. " + e.getMessage();
        }
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
