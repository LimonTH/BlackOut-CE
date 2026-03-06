package bodevelopment.client.blackout.enums;

import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

public enum SwitchMode {
    Disabled(false, false),
    Normal(true, false),
    Silent(true, false),
    InvSwitch(true, true),
    PickSilent(true, true);

    public final boolean hotbar;
    public final boolean inventory;

    SwitchMode(boolean h, boolean i) {
        this.hotbar = h;
        this.inventory = i;
    }

    public boolean swapBack() {
        return switch (this) {
            case Silent -> InvUtils.swapBack();
            case InvSwitch -> InvUtils.invSwapBack();
            case PickSilent -> InvUtils.pickSwapBack();
            default -> false;
        };
    }

    public boolean swap(int slot) {
        return switch (this) {
            case Silent, Normal -> InvUtils.swap(slot);
            case InvSwitch -> InvUtils.invSwap(slot);
            case PickSilent -> InvUtils.pickSwap(slot);
            default -> false;
        };
    }

    public boolean swapBackInstantly() {
        return switch (this) {
            case Silent -> InvUtils.swapBackInstantly();
            case InvSwitch -> InvUtils.invSwapBackInstantly();
            case PickSilent -> InvUtils.pickSwapBackInstantly();
            default -> false;
        };
    }

    public boolean swapInstantly(int slot) {
        return switch (this) {
            case Silent, Normal -> InvUtils.swapInstantly(slot);
            case InvSwitch -> InvUtils.invSwapInstantly(slot);
            case PickSilent -> InvUtils.pickSwapInstantly(slot);
            default -> false;
        };
    }

    public FindResult find(Predicate<ItemStack> predicate) {
        return InvUtils.find(this.hotbar, this.inventory, predicate);
    }

    public FindResult find(Item item) {
        return InvUtils.find(this.hotbar, this.inventory, item);
    }
}
