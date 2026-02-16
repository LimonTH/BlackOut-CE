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

    public void swapBack() {
        switch (this) {
            case Silent:
                InvUtils.swapBack();
                break;
            case InvSwitch:
                InvUtils.invSwapBack();
                break;
            case PickSilent:
                InvUtils.pickSwapBack();
        }
    }

    public boolean swap(int slot) {
        return switch (this) {
            case Silent, Normal -> {
                InvUtils.swap(slot);
                yield true;
            }
            case InvSwitch -> {
                InvUtils.invSwap(slot);
                yield true;
            }
            case PickSilent -> {
                InvUtils.pickSwap(slot);
                yield true;
            }
            default -> false;
        };
    }

    public void swapBackInstantly() {
        switch (this) {
            case Silent:
                InvUtils.swapBackInstantly();
                break;
            case InvSwitch:
                InvUtils.invSwapBackInstantly();
                break;
            case PickSilent:
                InvUtils.pickSwapBackInstantly();
        }
    }

    public boolean swapInstantly(int slot) {
        return switch (this) {
            case Silent, Normal -> {
                InvUtils.swapInstantly(slot);
                yield true;
            }
            case InvSwitch -> {
                InvUtils.invSwapInstantly(slot);
                yield true;
            }
            case PickSilent -> {
                InvUtils.pickSwapInstantly(slot);
                yield true;
            }
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
