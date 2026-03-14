package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalRenderState;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EndCrystalRenderState.class)
public class MixinEndCrystalRenderState implements IEndCrystalRenderState {
    @Unique
    private long spawnTime;

    @Override
    public void blackout_Client$setSpawnTime(long time) { this.spawnTime = time; }

    @Override
    public long blackout_Client$getSpawnTime() { return this.spawnTime; }
}