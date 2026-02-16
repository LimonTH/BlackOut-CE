package bodevelopment.client.blackout.interfaces.mixin;

import net.minecraft.client.gui.hud.ChatHudLine;

public interface IVisible {
    void blackout_Client$set(int id);

    boolean blackout_Client$idEquals(int id);

    boolean blackout_Client$messageEquals(ChatHudLine hudLine);

    void blackout_Client$setLine(ChatHudLine hudLine);
}
