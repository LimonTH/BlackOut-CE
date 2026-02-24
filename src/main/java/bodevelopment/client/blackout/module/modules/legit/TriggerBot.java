package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ToolItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.List;

public class TriggerBot extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.entityListSetting("Entities", ".", EntityType.PLAYER);
    private final Setting<Boolean> smartDelay = this.sgGeneral.booleanSetting("Smart Delay", true, "Charges weapon when hitting living entities.");
    private final Setting<Integer> minDelay = this.sgGeneral.intSetting("Min Delay", 2, 0, 20, 1, "Ticks between attacks.", this.smartDelay::get);
    private final Setting<Integer> attackDelay = this.sgGeneral.intSetting("Attack Delay", 2, 0, 20, 1, "Ticks between attacks.");
    private final Setting<Boolean> onlyWeapon = this.sgGeneral.booleanSetting("Only Weapon", true, "Only attacks with weapons");
    private final Setting<Boolean> critSync = this.sgGeneral.booleanSetting("Crit Sync", false, ".");
    private final Setting<Double> critSyncTime = this.sgGeneral.doubleSetting("Crit Sync Time", 0.3, 0.0, 1.0, 0.01, ".", this.critSync::get);
    private long critTime = 0L;

    public TriggerBot() {
        super("Trigger Bot", "Automatically attacks entities when you look at them.", SubCategory.LEGIT, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (!this.shouldWait()) {
                this.critTime = System.currentTimeMillis();
                HitResult result = BlackOut.mc.crosshairTarget;
                if (result != null && result.getType() == HitResult.Type.ENTITY) {
                    Entity entity = ((EntityHitResult) result).getEntity();
                    if (entity != null && this.entityTypes.get().contains(entity.getType())) {
                        if (!(entity instanceof LivingEntity livingEntity && livingEntity.isDead())) {
                            if (!this.onlyWeapon.get() || BlackOut.mc.player.getMainHandStack().getItem() instanceof ToolItem) {
                                int tickDelay = this.getTickDelay(entity);
                                if (BlackOut.mc.player.lastAttackedTicks >= tickDelay) {
                                    this.critTime = System.currentTimeMillis();
                                    BlackOut.mc.interactionManager.attackEntity(BlackOut.mc.player, entity);
                                    this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                                    this.clientSwing(SwingHand.MainHand, Hand.MAIN_HAND);
                                    if (entity instanceof EndCrystalEntity && CrystalOptimizer.getInstance().enabled) {
                                        BlackOut.mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean shouldWait() {
        if (!this.critSync.get()) {
            return false;
        } else if (BlackOut.mc.player.isOnGround() && !BlackOut.mc.options.jumpKey.isPressed()) {
            return false;
        } else {
            return !(BlackOut.mc.player.fallDistance > 0.0F) && System.currentTimeMillis() - this.critTime <= this.critSyncTime.get() * 1000.0;
        }
    }

    private int getTickDelay(Entity entity) {
        return this.smartDelay.get() && entity instanceof LivingEntity
                ? Math.max((int) Math.ceil(1.0 / BlackOut.mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0), this.minDelay.get())
                : this.attackDelay.get();
    }
}
