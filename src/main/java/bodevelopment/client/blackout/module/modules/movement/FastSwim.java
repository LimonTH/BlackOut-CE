package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.client.input.Input;
import net.minecraft.registry.tag.FluidTags;

public class FastSwim extends Module {
    private final SettingGroup sgSpeed = this.addGroup("Speed");
    private final SettingGroup sgVertical = this.addGroup("Vertical");
    private final Setting<Double> waterTouching = this.sgSpeed.doubleSetting("Water Touching", 0.5, 0.0, 2.0, 0.02, ".");
    private final Setting<Double> waterSubmerged = this.sgSpeed.doubleSetting("Water Submerged", 0.5, 0.0, 2.0, 0.02, ".");
    private final Setting<Double> waterDiving = this.sgSpeed.doubleSetting("Water Diving", 0.5, 0.0, 2.0, 0.02, ".");
    private final Setting<Double> lavaTouching = this.sgSpeed.doubleSetting("Lava Touching", 0.5, 0.0, 2.0, 0.02, ".");
    private final Setting<Double> lavaSubmerged = this.sgSpeed.doubleSetting("Lava Submerged", 0.5, 0.0, 2.0, 0.02, ".");
    private final Setting<Boolean> stillVertical = this.sgVertical.booleanSetting("Still Vertical", true, ".");
    private final Setting<Boolean> modifyVertical = this.sgVertical.booleanSetting("Modify Vertical", false, ".");
    private final Setting<Double> waterUp = this.sgVertical.doubleSetting("Water Up", 0.5, 0.0, 2.0, 0.02, ".", this.modifyVertical::get);
    private final Setting<Double> waterDown = this.sgVertical.doubleSetting("Water Down", 0.5, 0.0, 2.0, 0.02, ".", this.modifyVertical::get);
    private final Setting<Double> lavaUp = this.sgVertical.doubleSetting("Lava Up", 0.5, 0.0, 2.0, 0.02, ".", this.modifyVertical::get);
    private final Setting<Double> lavaDown = this.sgVertical.doubleSetting("Lava Down", 0.5, 0.0, 2.0, 0.02, ".", this.modifyVertical::get);

    public FastSwim() {
        super("Fast Swim", "Swims faster guh", SubCategory.MOVEMENT, true);
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        boolean touchingWater = BlackOut.mc.player.isTouchingWater();
        boolean diving = BlackOut.mc.player.isInSwimmingPose() && touchingWater;
        double targetSpeed;
        if (diving) {
            targetSpeed = this.waterDiving.get();
        } else {
            targetSpeed = this.getSpeed(touchingWater);
        }

        if (!(targetSpeed <= 0.0)) {
            if (!diving && this.modifyVertical.get() && BlackOut.mc.player.input.jumping ^ BlackOut.mc.player.input.sneaking) {
                event.setY(this, this.getVertical(touchingWater && !BlackOut.mc.player.isInLava()));
            } else if (this.canBeStill(diving)) {
                event.setY(this, 0.0);
            }

            if (Managers.ROTATION.move) {
                double yaw = Math.toRadians(Managers.ROTATION.moveYaw + 90.0F);
                double cos = Math.cos(yaw) * targetSpeed;
                double sin = Math.sin(yaw) * targetSpeed;
                if (!diving) {
                    event.setXZ(this, cos, sin);
                } else {
                    double hz = this.horizontalMulti(BlackOut.mc.player.input);
                    double v = this.verticalMulti(BlackOut.mc.player.input);
                    event.set(this, hz * cos, v * targetSpeed, hz * sin);
                }
            }
        }
    }

    private double getVertical(boolean water) {
        return BlackOut.mc.player.input.jumping ? (water ? this.waterUp : this.lavaUp).get() : -(water ? this.waterDown : this.lavaDown).get();
    }

    private double horizontalMulti(Input i) {
        if (i.getMovementInput().lengthSquared() == 0.0F) {
            return 0.0;
        } else {
            return i.jumping ^ i.sneaking ? 0.707106781 : 1.0;
        }
    }

    private double verticalMulti(Input i) {
        if (i.jumping == i.sneaking) {
            return 0.0;
        } else {
            double sus = i.getMovementInput().lengthSquared() == 0.0F ? 1.0 : 0.707106781;
            return i.jumping ? sus : -sus;
        }
    }

    private boolean canBeStill(boolean diving) {
        if (!this.stillVertical.get()) {
            return false;
        } else {
            Input i = BlackOut.mc.player.input;
            return diving ? !i.jumping && !i.sneaking && i.getMovementInput().lengthSquared() == 0.0F : !i.jumping && !i.sneaking;
        }
    }

    private double getSpeed(boolean touchingWater) {
        boolean submergedWater = BlackOut.mc.player.isSubmergedInWater();
        boolean submergedLava = BlackOut.mc.player.isSubmergedIn(FluidTags.LAVA);
        boolean touchingLava = BlackOut.mc.player.isInLava();
        if (submergedWater && submergedLava) {
            return Math.min(this.waterSubmerged.get(), this.lavaSubmerged.get());
        } else if (submergedWater) {
            return this.waterSubmerged.get();
        } else if (submergedLava) {
            return this.lavaSubmerged.get();
        } else if (touchingWater && touchingLava) {
            return Math.min(this.waterTouching.get(), this.lavaTouching.get());
        } else if (touchingWater) {
            return this.waterTouching.get();
        } else {
            return touchingLava ? this.lavaTouching.get() : -1.0;
        }
    }
}
