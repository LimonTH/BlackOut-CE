package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmartMend extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Integer> antiWaste = this.sgGeneral
            .intSetting("Anti Waste", 90, 0, 100, 1, "Doesn't use experience if any armor piece is above this durability.");
    private final Setting<Double> moveSpeed = this.sgGeneral.doubleSetting("Move Speed", 2.0, 0.0, 20.0, 0.2, ".");
    private final Setting<Boolean> closeInv = this.sgGeneral.booleanSetting("Close Inventory", true, ".");
    private final List<EquipmentSlot> moveBack = new ArrayList<>();
    private final TimerList<EquipmentSlot> delays = new TimerList<>(true);
    private final Map<EquipmentSlot, Long> wornSince = new HashMap<>();
    private long prevMove = 0L;

    public SmartMend() {
        super("Smart Mend", "Moves fully mended items to inventory.", SubCategory.DEFENSIVE, true);
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            for (EquipmentSlot equipmentSlot : OLEPOSSUtils.equipmentSlots) {
                if (BlackOut.mc.player.getInventory().getArmorStack(equipmentSlot.getEntitySlotId()).isEmpty()) {
                    this.wornSince.remove(equipmentSlot);
                } else if (!this.wornSince.containsKey(equipmentSlot)) {
                    this.wornSince.put(equipmentSlot, System.currentTimeMillis());
                }
            }

            this.updateMoving(this.isMending());
        }
    }

    private void updateMoving(boolean mending) {
        if (mending) {
            this.onMend();
        } else {
            this.onMendStop();
        }
    }

    private void onMend() {
        List<EquipmentSlot> mended = this.getMended();
        if (InvUtils.find(true, true, ItemStack::isEmpty).wasFound()) {
            if (!mended.isEmpty()) {
                EquipmentSlot equipmentSlot = mended.get(0);
                if (!(System.currentTimeMillis() - this.prevMove < 1000.0 / this.moveSpeed.get())) {
                    if (!this.delays.contains(equipmentSlot)) {
                        int slot = 8 - equipmentSlot.getEntitySlotId();
                        if (!this.moveBack.contains(equipmentSlot)) {
                            this.moveBack.add(equipmentSlot);
                        }

                        this.move(equipmentSlot, slot);
                        mended.remove(0);
                        this.prevMove = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void onMendStop() {
        this.moveBack
                .removeIf(equipmentSlotx -> this.wornSince.containsKey(equipmentSlotx) && System.currentTimeMillis() - this.wornSince.get(equipmentSlotx) > 500L);
        if (!this.moveBack.isEmpty()) {
            EquipmentSlot equipmentSlot = this.moveBack.get(0);
            if (!this.wornSince.containsKey(equipmentSlot)) {
                if (!(System.currentTimeMillis() - this.prevMove < 1000.0 / this.moveSpeed.get())) {
                    if (!this.delays.contains(equipmentSlot)) {
                        int slot = this.getArmorSlot(equipmentSlot);
                        if (slot >= 0) {
                            this.move(equipmentSlot, slot);
                            this.prevMove = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
    }

    private void move(EquipmentSlot equipmentSlot, int slot) {
        this.delays.add(equipmentSlot, 0.5);
        InvUtils.interactHandler(slot, 0, SlotActionType.QUICK_MOVE);
        if (this.closeInv.get()) {
            this.closeInventory();
        }
    }

    private int getArmorSlot(EquipmentSlot equipmentSlot) {
        for (Slot slot : BlackOut.mc.player.currentScreenHandler.slots) {
            if (slot.getStack().getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() == equipmentSlot) {
                return slot.getIndex();
            }
        }

        return -1;
    }

    private List<EquipmentSlot> getMended() {
        List<EquipmentSlot> armor = new ArrayList<>();

        for (EquipmentSlot equipmentSlot : OLEPOSSUtils.equipmentSlots) {
            ItemStack stack = BlackOut.mc.player.getInventory().getArmorStack(equipmentSlot.getEntitySlotId());
            if (!stack.isEmpty() && stack.isDamageable()) {
                double dur = (double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0;
                if (dur >= this.antiWaste.get()) {
                    armor.add(equipmentSlot);
                }
            }
        }

        return armor;
    }

    private boolean isMending() {
        return ExpThrower.getInstance().enabled;
    }
}
