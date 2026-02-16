package bodevelopment.client.blackout.interfaces.mixin;

import net.minecraft.text.Text;

public interface IChatHudLine {
    void blackout_Client$setId(int id);
    boolean blackout_Client$idEquals(int id);
    void blackout_Client$setSpam(int count);
    int blackout_Client$getSpam();
    void blackout_Client$setMessage(Text text);
    Text blackout_Client$getMessage();
}
