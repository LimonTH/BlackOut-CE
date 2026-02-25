package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.MovementUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.tag.FluidTags;

public class Jesus extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Bypass Mode", Mode.NCP, "The specific anti-cheat bypass logic to use for walking on fluids.");
    private final Setting<Boolean> toggle = this.sgGeneral.booleanSetting("Anti-Rubberband", true, "Automatically reduces speed upon detecting a server-side position setback to maintain stability.", () -> this.mode.get() == Mode.NCP_Fast);
    private final Setting<Double> bob = this.sgGeneral.doubleSetting("Buoyancy Force", 0.005, 0.0, 1.0, 0.005, "The upward vertical velocity applied to keep the player above the fluid surface.", () -> this.mode.get() == Mode.NCP_Fast);
    private final Setting<Double> waterSpeed = this.sgGeneral.doubleSetting("Surface Velocity", 1.175, 0.0, 2.0, 0.005, "The horizontal movement speed while traversing the surface of the fluid.", () -> this.mode.get() == Mode.NCP_Fast);

    private boolean inWater = false;
    private boolean isSlowed = false;

    public Jesus() {
        super("Jesus", "Allows the player to walk on the surface of water and lava as if they were solid blocks.", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onEnable() {
        this.inWater = false;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onRecieve(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            this.isSlowed = true;
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.world.getBlockState(BlackOut.mc.player.getBlockPos().down()).getBlock() != Blocks.WATER
                    && BlackOut.mc.world.getBlockState(BlackOut.mc.player.getBlockPos()).getBlock() != Blocks.WATER) {
                this.inWater = false;
            } else {
                if (!this.inWater) {
                    this.isSlowed = false;
                }

                this.inWater = true;
            }

            if (BlackOut.mc.options.sneakKey.isPressed() || BlackOut.mc.options.jumpKey.isPressed()) {
                return;
            }

            switch (this.mode.get()) {
                case NCP:
                    this.tickNCP(event);
                    break;
                case NCP_Fast:
                    this.tickFast(event);
                    break;
                case Matrix:
                    this.tickMatrix(event);
            }
        }
    }

    private void tickNCP(MoveEvent.Pre event) {
        double height = OLEPOSSUtils.fluidHeight(BlackOut.mc.player.getBoundingBox(), FluidTags.WATER);
        if (!(height <= 0.0)) {
            if (BlackOut.mc.player.horizontalCollision) {
                event.setY(this, 0.1);
            } else {
                event.setY(this, height < 0.05 ? -1.0E-4 : Math.min(height, 0.1));
            }

            double yaw = Math.toRadians(Managers.ROTATION.moveYaw + 90.0F);
            double speed = MovementUtils.getSpeed(0.2873);
            if (Managers.ROTATION.move) {
                event.setXZ(this, Math.cos(yaw) * speed, Math.sin(yaw) * speed);
            }
        }
    }

    private void tickFast(MoveEvent.Pre event) {
        if (BlackOut.mc.player.isTouchingWater() && !BlackOut.mc.player.isSubmergedInWater()
                || BlackOut.mc.player.isInLava() && !BlackOut.mc.player.isSubmergedIn(FluidTags.LAVA)) {
            ((IVec3d) BlackOut.mc.player.getVelocity()).blackout_Client$setY(this.bob.get());
            if (this.toggle.get() && (!BlackOut.mc.player.isInLava() || BlackOut.mc.player.isSubmergedIn(FluidTags.LAVA)) && !this.isSlowed) {
                double motion = MovementUtils.getSpeed(this.waterSpeed.get());
                if (BlackOut.mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                    motion *= 1.2 + BlackOut.mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() * 0.2;
                }

                if (BlackOut.mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                    motion /= 1.2 + BlackOut.mc.player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() * 0.2;
                }

                double x = Math.cos(Math.toRadians(Managers.ROTATION.moveYaw + 90.0F));
                double z = Math.sin(Math.toRadians(Managers.ROTATION.moveYaw + 90.0F));
                if (Managers.ROTATION.move) {
                    event.setXZ(this, motion * x, motion * z);
                } else {
                    event.setXZ(this, 0.0, 0.0);
                }
            }
        }
    }

    private void tickMatrix(MoveEvent.Pre event) {
        double height = OLEPOSSUtils.fluidHeight(BlackOut.mc.player.getBoundingBox(), FluidTags.WATER);
        if (height > 0.0 && height <= 1.0) {
            event.setY(this, 0.13);
        }
    }

    public enum Mode {
        NCP,
        NCP_Fast,
        Matrix
    }
}
