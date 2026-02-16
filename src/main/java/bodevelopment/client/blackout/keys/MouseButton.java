package bodevelopment.client.blackout.keys;

public class MouseButton extends Pressable {
    public MouseButton(int key) {
        super(key);
    }

    @Override
    public boolean isPressed() {
        return this.key != -1 && MouseButtons.get(this.key);
    }

    @Override
    public String getName() {
        return MouseButtons.getKeyName(this.key);
    }
}
