package bodevelopment.client.blackout.mixin.accessors;

import bodevelopment.client.blackout.annotations.Internal;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
@Internal
public interface AccessorAbstractSoundInstance {
    @Accessor("volume")
    void setVolume(float volume);

    @Accessor("pitch")
    void setPitch(float volumePitch);
}
