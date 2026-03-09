package bodevelopment.client.blackout.interfaces.mixin;

import java.util.UUID;

public interface IUUIDHolder {
    UUID blackout$getUUID();
    void blackout$setUUID(UUID uuid);
}