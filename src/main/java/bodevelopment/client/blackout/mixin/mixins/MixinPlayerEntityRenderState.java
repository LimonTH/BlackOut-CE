package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IUUIDHolder;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntityRenderState.class)
public class MixinPlayerEntityRenderState implements IUUIDHolder {
    @Unique
    private java.util.UUID blackout$uuid;

    @Override
    public java.util.UUID blackout$getUUID() {
        return this.blackout$uuid;
    }

    @Override
    public void blackout$setUUID(java.util.UUID uuid) {
        this.blackout$uuid = uuid;
    }
}