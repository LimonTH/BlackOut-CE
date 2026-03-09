package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class ElytraBoost extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> antiConsume = this.sgGeneral.booleanSetting("Anti-Consume", true, "Prevents using actual fireworks from your inventory.");
    private final Setting<Integer> fireworkLevel = this.sgGeneral.intSetting("Boost Power", 1, 1, 3, 1, "The flight duration of the spawned firework.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "How to swap to fireworks if you aren't holding them.");
    private final Setting<Boolean> playSound = this.sgGeneral.booleanSetting("Play Sound", true, "Plays launch sound.");

    private final List<FireworkRocketEntity> spawnedFireworks = new ArrayList<>();
    private long lastBoostTime = 0L;

    public ElytraBoost() {
        super("Elytra Boost", "Simple boost for elytra flight.", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onDisable() {
        spawnedFireworks.clear();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        spawnedFireworks.removeIf(Entity::isRemoved);

        if (BlackOut.mc.player.isGliding() && BlackOut.mc.options.useKey.isPressed()) {

            Hand hand = OLEPOSSUtils.getHand(stack -> stack.getItem() instanceof FireworkRocketItem);
            FindResult result = this.switchMode.get().find(stack -> stack.getItem() instanceof FireworkRocketItem);

            if (hand != null || result.wasFound()) {
                if (antiConsume.get()) {
                    if (System.currentTimeMillis() - lastBoostTime > 500) {
                        doFakeBoost();
                        lastBoostTime = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void doFakeBoost() {
        ItemStack stack = Items.FIREWORK_ROCKET.getDefaultStack();
        stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(fireworkLevel.get(), new ArrayList<>()));

        FireworkRocketEntity rocket = new FireworkRocketEntity(BlackOut.mc.world, stack, BlackOut.mc.player);
        spawnedFireworks.add(rocket);

        if (playSound.get()) {
            BlackOut.mc.world.playSoundFromEntity(BlackOut.mc.player, rocket,
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 3.0F, 1.0F);
        }

        BlackOut.mc.world.addEntity(rocket);
    }
}