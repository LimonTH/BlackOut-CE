package bodevelopment.client.blackout.interfaces.mixin;

import net.minecraft.client.GuiMessage;

public interface IVisible {
    void blackout_Client$set(int id);

    boolean blackout_Client$idEquals(int id);

    boolean blackout_Client$messageEquals(GuiMessage hudLine);

    void blackout_Client$setLine(GuiMessage hudLine);
}
