package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IChatHudLine;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public class MixinChatHudLine implements IChatHudLine {
    @Unique
    private int id;
    @Unique
    private Component message;
    @Unique
    private int spam;

    @Override
    public void blackout_Client$setId(int id) {
        this.id = id;
    }

    @Override
    public boolean blackout_Client$idEquals(int id) {
        return this.id == id;
    }

    @Override
    public Component blackout_Client$getMessage() {
        return this.message;
    }

    @Override
    public void blackout_Client$setMessage(Component message) {
        this.message = message;
    }

    @Override
    public void blackout_Client$setSpam(int spam) {
        this.spam = spam;
    }

    @Override
    public int blackout_Client$getSpam() {
        return this.spam;
    }
}
