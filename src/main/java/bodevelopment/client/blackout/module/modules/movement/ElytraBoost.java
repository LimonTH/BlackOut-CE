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

        // Очистка старых сущностей из памяти
        spawnedFireworks.removeIf(Entity::isRemoved);

        // Условие: мы летим на элитрах и зажали ПКМ (или используем бинд модуля)
        if (BlackOut.mc.player.isFallFlying() && BlackOut.mc.options.useKey.isPressed()) {

            // Проверяем, есть ли у нас фейерверк в руках или инвентаре
            Hand hand = OLEPOSSUtils.getHand(stack -> stack.getItem() instanceof FireworkRocketItem);
            FindResult result = this.switchMode.get().find(stack -> stack.getItem() instanceof FireworkRocketItem);

            if (hand != null || result.wasFound()) {
                // Если включен Anti-Consume, мы перехватываем действие
                if (antiConsume.get()) {
                    // Ограничиваем скорость спавна, чтобы не лагало (раз в 500мс)
                    if (System.currentTimeMillis() - lastBoostTime > 500) {
                        doFakeBoost();
                        lastBoostTime = System.currentTimeMillis();
                    }

                    // ВАЖНО: Мы не даем ванильному майнкрафту использовать предмет
                    // В BlackOut это часто делается через отмену пакета или перехват в Mixin.
                }
            }
        }
    }

    private void doFakeBoost() {
        // Создаем предмет-компонент для ракеты
        ItemStack stack = Items.FIREWORK_ROCKET.getDefaultStack();
        stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(fireworkLevel.get(), new ArrayList<>()));

        // Спавним саму ракету
        FireworkRocketEntity rocket = new FireworkRocketEntity(BlackOut.mc.world, stack, BlackOut.mc.player);
        spawnedFireworks.add(rocket);

        if (playSound.get()) {
            BlackOut.mc.world.playSoundFromEntity(BlackOut.mc.player, rocket,
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 3.0F, 1.0F);
        }

        // Добавляем в мир (только визуально для нас, если это клиентская часть)
        BlackOut.mc.world.addEntity(rocket);
    }
}