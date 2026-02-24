package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class KillEffects extends Module {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Integer> range = this.sgGeneral.intSetting("Range", 50, 0, 100, 1, ".");
    public final Setting<Integer> tickDelay = this.sgGeneral.intSetting("Tick Delay", 10, 0, 20, 1, ".");
    private int ticks = 0;

    public KillEffects() {
        super("Kill Effects", "Spawns lighting when someone dies", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.ticks++;
            if (this.ticks >= this.tickDelay.get()) {
                for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
                    if (player != BlackOut.mc.player
                            && player.getPos().distanceTo(BlackOut.mc.player.getPos()) <= this.range.get()
                            && (player.getHealth() <= 0.0F || player.isDead())) {
                        this.ticks = 0;
                        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, BlackOut.mc.world);
                        lightning.setPosition(player.getX(), player.getY(), player.getZ());
                        BlackOut.mc.world.addEntity(lightning);
                        BlackOut.mc
                                .world
                                .playSound(
                                        player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0F, 1.0F, true
                                );
                    }
                }
            }
        }
    }
}
