package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraftClient;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.SettingUtils;

public class Octopus extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<KeyBind> slot1 = this.sgGeneral.keySetting("Bind Slot 1", "Hotkey to silently use the item in hotbar slot 1.");
    private final Setting<KeyBind> slot2 = this.sgGeneral.keySetting("Bind Slot 2", "Hotkey to silently use the item in hotbar slot 2.");
    private final Setting<KeyBind> slot3 = this.sgGeneral.keySetting("Bind Slot 3", "Hotkey to silently use the item in hotbar slot 3.");
    private final Setting<KeyBind> slot4 = this.sgGeneral.keySetting("Bind Slot 4", "Hotkey to silently use the item in hotbar slot 4.");
    private final Setting<KeyBind> slot5 = this.sgGeneral.keySetting("Bind Slot 5", "Hotkey to silently use the item in hotbar slot 5.");
    private final Setting<KeyBind> slot6 = this.sgGeneral.keySetting("Bind Slot 6", "Hotkey to silently use the item in hotbar slot 6.");
    private final Setting<KeyBind> slot7 = this.sgGeneral.keySetting("Bind Slot 7", "Hotkey to silently use the item in hotbar slot 7.");
    private final Setting<KeyBind> slot8 = this.sgGeneral.keySetting("Bind Slot 8", "Hotkey to silently use the item in hotbar slot 8.");
    private final Setting<KeyBind> slot9 = this.sgGeneral.keySetting("Bind Slot 9", "Hotkey to silently use the item in hotbar slot 9.");

    private int toUse = -1;

    public Octopus() {
        super("Octopus", "Allows you to use items from specific hotbar slots using custom hotkeys without manually switching to them.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (this.toUse >= 0) {
            this.use(this.toUse);
        }

        this.toUse = -1;
    }

    @Event
    public void onKey(KeyEvent event) {
        if (event.pressed) {
            int pressed = this.pressedSlot(event.key, false);
            if (pressed >= 0) {
                this.handleClick(pressed);
            }
        }
    }

    @Event
    public void onMouse(MouseButtonEvent event) {
        if (event.pressed) {
            int pressed = this.pressedSlot(event.button, true);
            if (pressed >= 0) {
                this.handleClick(pressed);
            }
        }
    }

    private void handleClick(int slot) {
        if (SettingUtils.grimPackets()) {
            this.toUse = slot;
        } else {
            this.use(slot);
        }
    }

    private void use(int slot) {
        boolean switched = false;
        if (BlackOut.mc.interactionManager.lastSelectedSlot != slot) {
            InvUtils.swap(slot);
            switched = true;
        }

        ((IMinecraftClient) BlackOut.mc).blackout_Client$useItem();
        if (switched) {
            InvUtils.swapBack();
        }
    }

    private int pressedSlot(int key, boolean mouse) {
        for (int i = 0; i < 9; i++) {
            KeyBind b = this.getSetting(i).get();
            if (!mouse && b.isKey(key)) {
                return i;
            }

            if (mouse && b.isMouse(key)) {
                return i;
            }
        }

        return -1;
    }

    private Setting<KeyBind> getSetting(int slot) {
        return switch (slot) {
            case 0 -> this.slot1;
            case 1 -> this.slot2;
            case 2 -> this.slot3;
            case 3 -> this.slot4;
            case 4 -> this.slot5;
            case 5 -> this.slot6;
            case 6 -> this.slot7;
            case 7 -> this.slot8;
            case 8 -> this.slot9;
            default -> throw new IllegalStateException("Unexpected value: " + slot);
        };
    }
}
