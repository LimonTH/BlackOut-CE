package bodevelopment.client.blackout.interfaces.mixin;


import net.minecraft.network.chat.Component;

public interface IChatComponent {
    void blackout_Client$addMessageToChat(Component text, int id);
}
