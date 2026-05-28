package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;

public class FPS extends TextElement {
    public FPS() {
        super("FPS", "Displays the client's current frames per second to monitor graphical performance.");
    }

    @Override
    public void render() {
        this.drawElement(this.stack, "FPS:", String.valueOf(BlackOut.mc.getFps()));
    }
}
