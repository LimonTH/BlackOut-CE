package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class TriggerBot extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.entityFilterdListSetting("Target Entities", "The types of entities that will trigger an automatic attack.", type -> type != EntityType.ITEM && type != EntityType.EXPERIENCE_ORB && type != EntityType.AREA_EFFECT_CLOUD && type != EntityType.MARKER && type != EntityType.POTION && type != EntityType.LLAMA_SPIT && type != EntityType.EYE_OF_ENDER && type != EntityType.DRAGON_FIREBALL && type != EntityType.FIREWORK_ROCKET && type != EntityType.ENDER_PEARL && type != EntityType.FISHING_BOBBER && type != EntityType.ARROW && type != EntityType.SPECTRAL_ARROW && type != EntityType.SNOWBALL && type != EntityType.SMALL_FIREBALL && type != EntityType.WITHER_SKULL && type != EntityType.FALLING_BLOCK && type != EntityType.TNT && type != EntityType.EVOKER_FANGS && type != EntityType.LIGHTNING_BOLT && type != EntityType.WIND_CHARGE && type != EntityType.BREEZE_WIND_CHARGE && !type.toString().contains("display"), EntityType.PLAYER);
    private final Setting<Boolean> smartDelay = this.sgGeneral.booleanSetting("Attack Speed Synchronization", true, "Matches the attack timing with the weapon's cooldown for maximum damage efficiency.");
    private final Setting<Integer> minDelay = this.sgGeneral.intSetting("Minimum Tick Delay", 2, 0, 20, 1, "The minimum number of ticks to wait between attacks when using smart delay.", this.smartDelay::get);
    private final Setting<Integer> attackDelay = this.sgGeneral.intSetting("Static Attack Delay", 2, 0, 20, 1, "The fixed interval (in ticks) between attacks when smart delay is disabled.");
    private final Setting<Boolean> onlyWeapon = this.sgGeneral.booleanSetting("Weapon Restricted", true, "Prevents the module from triggering unless a tool or weapon is held in the main hand.");
    private final Setting<Boolean> critSync = this.sgGeneral.booleanSetting("Critical Hit Sync", false, "Synchronizes attacks with the player's falling state to guarantee critical damage.");
    private final Setting<Double> critSyncTime = this.sgGeneral.doubleSetting("Crit Window Duration", 0.3, 0.0, 1.0, 0.01, "The time window allowed for a critical hit before timing out.", this.critSync::get);

    private long critTime = 0L;

    public TriggerBot() {
        super("Trigger Bot", "Automatically initiates an attack when the crosshair is positioned over a valid target.", SubCategory.LEGIT, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (!this.shouldWait()) {
                this.critTime = System.currentTimeMillis();
                HitResult result = BlackOut.mc.hitResult;
                if (result != null && result.getType() == HitResult.Type.ENTITY) {
                    Entity entity = ((EntityHitResult) result).getEntity();
                    if (entity != null && this.entityTypes.get().contains(entity.getType())) {
                        if (!(entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying())) {
                            if (!this.onlyWeapon.get() || BlackOut.mc.player.getMainHandItem().has(DataComponents.TOOL)) {
                                int tickDelay = this.getTickDelay(entity);
                                if (BlackOut.mc.player.attackStrengthTicker >= tickDelay) {
                                    this.critTime = System.currentTimeMillis();
                                    BlackOut.mc.gameMode.attack(BlackOut.mc.player, entity);
                                    this.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                                    this.clientSwing(SwingHand.MainHand, InteractionHand.MAIN_HAND);
                                    if (entity instanceof EndCrystal && CrystalOptimizer.getInstance().enabled) {
                                        BlackOut.mc.level.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
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
        } else if (BlackOut.mc.player.onGround() && !BlackOut.mc.options.keyJump.isDown()) {
            return false;
        } else {
            return !(BlackOut.mc.player.fallDistance > 0.0F) && System.currentTimeMillis() - this.critTime <= this.critSyncTime.get() * 1000.0;
        }
    }

    private int getTickDelay(Entity entity) {
        return this.smartDelay.get() && entity instanceof LivingEntity
                ? Math.max((int) Math.ceil(1.0 / BlackOut.mc.player.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0), this.minDelay.get())
                : this.attackDelay.get();
    }
}
