package bodevelopment.client.blackout.randomstuff;

import net.minecraft.item.ItemStack;

public record FindResult(int slot, int amount, ItemStack stack) {
    public boolean wasFound() {
        return this.slot > -1;
    }
}
