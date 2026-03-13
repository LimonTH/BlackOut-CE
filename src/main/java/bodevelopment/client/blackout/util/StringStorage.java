package bodevelopment.client.blackout.util;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.Mth;

public class StringStorage {
    private static final String[] adjectives = new String[]{
            "Fat",
            "Goofy",
            "Funny",
            "Sad",
            "Mad",
            "Large",
            "Former",
            "Massive",
            "Huge",
            "Angry",
            "Legal",
            "Nice",
            "Cute",
            "Happy",
            "Poor",
            "Hot",
            "Strong",
            "Known",
            "Scared",
            "Old",
            "Fast",
            "Epic",
            "Best",
            "Wide",
            "Smart"
    };
    private static final String[] substantives = new String[]{
            "Dog",
            "Pig",
            "Bear",
            "Player",
            "Salmon",
            "Fish",
            "Sheep",
            "Cow",
            "Bat",
            "Goose",
            "Ostrich",
            "Emu",
            "Kiwi",
            "Hog",
            "Sloth",
            "Noob",
            "Person",
            "Kid",
            "Rat",
            "Mouse",
            "Cat",
            "Bird"
    };

    public static String randomAdj() {
        return getRandom(adjectives);
    }

    public static String randomSub() {
        return getRandom(substantives);
    }

    private static <T> T getRandom(T[] array) {
        return array[(int) Math.round(Mth.lerp(ThreadLocalRandom.current().nextDouble(), 0.0, array.length - 1))];
    }
}
