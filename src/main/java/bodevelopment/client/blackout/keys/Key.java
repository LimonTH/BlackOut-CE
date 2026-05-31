package bodevelopment.client.blackout.keys;

import bodevelopment.client.blackout.annotations.PublicAPI;

@PublicAPI
public class Key extends Pressable {
    public Key(int key) {
        super(key);
    }

    @Override
    public boolean isPressed() {
        return this.key != -1 && Keys.get(this.key);
    }

    @Override
    public String getName() {
        return Keys.getKeyName(this.key);
    }
}
