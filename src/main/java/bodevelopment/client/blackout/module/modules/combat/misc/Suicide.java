package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoCrystal;
import bodevelopment.client.blackout.module.modules.combat.offensive.BedAura;
import bodevelopment.client.blackout.module.modules.combat.offensive.CreeperAura;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ItemUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Suicide extends Module {
    private static Suicide INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> disableDeath = this.sgGeneral.booleanSetting("Disable On Death", true, "Automatically terminates the module upon successful respawn.");
    public final Setting<Boolean> useCA = this.sgGeneral.booleanSetting("Use Auto Crystal", true, "Forces AutoCrystal to prioritize self-destruction.");
    public final Setting<Boolean> enableCA = this.sgGeneral.booleanSetting("Enable Auto Crystal", true, "Instantly toggles AutoCrystal on activation.", this.useCA::get);
    public final Setting<Boolean> useBA = this.sgGeneral.booleanSetting("Use Bed Aura", false, "Forces BedAura to target your own hitbox.");
    public final Setting<Boolean> enableBA = this.sgGeneral.booleanSetting("Enable Bed Aura", true, "Instantly toggles BedAura on activation.", this.useBA::get);
    public final Setting<Boolean> useCreeper = this.sgGeneral.booleanSetting("Use Creeper Aura", false, "Utilizes CreeperAura for explosive termination.");
    public final Setting<Boolean> enableCreeper = this.sgGeneral.booleanSetting("Enable Creeper Aura", true, "Instantly toggles CreeperAura on activation.", this.useCreeper::get);
    public final Setting<Boolean> offHand = this.sgGeneral.booleanSetting("Force Offhand", false, "Inhibits Offhand module to prevent totem-saves during execution.");
    public final Setting<Integer> dropArmor = this.sgGeneral.intSetting("Jettison Armor", 0, 0, 4, 1, "Number of armor pieces to drop to maximize incoming damage.");

    public Suicide() {
        super("Suicide", "Automates tactical self-termination to deny kill-streaks or quick-reset.", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static Suicide getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        if (this.useCA.get() && this.enableCA.get()) {
            AutoCrystal.getInstance().enable();
        }

        if (this.useBA.get() && this.enableBA.get()) {
            BedAura.getInstance().enable();
        }

        if (this.useCreeper.get() && this.enableCreeper.get()) {
            CreeperAura.getInstance().enable();
        }

        this.dropArmor();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.currentScreen instanceof DeathScreen && this.disableDeath.get()) {
            this.disable("died");
        }
    }

    private void dropArmor() {
        if (this.dropArmor.get() >= 1) {
            boolean dropped = false;

            for (EquipmentSlot equipmentSlot : this.toDrop(this.dropArmor.get())) {
                BlackOut.mc.interactionManager.clickSlot(0, 8 - equipmentSlot.getEntitySlotId(), 0, SlotActionType.THROW, BlackOut.mc.player);
                dropped = true;
            }

            if (dropped && BlackOut.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
                BlackOut.mc.player.closeHandledScreen();
            }
        }
    }

    private List<EquipmentSlot> toDrop(int amount) {
        List<EquipmentSlot> list = Arrays.stream(OLEPOSSUtils.equipmentSlots)
                .filter(slot -> BlackOut.mc.player.getInventory().getArmorStack(slot.getEntitySlotId()).getItem() instanceof ArmorItem)
                .sorted(Comparator.comparingDouble(slot -> ItemUtils.getArmorValue(BlackOut.mc.player.getInventory().getArmorStack(slot.getEntitySlotId()))))
                .collect(Collectors.toList());
        return list.subList(Math.max(0, list.size() - amount), list.size());
    }
}
