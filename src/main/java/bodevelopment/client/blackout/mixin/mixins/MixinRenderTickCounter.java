package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IRenderTickCounter;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DeltaTracker.Timer.class)
public class MixinRenderTickCounter implements IRenderTickCounter {

    @Mutable
    @Shadow
    @Final
    private float msPerTick;

    @Override
    public void blackout_Client$set(float time) {
        this.msPerTick = time;
    }
}