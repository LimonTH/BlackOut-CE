package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.client.gui.screen.TitleScreen;

public class ClickGuiManager extends Manager {
    public final ClickGui CLICK_GUI = new ClickGui();

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
        BlackOut.EVENT_BUS.subscribe(this.CLICK_GUI, () -> false);
    }

    @Event
    public void onKey(KeyEvent event) {
        if (!event.pressed || BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        if (event.key == 344) {
            if (BlackOut.mc.currentScreen == null || BlackOut.mc.currentScreen instanceof ClickGui) {
                this.toggle();
            }
        }
    }

    public void openScreen(ClickGuiScreen screen) {
        if (BlackOut.mc.currentScreen instanceof ClickGui || BlackOut.mc.currentScreen instanceof TitleScreen) {
            this.CLICK_GUI.setScreen(screen);
        } else {
            Managers.HUD.HUD_EDITOR.setScreen(screen);
        }
    }

    private void toggle() {
        if (this.CLICK_GUI.isOpen()) {
            if (System.currentTimeMillis() - this.CLICK_GUI.toggleTime < 500L) return;

            this.CLICK_GUI.toggleTime = System.currentTimeMillis();
            this.CLICK_GUI.setOpen(false);
        } else {
            if (System.currentTimeMillis() - this.CLICK_GUI.toggleTime < 250L) return;

            this.CLICK_GUI.toggleTime = System.currentTimeMillis();
            this.CLICK_GUI.setOpen(true);
            this.CLICK_GUI.initGui();
            BlackOut.mc.setScreen(this.CLICK_GUI);
        }
    }
}