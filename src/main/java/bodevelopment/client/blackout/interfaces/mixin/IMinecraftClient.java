package bodevelopment.client.blackout.interfaces.mixin;


import net.minecraft.client.session.Session;

import java.util.Optional;
import java.util.UUID;

public interface IMinecraftClient {
    void blackout_Client$setSession(String name, UUID uuid, String token, String xuid, String clientId, Session.AccountType accountType);

    void blackout_Client$setSession(Session session);

    void blackout_Client$useItem();
}
