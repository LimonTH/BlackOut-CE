package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;

public class FPS extends TextElement {
    public FPS() {
        super("FPS", "Displays the client's current frames per second to monitor graphical performance.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.drawElement(this.stack, "FPS:", String.valueOf(BlackOut.mc.getCurrentFps()));
        }
    }
}
