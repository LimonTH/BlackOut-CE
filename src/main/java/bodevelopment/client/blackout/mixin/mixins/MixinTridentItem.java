package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.movement.FastRiptide;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TridentItem.class)
public abstract class MixinTridentItem {
    @Shadow
    public abstract void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks);

    @ModifyVariable(method = "onStoppedUsing", at = @At("HEAD"), index = 4, argsOnly = true)
    private int preUse(int value) {
        FastRiptide fastRiptide = FastRiptide.getInstance();
        if (!fastRiptide.enabled) {
            return value;
        } else if (System.currentTimeMillis() - fastRiptide.prevRiptide < fastRiptide.cooldown.get() * 1000.0) {
            return Integer.MAX_VALUE;
        } else {
            fastRiptide.prevRiptide = System.currentTimeMillis();
            return 0;
        }
    }
}
