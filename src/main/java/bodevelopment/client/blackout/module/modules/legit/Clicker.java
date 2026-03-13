package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RandomMode;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Clicker extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<RandomMode> randomise = this.sgGeneral.enumSetting("Randomization Mode", RandomMode.Random, "The algorithm used to vary the clicks per second.");
    private final Setting<Double> minCps = this.sgGeneral.doubleSetting("Minimum CPS", 10.0, 0.0, 20.0, 0.1, "The lower bound for randomized clicks per second.", () -> this.randomise.get() != RandomMode.Disabled);
    private final Setting<Double> cps = this.sgGeneral.doubleSetting("Target CPS", 14.0, 0.0, 20.0, 0.1, "The base or maximum clicks per second to maintain.");

    private long prev = 0L;

    public Clicker() {
        super("Clicker", "Simulates natural mouse clicks at a specified frequency while holding the attack key.", SubCategory.LEGIT, true);
    }

    @Event
    public void onRender(TickEvent.Pre event) {
        if (BlackOut.mc.player != null) {
            if (BlackOut.mc.options.keyAttack.isDown()) {
                if (this.delayCheck()) {
                    this.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    this.clientSwing(SwingHand.MainHand, InteractionHand.MAIN_HAND);
                    HitResult result = BlackOut.mc.hitResult;
                    if (result == null || result.getType() != HitResult.Type.ENTITY) {
                        return;
                    }

                    Entity entity = ((EntityHitResult) result).getEntity();
                    if (entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) {
                        return;
                    }

                    BlackOut.mc.gameMode.attack(BlackOut.mc.player, entity);
                    this.prev = System.currentTimeMillis();
                }
            }
        }
    }

    private boolean delayCheck() {
        double d = this.randomise.get() == RandomMode.Disabled
                ? this.cps.get()
                : Mth.lerp(this.randomise.get().get(), this.cps.get(), this.minCps.get());
        return System.currentTimeMillis() - this.prev > 1000.0 / d;
    }
}
