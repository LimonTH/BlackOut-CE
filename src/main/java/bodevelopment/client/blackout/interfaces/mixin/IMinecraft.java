package bodevelopment.client.blackout.interfaces.mixin;


import net.minecraft.client.User;

import java.util.UUID;

public interface IMinecraft {
    void blackout_Client$setSession(String name, UUID uuid, String token, String xuid, String clientId, User.Type accountType);

    void blackout_Client$setSession(User session);

    void blackout_Client$useItem();
}
