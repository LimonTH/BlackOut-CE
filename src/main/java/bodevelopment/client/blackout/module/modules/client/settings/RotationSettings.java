package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.functional.DoubleConsumer;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.Rotation;
import bodevelopment.client.blackout.util.*;
import it.unimi.dsi.fastutil.floats.FloatFloatPair;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RotationSettings extends SettingsModule {
    private static RotationSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgAttack = this.addGroup("Attack");
    private final SettingGroup sgMining = this.addGroup("Mining");
    private final SettingGroup sgBlockPlace = this.addGroup("Block Place");
    private final SettingGroup sgInteract = this.addGroup("Interact");

    public final Setting<Integer> renderSmoothness = this.sgGeneral.intSetting("Render Smoothness", 1, 0, 10, 1,
            "Smooths out the visual rotation of your head for a more natural look in third-person.");
    public final Setting<Boolean> vanillaRotation = this.sgGeneral.booleanSetting("Client Rotation", false,
            "Synchronizes your client-side camera with the internal rotation logic. Can be disorienting.");
    public final Setting<RotationSpeedMode> rotationSpeedMode = this.sgGeneral.enumSetting("Rotation Speed Mode", RotationSpeedMode.Normal,
            "The mathematical algorithm used to determine how fast the head moves between targets.");
    public final Setting<Boolean> timeBasedSpeed = this.sgGeneral.booleanSetting("Time Based Speed", true,
            "Adjusts rotation speed based on real time elapsed since the last tick to ensure consistency regardless of TPS.",
            () -> this.rotationSpeedMode.get() != RotationSpeedMode.Balance);
    public final Setting<Double> maxMulti = this.sgGeneral.doubleSetting("Max Multi", 1.0, 0.0, 5.0, 0.05,
            "The maximum allowed speed multiplier for time-based corrections to prevent excessive snapping.",
            () -> this.rotationSpeedMode.get() != RotationSpeedMode.Balance && this.timeBasedSpeed.get());
    public final Setting<Integer> averageTicks = this.sgGeneral.intSetting("Average Ticks", 10, 0, 20, 1,
            "The sample size of previous ticks used to calculate the mean rotation speed in Balance mode.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Balance);
    public final Setting<Double> averageSpeed = this.sgGeneral.doubleSetting("Average Speed", 20.0, 0.0, 100.0, 1.0,
            "The target average degrees per tick the module tries to maintain in Balance mode.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Balance);
    public final Setting<Double> maxSpeed = this.sgGeneral.doubleSetting("Max Speed", 60.0, 0.0, 100.0, 1.0,
            "The absolute limit for rotation speed in a single packet when using Balance mode.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Balance);
    public final Setting<Double> yawStep = this.sgGeneral.doubleSetting("Yaw Step", 90.0, 0.0, 180.0, 1.0,
            "Maximum horizontal degrees the head can rotate in a single packet.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Separate);
    public final Setting<Double> pitchStep = this.sgGeneral.doubleSetting("Pitch Step", 45.0, 0.0, 180.0, 1.0,
            "Maximum vertical degrees the head can rotate in a single packet.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Separate);
    public final Setting<Double> rotationSpeed = this.sgGeneral.doubleSetting("Rotation Speed", 90.0, 0.0, 360.0, 1.0,
            "The base degrees per packet speed used in the Normal rotation mode.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Normal);
    public final Setting<Double> minSmoothSpeed = this.sgGeneral.doubleSetting("Min Smooth Speed", 10.0, 0.0, 100.0, 1.0,
            "The starting rotation speed in Smooth mode when the angle to the target is small.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Smooth);
    public final Setting<Double> maxSmoothSpeed = this.sgGeneral.doubleSetting("Max Smooth Speed", 75.0, 0.0, 100.0, 1.0,
            "The peak rotation speed in Smooth mode achieved when the angle to the target is large.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Smooth);
    public final Setting<Double> minSmoothAngle = this.sgGeneral.doubleSetting("Min Smooth Angle", 5.0, 0.0, 100.0, 1.0,
            "The angle threshold below which the rotation uses Min Smooth Speed.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Smooth);
    public final Setting<Double> maxSmoothAngle = this.sgGeneral.doubleSetting("Max Smooth Angle", 135.0, 0.0, 100.0, 1.0,
            "The angle threshold above which the rotation reaches its Max Smooth Speed.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Smooth);
    public final Setting<Double> yawRandom = this.sgGeneral.doubleSetting("Yaw Random", 1.0, 0.0, 10.0, 0.1,
            "Adds a degree of random horizontal variation to prevent perfectly static rotations.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Separate);
    public final Setting<Double> pitchRandom = this.sgGeneral.doubleSetting("Pitch Random", 1.0, 0.0, 10.0, 0.1,
            "Adds a degree of random vertical variation to prevent perfectly static rotations.",
            () -> this.rotationSpeedMode.get() == RotationSpeedMode.Separate);
    public final Setting<Boolean> pauseRotated = this.sgGeneral.booleanSetting("Pause Rotated", false,
            "Briefly stops rotation updates after a successful rotation has been completed.");
    public final Setting<Integer> delay = this.sgGeneral.intSetting("Delay", 3, 0, 10, 1,
            "How many ticks to wait before starting a new rotation if Pause Rotated is enabled.", this.pauseRotated::get);
    public final Setting<Double> returnSpeed = this.sgGeneral.doubleSetting("Return Speed", 20.0, 0.0, 180.0, 1.0,
            "How fast the head returns to your actual camera direction once a module stops rotating.");
    public final Setting<PacketRotationMode> packetRotationMode = this.sgGeneral.enumSetting("Packet Rotation Mode", PacketRotationMode.Disabled,
            "Enables sending additional rotation packets for higher precision or specific server bypasses.");
    public final Setting<Boolean> smartPacket = this.sgGeneral.booleanSetting("Smart Packet", true,
            "Only sends extra rotation packets if your current look direction is not already targeting the object.",
            () -> this.packetRotationMode.get() != PacketRotationMode.Disabled);
    public final Setting<Double> packetRotations = this.sgGeneral.doubleSetting("Packet Rotations", 1.0, 1.0, 10.0, 0.1,
            "How many extra rotation packets to send per tick to increase rotation frequency.",
            () -> this.packetRotationMode.get() != PacketRotationMode.Disabled);
    public final Setting<Boolean> instantPacketRotation = this.sgGeneral.booleanSetting("Instant Packet Rotation", true,
            "Ignores standard speed limits for the extra rotation packets, making them effectively instant.",
            () -> this.packetRotationMode.get() != PacketRotationMode.Disabled);
    private final Setting<Integer> jitterStrength = this.sgGeneral.intSetting("Jitter Strength", 1, 0, 10, 1,
            "Simulates a realistic 'hand shake' by adding micro-vibrations to your rotation.");

    public final Setting<Boolean> attackRotate = this.sgAttack.booleanSetting("Attack Rotate", false,
            "Enables automated rotations specifically when attacking entities.");
    public final Setting<RotationCheckMode> attackMode = this.sgAttack.enumSetting("Attack Mode", RotationCheckMode.Raytrace,
            "The method used to verify if the crosshair is correctly positioned on the target entity.");
    public final Setting<Double> attackYawAngle = this.sgAttack.doubleSetting("Attack Yaw Angle", 90.0, 0.0, 180.0, 1.0,
            "The horizontal FOV within which a rotation is considered valid for attacking.",
            () -> this.attackMode.get() == RotationCheckMode.Angle);
    public final Setting<Double> attackPitchAngle = this.sgAttack.doubleSetting("Attack Pitch Angle", 45.0, 0.0, 180.0, 1.0,
            "The vertical FOV within which a rotation is considered valid for attacking.",
            () -> this.attackMode.get() == RotationCheckMode.Angle);
    public final Setting<Boolean> attackLimit = this.sgAttack.booleanSetting("Attack Limit", false,
            "If enabled, enforces a strict maximum speed cap during combat rotations.");
    public final Setting<Double> attackMaxSpeed = this.sgAttack.doubleSetting("Max Attack Speed", 30.0, 0.0, 100.0, 1.0,
            "The absolute maximum degrees per packet allowed when Attack Limit is active.");
    public final Setting<Integer> attackTicks = this.sgAttack.intSetting("Attack Ticks", 10, 0, 50, 1,
            "How many ticks the module should 'stick' to the target after the initial attack.");
    public final Setting<Double> noOwnTime = this.sgAttack.doubleSetting("No Own Rotate", 0.0, 0.0, 5.0, 0.05,
            "The time (in seconds) to wait before this module can rotate again after another action.");
    public final Setting<Double> noOtherTime = this.sgAttack.doubleSetting("No Other Rotate", 0.0, 0.0, 5.0, 0.05,
            "The duration in seconds during which other modules are blocked from rotating while attacking.");

    public final Setting<Boolean> mineRotate = this.sgMining.booleanSetting("Mining Rotate", false,
            "Enables automated rotations specifically while mining or breaking blocks.");
    public final Setting<BlockRotationCheckMode> mineMode = this.sgMining.enumSetting("Mining Mode", BlockRotationCheckMode.Raytrace,
            "The verification mode used to ensure you are looking at the block being mined.");
    public final Setting<Double> mineYawAngle = this.sgMining.doubleSetting("Mining Yaw Angle", 90.0, 0.0, 180.0, 1.0,
            "Horizontal angle tolerance for validating block mining rotations.",
            () -> this.mineMode.get() == BlockRotationCheckMode.Angle);
    public final Setting<Double> minePitchAngle = this.sgMining.doubleSetting("Mining Pitch Angle", 45.0, 0.0, 180.0, 1.0,
            "Vertical angle tolerance for validating block mining rotations.",
            () -> this.mineMode.get() == BlockRotationCheckMode.Angle);
    public final Setting<Double> mineUpExpand = this.sgMining.doubleSetting("Mining Up Expand", 0.0, 0.0, 1.0, 0.01,
            "Increases the effective size of the block's hit-box upwards for easier raycasting.",
            () -> this.mineMode.get() == BlockRotationCheckMode.Raytrace);
    public final Setting<Double> mineDownExpand = this.sgMining.doubleSetting("Mining Down Expand", 0.0, 0.0, 1.0, 0.01,
            "Increases the effective size of the block's hit-box downwards for easier raycasting.",
            () -> this.mineMode.get() == BlockRotationCheckMode.Raytrace);
    public final Setting<Double> mineXZExpand = this.sgMining.doubleSetting("Mining XZ Expand", 0.0, 0.0, 1.0, 0.01,
            "Expands the block's hit-box horizontally on the X and Z axes for easier raycasting.",
            () -> this.mineMode.get() == BlockRotationCheckMode.Raytrace);
    public final Setting<MiningRotMode> mineTiming = this.sgMining.enumSetting("Mining Rotate Timing", MiningRotMode.End,
            "Determines if the rotation happens when starting to mine or just before the block breaks.");

    public final Setting<Boolean> blockRotate = this.rotateSetting("Block Place", "placing a block", this.sgBlockPlace);
    public final Setting<BlockRotationCheckMode> blockMode = this.blockModeSetting("Block Place", this.sgBlockPlace);
    public final Setting<Double> blockYawAngle = this.yawAngleSetting(
            "Block Place", this.sgBlockPlace, () -> this.blockMode.get() == BlockRotationCheckMode.Angle);
    public final Setting<Double> blockPitchAngle = this.pitchAngleSetting(
            "Block Place", this.sgBlockPlace, () -> this.blockMode.get() == BlockRotationCheckMode.Angle);
    public final Setting<Double> blockUpExpand = this.upExpandSetting(
            "Block Place", this.sgBlockPlace, () -> this.blockMode.get() == BlockRotationCheckMode.Raytrace);
    public final Setting<Double> blockDownExpand = this.downExpandSetting(
            "Block Place", this.sgBlockPlace, () -> this.blockMode.get() == BlockRotationCheckMode.Raytrace);
    public final Setting<Double> blockXZExpand = this.hzExpandSetting(
            "Block Place", this.sgBlockPlace, () -> this.blockMode.get() == BlockRotationCheckMode.Raytrace);

    public final Setting<Boolean> interactRotate = this.rotateSetting("Interact", "interacting with a block", this.sgInteract);
    public final Setting<BlockRotationCheckMode> interactMode = this.blockModeSetting("Interact", this.sgInteract);
    public final Setting<Double> interactYawAngle = this.yawAngleSetting(
            "Interact", this.sgInteract, () -> this.interactMode.get() == BlockRotationCheckMode.Angle);
    public final Setting<Double> interactPitchAngle = this.pitchAngleSetting(
            "Interact", this.sgInteract, () -> this.interactMode.get() == BlockRotationCheckMode.Angle);
    public final Setting<Double> interactUpExpand = this.upExpandSetting(
            "Interact", this.sgInteract, () -> this.interactMode.get() == BlockRotationCheckMode.Raytrace);
    public final Setting<Double> interactDownExpand = this.downExpandSetting(
            "Interact", this.sgInteract, () -> this.interactMode.get() == BlockRotationCheckMode.Raytrace);
    public final Setting<Double> interactXZExpand = this.hzExpandSetting(
            "Interact", this.sgInteract, () -> this.interactMode.get() == BlockRotationCheckMode.Raytrace);

    private int sinceRotated = 0;
    public final Vec3d vec = new Vec3d(0.0, 0.0, 0.0);
    public RotationSettings() {
        super("Rotate", false, true);
        INSTANCE = this;
    }

    public static RotationSettings getInstance() {
        return INSTANCE;
    }

    private Setting<Boolean> rotateSetting(String type, String verb, SettingGroup sg) {
        return sg.booleanSetting(type + " Rotate", false,
                "Enables automatic rotations while " + verb + " to ensure the action is correctly validated by the server.");
    }

    // TODO: modeSetting не используется
    private Setting<RotationCheckMode> modeSetting(String type, SettingGroup sg) {
        return sg.enumSetting(type + " Mode", RotationCheckMode.Raytrace,
                "The mathematical method used to verify if you are looking at the target entity.");
    }

    private Setting<BlockRotationCheckMode> blockModeSetting(String type, SettingGroup sg) {
        return sg.enumSetting(type + " Mode", BlockRotationCheckMode.Raytrace,
                "The mathematical method used to verify if you are looking at the target block.");
    }

    private Setting<Double> yawAngleSetting(String type, SettingGroup sg, SingleOut<Boolean> visible) {
        return sg.doubleSetting(type + " Yaw Angle", 90.0, 0.0, 180.0, 1.0,
                "The maximum horizontal degrees allowed between your crosshair and the target for the rotation to be considered valid.", visible);
    }

    private Setting<Double> pitchAngleSetting(String type, SettingGroup sg, SingleOut<Boolean> visible) {
        return sg.doubleSetting(type + " Pitch Angle", 45.0, 0.0, 180.0, 1.0,
                "The maximum vertical degrees allowed between your crosshair and the target for the rotation to be considered valid.", visible);
    }

    private Setting<Double> upExpandSetting(String type, SettingGroup sg, SingleOut<Boolean> visible) {
        return sg.doubleSetting(type + " Up Expand", 0.0, 0.0, 1.0, 0.01,
                "Artificially increases the top boundary of the target's bounding box to make raytrace verification more lenient.", visible);
    }

    private Setting<Double> downExpandSetting(String type, SettingGroup sg, SingleOut<Boolean> visible) {
        return sg.doubleSetting(type + " Down Expand", 0.0, 0.0, 1.0, 0.01,
                "Artificially increases the bottom boundary of the target's bounding box to make raytrace verification more lenient.", visible);
    }

    private Setting<Double> hzExpandSetting(String type, SettingGroup sg, SingleOut<Boolean> visible) {
        return sg.doubleSetting(type + " XZ Expand", 0.0, 0.0, 1.0, 0.01,
                "Artificially increases the horizontal width and depth of the target's bounding box for easier raytracing.", visible);
    }

    public boolean blockRotationCheck(BlockPos pos, Direction dir, float yaw, float pitch, RotationType type) {
        BlockRotationCheckMode m = this.mode(type);
        Vec3d end;
        BlockHitResult result;
        switch (m) {
            case Raytrace:
                Box boxx = this.expandedBox(pos, type);
                if (boxx.contains(BlackOut.mc.player.getEyePos())) {
                    return true;
                }

                if (this.raytraceCheck(boxx)) {
                    return true;
                }
                break;
            case DirectionRaytrace:
                return this.directionRaytraceCheck(BlackOut.mc.player.getEyePos(), yaw, pitch, pos, dir);
            case StrictRaytrace:
                end = RotationUtils.rotationVec(yaw, pitch, BlackOut.mc.player.getEyePos(), 7.0);
                Box boxxx = BoxUtils.get(pos);
                if (boxxx.contains(BlackOut.mc.player.getEyePos())) {
                    return true;
                }

                ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(BlackOut.mc.player.getEyePos(), end);
                result = DamageUtils.raycast(DamageUtils.raycastContext, false);
                if (result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                    return true;
                }

                return this.raytraceCheck(BlackOut.mc.player.getEyePos(), yaw, pitch, boxxx)
                        && SettingUtils.placeRangeTo(pos) < SettingUtils.getPlaceWallsRange();
            case DirectionStrict:
                end = RotationUtils.rotationVec(yaw, pitch, BlackOut.mc.player.getEyePos(), 7.0);
                ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(BlackOut.mc.player.getEyePos(), end);
                result = DamageUtils.raycast(DamageUtils.raycastContext, false);
                if (result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos) && result.getSide() == dir) {
                    return true;
                }

                return this.directionRaytraceCheck(BlackOut.mc.player.getEyePos(), yaw, pitch, pos, dir)
                        && SettingUtils.placeRangeTo(pos) < SettingUtils.getPlaceWallsRange();
            case Angle:
                Box box = BoxUtils.get(pos);
                if (box.contains(BlackOut.mc.player.getEyePos())) {
                    return true;
                }

                if (this.angleCheck(Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, box, RotationType.Attacking)) {
                    return true;
                }
                break;
            default:
                return true;
        }

        return false;
    }

    private Box expandedBox(BlockPos pos, RotationType type) {
        double up = switch (type.checkType) {
            case Interact -> this.interactUpExpand.get();
            case BlockPlace -> this.blockUpExpand.get();
            case Mining -> this.mineUpExpand.get();
            default -> 0.0;
        };

        double down = switch (type.checkType) {
            case Interact -> this.interactDownExpand.get();
            case BlockPlace -> this.blockDownExpand.get();
            case Mining -> this.mineDownExpand.get();
            default -> 0.0;
        };

        double hz = switch (type.checkType) {
            case Interact -> this.interactXZExpand.get();
            case BlockPlace -> this.blockXZExpand.get();
            case Mining -> this.mineXZExpand.get();
            default -> 0.0;
        };
        return new Box(
                pos.getX() - hz,
                pos.getY() - down,
                pos.getZ() - hz,
                pos.getX() + 1 + hz,
                pos.getY() + 1 + up,
                pos.getZ() + 1 + hz
        );
    }

    public Rotation applyStep(Rotation rotation, RotationType type, boolean rotated) {
        if (rotated) {
            this.sinceRotated = 0;
        }

        float oy = rotation.yaw();
        float op = rotation.pitch();
        double o = this.jitterStrength.get() / 5.0;
        o *= o * o;
        oy += (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * o);
        op = (float) MathHelper.clamp(op + (ThreadLocalRandom.current().nextDouble() - 0.5) * o, -90.0, 90.0);
        rotation = new Rotation(oy, op);
        if (type.instant) {
            return rotation;
        } else {
            double dy;
            double dp;
            double total;
            double multi;

            double realY;
            double realP;
            switch (this.rotationSpeedMode.get()) {
                case Separate:
                    double yawSpeed;
                    double pitchSpeed;
                    if (this.pauseRotated.get() && this.sinceRotated < this.delay.get()) {
                        yawSpeed = 0.0;
                        pitchSpeed = 0.0;
                    } else {
                        yawSpeed = this.yawStep(type);
                        pitchSpeed = this.pitchStep(type);
                    }

                    return this.applyStep(rotation, yawSpeed, pitchSpeed);
                case Normal:
                    dy = Math.abs(RotationUtils.yawAngle(Managers.ROTATION.prevYaw, rotation.yaw()));
                    dp = Math.abs(RotationUtils.pitchAngle(Managers.ROTATION.prevPitch, rotation.pitch()));
                    total = Math.sqrt(dy * dy + dp * dp);
                    if (this.pauseRotated.get() && this.sinceRotated < this.delay.get()) {
                        multi = 0.0;
                    } else {
                        multi = total == 0.0 ? Double.MAX_VALUE : this.rotationSpeed.get() / total;
                    }

                    realY = dy * multi;
                    realP = dp * multi;
                    return this.applyStep(rotation, realY, realP);
                case Balance:
                    dy = Math.abs(RotationUtils.yawAngle(Managers.ROTATION.prevYaw, rotation.yaw()));
                    dp = Math.abs(RotationUtils.pitchAngle(Managers.ROTATION.prevPitch, rotation.pitch()));
                    total = Math.sqrt(dy * dy + dp * dp);
                    if (this.pauseRotated.get() && this.sinceRotated < this.delay.get()) {
                        multi = 0.0;
                    } else {
                        multi = total == 0.0 ? Double.MAX_VALUE : this.getBalanceSpeed() / total;
                    }

                    realY = dy * multi;
                    realP = dp * multi;
                    return this.applyStep(rotation, realY, realP);
                case Smooth:
                    dy = Math.abs(RotationUtils.yawAngle(Managers.ROTATION.prevYaw, rotation.yaw()));
                    dp = Math.abs(RotationUtils.pitchAngle(Managers.ROTATION.prevPitch, rotation.pitch()));
                    total = Math.sqrt(dy * dy + dp * dp);
                    double speed;
                    if (this.pauseRotated.get() && this.sinceRotated < this.delay.get()) {
                        speed = 0.0;
                    } else {
                        speed = MathHelper.clampedLerp(
                                this.minSmoothSpeed.get(), this.maxSmoothSpeed.get(), MathHelper.getLerpProgress(total, this.minSmoothAngle.get(), this.maxSmoothAngle.get())
                        );
                    }

                    multi = total == 0.0 ? Double.MAX_VALUE : speed / total;
                    realY = dy * multi;
                    realP = dp * multi;
                    return this.applyStep(rotation, Math.min(realY, dy), Math.min(realP, dp));
                default:
                    return rotation;
            }
        }
    }

    private double getBalanceSpeed() {
        List<FloatFloatPair> list = Managers.ROTATION.tickRotationHistory;
        int length = Math.min(list.size(), this.averageTicks.get());
        if (length <= 1) {
            return this.averageSpeed.get();
        } else {
            FloatFloatPair prev = list.getFirst();
            double total = 0.0;

            for (int i = 1; i < length; i++) {
                FloatFloatPair pair = list.get(i);
                double yaw = Math.abs(RotationUtils.yawAngle(pair.firstFloat(), prev.firstFloat()));
                float pitch = pair.secondFloat() - prev.secondFloat();
                prev = pair;
                total += Math.min(Math.sqrt(yaw * yaw + pitch * pitch), 180.0);
            }

            return MathHelper.clamp(this.averageSpeed.get() * length - total, 0.0, this.maxSpeed.get());
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.sinceRotated++;
    }

    private Rotation applyStep(Rotation rotation, double yaw, double pitch) {
        return new Rotation(
                RotationUtils.nextYaw(Managers.ROTATION.prevYaw, rotation.yaw(), yaw * this.speedMulti()),
                RotationUtils.nextPitch(Managers.ROTATION.prevPitch, rotation.pitch(), pitch * this.speedMulti())
        );
    }

    private double speedMulti() {
        return !this.timeBasedSpeed.get() ? 1.0 : Math.min((System.currentTimeMillis() - Managers.ROTATION.prevRotation) / 50.0, this.maxMulti.get());
    }

    public boolean attackRotationCheck(Box box, float yaw, float pitch) {
        if (box.contains(BlackOut.mc.player.getEyePos())) {
            return true;
        } else {
            switch (this.attackMode.get()) {
                case Raytrace:
                    if (this.raytraceCheck(BlackOut.mc.player.getEyePos(), yaw, pitch, box)) {
                        return true;
                    }
                    break;
                case StrictRaytrace:
                    Vec3d end = RotationUtils.rotationVec(yaw, pitch, BlackOut.mc.player.getEyePos(), 7.0);
                    Optional<Vec3d> pos = box.raycast(BlackOut.mc.player.getEyePos(), end);
                    if (pos.isEmpty()) {
                        return false;
                    }

                    ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(BlackOut.mc.player.getEyePos(), pos.get());
                    boolean visible = DamageUtils.raycast(DamageUtils.raycastContext, false).getType() == HitResult.Type.MISS;
                    if (visible) {
                        return true;
                    }

                    return this.raytraceCheck(BlackOut.mc.player.getEyePos(), yaw, pitch, box)
                            && SettingUtils.attackRangeTo(box, BlackOut.mc.player.getPos()) < SettingUtils.getAttackWallsRange();
                case Angle:
                    if (this.angleCheck(Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, box, RotationType.Attacking)) {
                        return true;
                    }

                    if (this.raytraceCheck(BlackOut.mc.player.getEyePos(), yaw, pitch, box)) {
                        return true;
                    }
                    break;
                default:
                    return true;
            }

            return false;
        }
    }

    public Vec3d getRotationVec(BlockPos pos, Direction dir, Vec3d vec, RotationType type) {
        BlockRotationCheckMode mode = this.mode(type);
        if (mode != BlockRotationCheckMode.Raytrace && mode != BlockRotationCheckMode.Angle) {
            if (mode == BlockRotationCheckMode.DirectionRaytrace) {
                return pos.toCenterPos().add(dir.getOffsetX() / 2.0F, dir.getOffsetY() / 2.0F, dir.getOffsetZ() / 2.0F);
            } else {
                BlockState state = BlackOut.mc.world.getBlockState(pos);
                VoxelShape shape = state.getOutlineShape(BlackOut.mc.world, pos);
                Box box;
                if (shape.isEmpty()) {
                    box = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
                } else {
                    box = shape.getBoundingBox();
                }

                double minX = box.minX;
                double minY = box.minY;
                double minZ = box.minZ;
                double maxX = box.maxX;
                double maxY = box.maxY;
                double maxZ = box.maxZ;
                if (mode == BlockRotationCheckMode.DirectionStrict) {
                    minX = dir.getOffsetX() > 0 ? maxX : minX;
                    minY = dir.getOffsetY() > 0 ? maxY : minY;
                    minZ = dir.getOffsetZ() > 0 ? maxZ : minZ;
                    maxX = dir.getOffsetX() < 0 ? minX : maxX;
                    maxY = dir.getOffsetY() < 0 ? minY : maxY;
                    maxZ = dir.getOffsetZ() < 0 ? minZ : maxZ;
                }

                return this.getRaytraceRotationVec(new Box(minX, minY, minZ, maxX, maxY, maxZ), vec);
            }
        } else {
            return vec == null ? pos.toCenterPos() : vec;
        }
    }

    public Rotation getRotation(BlockPos pos, Direction dir, Vec3d vec, RotationType type) {
        return this.getRotation(this.getRotationVec(pos, dir, vec, type));
    }

    public Vec3d getAttackRotationVec(Box box, Vec3d vec) {
        RotationCheckMode mode = this.attackMode.get();
        if (mode != RotationCheckMode.Raytrace && mode != RotationCheckMode.Angle) {
            return this.getRaytraceRotationVec(box, vec);
        } else {
            return vec == null ? BoxUtils.middle(box) : vec;
        }
    }

    public Vec3d getRaytraceRotationVec(Box box, Vec3d vec) {
        Vec3d v = new Vec3d(0.0, 0.0, 0.0);
        double cd = 100.0;
        double ox = MathHelper.getLerpProgress(vec.x, box.minX, box.maxX);
        double oy = MathHelper.getLerpProgress(vec.y, box.minY, box.maxY);
        double oz = MathHelper.getLerpProgress(vec.z, box.minZ, box.maxZ);
        double lenX = box.getLengthX();
        double lenY = box.getLengthY();
        double lenZ = box.getLengthZ();

        for (int i = 1; i <= 9; i++) {
            double x = i / 10.0;
            ((IVec3d) v).blackout_Client$setX(box.minX + lenX * x);

            for (int j = 1; j <= 9; j++) {
                double y = j / 10.0;
                ((IVec3d) v).blackout_Client$setY(box.minY + lenY * y);

                for (int k = 1; k <= 9; k++) {
                    double z = k / 10.0;
                    ((IVec3d) v).blackout_Client$setZ(box.minZ + lenZ * z);

                    double distance = BlackOut.mc.player.getEyePos().distanceTo(v);
                    if (!(distance >= SettingUtils.getAttackRange())) {
                        if (distance > SettingUtils.getAttackWallsRange()) {
                            ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(BlackOut.mc.player.getEyePos(), v);
                            BlockHitResult result = DamageUtils.raycast(DamageUtils.raycastContext, false);
                            if (result.getType() != HitResult.Type.MISS) {
                                continue;
                            }
                        }

                        double dx = ox - x;
                        double dy = oy - y;
                        double dz = oz - z;
                        double d = dx * dx + dy * dy + dz * dz;
                        if (!(d >= cd)) {
                            cd = d;
                            ((IVec3d) vec).blackout_Client$set(v.x, v.y, v.z);
                        }
                    }
                }
            }
        }

        return vec;
    }

    public Rotation getAttackRotation(Box box, Vec3d vec) {
        return this.getRotation(this.getAttackRotationVec(box, vec));
    }

    public Rotation getRotation(Vec3d vec) {
        return new Rotation((float) RotationUtils.getYaw(vec), (float) RotationUtils.getPitch(vec));
    }

    public boolean shouldRotate(RotationType type) {
        return switch (type.checkType) {
            case Interact -> this.interactRotate.get();
            case BlockPlace -> this.blockRotate.get();
            case Mining -> this.mineRotate.get();
            case Attacking -> this.attackRotate.get();
            default -> true;
        };
    }

    public BlockRotationCheckMode mode(RotationType type) {
        return switch (type.checkType) {
            case Interact -> this.interactMode.get();
            case BlockPlace -> this.blockMode.get();
            case Mining -> this.mineMode.get();
            default -> null;
        };
    }

    public double yawAngle(RotationType type) {
        return switch (type.checkType) {
            case Interact -> this.interactYawAngle.get();
            case BlockPlace -> this.blockYawAngle.get();
            case Mining -> this.mineYawAngle.get();
            case Attacking -> this.attackYawAngle.get();
            default -> 0.0;
        };
    }

    public double pitchAngle(RotationType type) {
        return switch (type.checkType) {
            case Interact -> this.interactPitchAngle.get();
            case BlockPlace -> this.blockPitchAngle.get();
            case Mining -> this.minePitchAngle.get();
            case Attacking -> this.attackPitchAngle.get();
            default -> 0.0;
        };
    }

    public double yawStep(RotationType type) {
        return type.instant ? 42069.0 : this.yawStep.get() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * this.yawRandom.get();
    }

    public double pitchStep(RotationType type) {
        return type.instant ? 42069.0 : this.pitchStep.get() + (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * this.pitchRandom.get();
    }

    public boolean angleCheck(double y, double p, Box box, RotationType type) {
        double yawTo = RotationUtils.getYaw(BlackOut.mc.player.getEyePos(), box.getCenter(), y);
        double pitchTo = RotationUtils.getPitch(BlackOut.mc.player.getEyePos(), box.getCenter());
        return Math.abs(RotationUtils.yawAngle(y, yawTo)) <= this.yawAngle(type) && Math.abs(p - pitchTo) <= this.pitchAngle(type);
    }

    public boolean raytraceCheck(Box box) {
        return this.raytraceCheck(BlackOut.mc.player.getEyePos(), Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, box);
    }

    public boolean raytraceCheck(Vec3d pos, double y, double p, Box box) {
        double range = pos.distanceTo(OLEPOSSUtils.getMiddle(box)) + 3.0;
        Vec3d end = RotationUtils.rotationVec(y, p, pos, range);

        for (float i = 0.0F; i < 1.0F; i = (float) (i + 0.01)) {
            if (box.contains(
                    pos.x + (end.x - pos.x) * i,
                    pos.y + (end.y - pos.y) * i,
                    pos.z + (end.z - pos.z) * i
            )) {
                return true;
            }
        }

        return false;
    }

    public boolean directionRaytraceCheck(Vec3d pos, double yaw, double pitch, BlockPos block, Direction dir) {
        double range = pos.distanceTo(block.toCenterPos()) + 3.0;
        Vec3d end = RotationUtils.rotationVec(yaw, pitch, pos, range);

        for (float i = 0.0F; i < 1.0F; i = (float) (i + 0.001)) {
            double x = pos.x + (end.x - pos.x) * i;
            double y = pos.y + (end.y - pos.y) * i;
            double z = pos.z + (end.z - pos.z) * i;
            if (this.contains(x, y, z, block)) {
                Direction d = this.getDirection(x, y, z);
                if (d == dir) {
                    return true;
                }
            }
        }

        return false;
    }

    private Direction getDirection(double x, double y, double z) {
        double offsetX = this.offsetFrom(x);
        double offsetY = this.offsetFrom(y);
        double offsetZ = this.offsetFrom(z);
        Direction closest = null;
        double dist = 0.0;

        for (Direction dir : Direction.values()) {
            double d = dir.getVector().getSquaredDistance(offsetX, offsetY, offsetZ);
            if (closest == null || d < dist) {
                closest = dir;
                dist = d;
            }
        }

        return closest;
    }

    private double offsetFrom(double val) {
        return (val - Math.floor(val) - 0.5) * 2.0;
    }

    private boolean contains(double x, double y, double z, BlockPos pos) {
        return x >= pos.getX()
                && x <= pos.getX() + 1
                && y >= pos.getY()
                && y <= pos.getY() + 1
                && z >= pos.getZ()
                && z <= pos.getZ() + 1;
    }

    public boolean endMineRot() {
        return this.mineRotate.get() && (this.mineTiming.get() == MiningRotMode.End || this.mineTiming.get() == MiningRotMode.Double);
    }

    public boolean startMineRot() {
        return this.mineRotate.get() && (this.mineTiming.get() == MiningRotMode.Start || this.mineTiming.get() == MiningRotMode.Double);
    }

    public enum BlockRotationCheckMode {
        Raytrace,
        DirectionRaytrace,
        StrictRaytrace,
        DirectionStrict,
        Angle
    }

    public enum MiningRotMode {
        Start,
        End,
        Double
    }

    public enum PacketRotationMode {
        Disabled(null),
        Basic((yaw, pitch) -> {
            Vec3d pos = Managers.PACKET.pos;
            Managers.PACKET.sendPacket(new PlayerMoveC2SPacket.Full(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, Managers.PACKET.isOnGround()));
        }),
        Double((yaw, pitch) -> {
            Vec3d pos = Managers.PACKET.pos;

            for (int i = 0; i < 2; i++) {
                Managers.PACKET.sendPacket(new PlayerMoveC2SPacket.Full(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, Managers.PACKET.isOnGround()));
            }
        }),
        Return(
                (yaw, pitch) -> {
                    float prevYaw = Managers.ROTATION.prevYaw;
                    float prevPitch = Managers.ROTATION.prevPitch;
                    Vec3d pos = Managers.PACKET.pos;
                    Managers.PACKET.sendPacket(new PlayerMoveC2SPacket.Full(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, Managers.PACKET.isOnGround()));
                    Managers.PACKET
                            .sendPacket(new PlayerMoveC2SPacket.Full(pos.getX(), pos.getY(), pos.getZ(), prevYaw, prevPitch, Managers.PACKET.isOnGround()));
                }
        );

        private final DoubleConsumer<Float, Float> consumer;

        PacketRotationMode(DoubleConsumer<Float, Float> consumer) {
            this.consumer = consumer;
        }

        public void send(float yaw, float pitch) {
            this.consumer.accept(yaw, pitch);
        }
    }

    public enum RotationCheckMode {
        Raytrace,
        StrictRaytrace,
        Angle
    }

    public enum RotationSpeedMode {
        Separate,
        Normal,
        Smooth,
        Balance
    }
}
