package bodevelopment.client.blackout.keys;

import bodevelopment.client.blackout.annotations.PublicAPI;

@PublicAPI
public class Pressable {
    public int key;

    public Pressable(int key) {
        this.key = key;
    }

    public String getName() {
        return "sus";
    }

    public boolean isPressed() {
        return true;
    }
}
