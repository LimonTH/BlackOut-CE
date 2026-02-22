package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.ClassUtils;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandManager extends Manager {
    private final Map<String, Command> commands = new HashMap<>();
    public String prefix = "-";

    @Override
    public void init() {
        String internalPath = Command.class.getCanonicalName().replace(Command.class.getSimpleName(), "commands");

        ClassUtils.forEachClass(clazz -> {
            if (Command.class.isAssignableFrom(clazz)
                    && !clazz.isInterface()
                    && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {

                String name = clazz.getSimpleName().toLowerCase();
                if (!this.commands.containsKey(name)) {
                    this.add((Command) ClassUtils.instance((Class<?>) clazz));
                }
            }
        }, internalPath, Command.class.getClassLoader());
    }

    public void add(Command command) {
        if (command == null) return;
        this.commands.putIfAbsent(command.name.toLowerCase(), command);
    }

    public String onCommand(String[] args) {
        if (args.length == 0) return null;

        String commandName = args[0].toLowerCase();
        if (this.commands.containsKey(commandName)) {
            Command command = this.commands.get(commandName);

            try {
                if (BlackOut.mc.player == null && !command.canUseOutsideWorld()) {
                    return String.format("[%s]%s This command can only be used in-game!",
                            command.name, Formatting.RED);
                }

                String respond = command.execute(Arrays.copyOfRange(args, 1, args.length));
                return String.format("[%s]%s %s", command.name, Formatting.GRAY, respond);

            } catch (Exception e) {
                BOLogger.warn("An error occurred while executing command " + Arrays.toString(args) + " : " + e);
                return String.format("[%s]%s Error: %s",
                        command.name, Formatting.RED, "An error occurred while executing command.");
            }
        } else {
            return null;
        }
    }

    public CompletableFuture<Suggestions> getCommandSuggestions(SuggestionsBuilder builder) {
        String text = builder.getInput();
        int lastSpace = text.lastIndexOf(' ');
        int offset = lastSpace == -1 ? 1 : lastSpace + 1;

        SuggestionsBuilder subBuilder = builder.createOffset(offset);
        String currentWord = subBuilder.getRemaining().toLowerCase();

        String content = text.substring(prefix.length());
        String[] args = content.split(" ", -1);

        if (args.length <= 1) {
            for (Command cmd : commands.values()) {
                if (cmd.name.toLowerCase().startsWith(currentWord)) {
                    subBuilder.suggest(cmd.name);
                }
            }
        } else {
            Command cmd = commands.get(args[0].toLowerCase());
            if (cmd != null) {
                List<String> suggestions = cmd.getSuggestions(Arrays.copyOfRange(args, 1, args.length));
                for (String s : suggestions) {
                    if (s.toLowerCase().startsWith(currentWord)) {
                        subBuilder.suggest(s);
                    }
                }
            }
        }
        return subBuilder.buildFuture();
    }
}
