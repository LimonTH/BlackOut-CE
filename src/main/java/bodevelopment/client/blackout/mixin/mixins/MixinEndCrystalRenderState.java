package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalState;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EndCrystalEntityRenderState.class)
public class MixinEndCrystalRenderState implements IEndCrystalState {
    @Unique
    private long spawnTime;

    @Override
    public void blackout_Client$setSpawnTime(long time) { this.spawnTime = time; }

    @Override
    public long blackout_Client$getSpawnTime() { return this.spawnTime; }
}