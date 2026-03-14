package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystal;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystal.class)
public class MixinEndCrystal implements IEndCrystal {
    @Unique
    private final long spawnTime = System.currentTimeMillis();
    @Unique
    private boolean isOwn = false;

    @Override
    public long blackout_Client$getSpawnTime() {
        return this.spawnTime;
    }

    @Override
    public boolean blackout_Client$isOwn() {
        return this.isOwn;
    }

    @Override
    public void blackout_Client$markOwn() {
        this.isOwn = true;
    }

    @Inject(method = "showsBottom", at = @At("HEAD"), cancellable = true)
    private void cancelBottom(CallbackInfoReturnable<Boolean> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.crystalBase.get()) {
            cir.setReturnValue(false);
        }
    }
}
