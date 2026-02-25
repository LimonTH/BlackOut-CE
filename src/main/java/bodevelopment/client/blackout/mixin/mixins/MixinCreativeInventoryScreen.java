package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.ICreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CreativeInventoryScreen.class)
public abstract class MixinCreativeInventoryScreen implements ICreativeInventoryScreen {

    @Shadow
    private static ItemGroup selectedTab;

    @Override
    public ItemGroup blackout_Client$getSelectedTab() {
        return selectedTab;
    }
}