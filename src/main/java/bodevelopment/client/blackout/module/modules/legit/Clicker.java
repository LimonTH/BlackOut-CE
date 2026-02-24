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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

public class Clicker extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<RandomMode> randomise = this.sgGeneral.enumSetting("Randomise", RandomMode.Random, "Randomises CPS.");
    private final Setting<Double> minCps = this.sgGeneral.doubleSetting("Min CPS", 10.0, 0.0, 20.0, 0.1, ".", () -> this.randomise.get() != RandomMode.Disabled);
    private final Setting<Double> cps = this.sgGeneral.doubleSetting("CPS", 14.0, 0.0, 20.0, 0.1, ".");
    private long prev = 0L;

    public Clicker() {
        super("Clicker", "Automatically clicks", SubCategory.LEGIT, true);
    }

    @Event
    public void onRender(TickEvent.Pre event) {
        if (BlackOut.mc.player != null) {
            if (BlackOut.mc.options.attackKey.isPressed()) {
                if (this.delayCheck()) {
                    this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    this.clientSwing(SwingHand.MainHand, Hand.MAIN_HAND);
                    HitResult result = BlackOut.mc.crosshairTarget;
                    if (result == null || result.getType() != HitResult.Type.ENTITY) {
                        return;
                    }

                    Entity entity = ((EntityHitResult) result).getEntity();
                    if (entity instanceof LivingEntity livingEntity && livingEntity.isDead()) {
                        return;
                    }

                    BlackOut.mc.interactionManager.attackEntity(BlackOut.mc.player, entity);
                    this.prev = System.currentTimeMillis();
                }
            }
        }
    }

    private boolean delayCheck() {
        double d = this.randomise.get() == RandomMode.Disabled
                ? this.cps.get()
                : MathHelper.lerp(this.randomise.get().get(), this.cps.get(), this.minCps.get());
        return System.currentTimeMillis() - this.prev > 1000.0 / d;
    }
}
