package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IUUIDHolder;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerRenderState.class)
public class MixinPlayerRenderState implements IUUIDHolder {
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