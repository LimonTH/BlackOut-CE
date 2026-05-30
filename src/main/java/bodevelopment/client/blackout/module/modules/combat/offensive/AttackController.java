package bodevelopment.client.blackout.module.modules.combat.offensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwingState;
import bodevelopment.client.blackout.enums.SwingType;
import bodevelopment.client.blackout.helpers.RotationHelper;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.EntityUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Extracted attack logic from {@link bodevelopment.client.blackout.module.ObsidianModule}.
 * Handles crystal targeting, damage calculation, and attack packet dispatch.
 */
public class AttackController {
    private final Setting<Boolean> attackSetting;
    private final Setting<Double> attackSpeed;
    private final Setting<Boolean> alwaysAttack;
    private final Setting<Boolean> attackSwing;
    private final Setting<SwingHand> attackHand;
    private final RotationHelper rotation;
    private final List<BlockPos> blockPlacements;
    private final List<BlockPos> valids;

    private long lastAttack;

    public AttackController(Setting<Boolean> attackSetting, Setting<Double> attackSpeed,
                            Setting<Boolean> alwaysAttack, Setting<Boolean> attackSwing,
                            Setting<SwingHand> attackHand, RotationHelper rotation,
                            List<BlockPos> blockPlacements, List<BlockPos> valids) {
        this.attackSetting = attackSetting;
        this.attackSpeed = attackSpeed;
        this.alwaysAttack = alwaysAttack;
        this.attackSwing = attackSwing;
        this.attackHand = attackHand;
        this.rotation = rotation;
        this.blockPlacements = blockPlacements;
        this.valids = valids;
    }

    public void updateAttack() {
        if (!attackSetting.get()) return;
        if (System.currentTimeMillis() - lastAttack < 1000.0 / attackSpeed.get()) return;

        Entity blocking = getBlocking();
        if (blocking == null) return;

        if (SettingUtils.shouldRotate(RotationType.Attacking)
                && !rotation.attackRotate(blocking.getBoundingBox(), -0.1, "attacking")) {
            return;
        }

        SettingUtils.swing(SwingState.Pre, SwingType.Attacking, InteractionHand.MAIN_HAND);
        BlackOut.mc.getConnection().send(
                ServerboundInteractPacket.createAttackPacket(blocking, BlackOut.mc.player.isShiftKeyDown()));
        SettingUtils.swing(SwingState.Post, SwingType.Attacking, InteractionHand.MAIN_HAND);

        if (SettingUtils.shouldRotate(RotationType.Attacking)) {
            rotation.end("attacking");
        }

        // Note: clientSwing() is called by the owning Module after updateAttack() returns,
        // since it requires Module-level access (not available in this extracted class).

        lastAttack = System.currentTimeMillis();
    }

    private Entity getBlocking() {
        Entity crystal = null;
        double lowest = Double.MAX_VALUE;
        AABB searchBox = BlackOut.mc.player.getBoundingBox().inflate(6.0);

        for (Entity entity : BlackOut.mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal)) continue;
            if (!entity.getBoundingBox().intersects(searchBox)) continue;
            if (!SettingUtils.inAttackRange(entity.getBoundingBox())) continue;
            if (!validForBlocking(entity)) continue;

            double dmg = DamageUtils.crystalDamage(BlackOut.mc.player,
                    BlackOut.mc.player.getBoundingBox(), entity.position());
            if (dmg < lowest) {
                lowest = dmg;
                crystal = entity;
            }
        }

        return crystal;
    }

    public boolean validForBlocking(Entity entity) {
        List<BlockPos> targets = alwaysAttack.get() ? blockPlacements : valids;
        for (int i = 0; i < targets.size(); i++) {
            if (BoxUtils.get(targets.get(i)).intersects(entity.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }

    public static boolean validEntity(Entity entity, long lastAttackTime) {
        if (entity instanceof EndCrystal && System.currentTimeMillis() - lastAttackTime < 100L) return false;
        return !(entity instanceof ItemEntity);
    }
}
