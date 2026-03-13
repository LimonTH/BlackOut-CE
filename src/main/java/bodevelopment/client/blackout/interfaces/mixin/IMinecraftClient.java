package bodevelopment.client.blackout.interfaces.mixin;


import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.User;

public interface IMinecraftClient {
    void blackout_Client$setSession(String name, UUID uuid, String token, String xuid, String clientId, User.Type accountType);

    void blackout_Client$setSession(User session);

    void blackout_Client$useItem();
}
