package bodevelopment.client.blackout.gui.menu;

import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.util.StringStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

public class AccountScriptReader {
    private static final Map<SingleOut<String>, String[]> commands = new HashMap<>();
    private static final List<Pair<String, String>> braces = new ArrayList<>();

    static {
        addCommand(StringStorage::randomAdj, "rndAdj", "rndadj", "ra");
        addCommand(StringStorage::randomSub, "rndSub", "rndsub", "rs");
        addCommand(() -> String.valueOf((int) Math.floor(ThreadLocalRandom.current().nextDouble() * 10.0 - 1.0)), "1", "n1", "num1");
        addCommand(() -> String.valueOf((int) Math.floor(ThreadLocalRandom.current().nextDouble() * 100.0 - 1.0)), "2", "n2", "num2");
        addCommand(() -> String.valueOf((int) Math.floor(ThreadLocalRandom.current().nextDouble() * 1000.0 - 1.0)), "3", "n3", "num3");
        addCommand(() -> String.valueOf((int) Math.floor(ThreadLocalRandom.current().nextDouble() * 10000.0 - 1.0)), "4", "n4", "num4");
        addCommand(() -> String.valueOf((int) Math.floor(ThreadLocalRandom.current().nextDouble() * 100000.0 - 1.0)), "5", "n5", "num5");
        braces.add(new Pair<>("{", "}"));
        braces.add(new Pair<>("[", "]"));
        braces.add(new Pair<>("(", ")"));
        braces.add(new Pair<>("'", "'"));
        braces.add(new Pair<>("<", ">"));
    }

    private static void addCommand(SingleOut<String> singleOut, String... usages) {
        commands.put(singleOut, usages);
    }

    public static String nameFromScript(String string) {
        for (Entry<SingleOut<String>, String[]> entry : commands.entrySet()) {
            SingleOut<String> mod = entry.getKey();

            for (String key : entry.getValue()) {
                for (Pair<String, String> pair : braces) {
                    string = string.replace(pair.getLeft() + key + pair.getRight(), mod.get());
                }
            }
        }

        return string;
    }
}
