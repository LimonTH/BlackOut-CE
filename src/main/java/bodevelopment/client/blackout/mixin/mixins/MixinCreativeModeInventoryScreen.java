package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.ICreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class MixinCreativeModeInventoryScreen implements ICreativeModeInventoryScreen {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Override
    public CreativeModeTab blackout_Client$getSelectedTab() {
        return selectedTab;
    }
}