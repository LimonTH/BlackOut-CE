package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingState;
import bodevelopment.client.blackout.enums.SwingType;
import bodevelopment.client.blackout.interfaces.functional.DoublePredicate;
import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalEntity;
import bodevelopment.client.blackout.module.modules.client.settings.*;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.randomstuff.Rotation;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SettingUtils {
    private static FacingSettings facing;
    private static RangeSettings range;
    private static RaytraceSettings raytrace;
    private static RotationSettings rotation;
    private static ServerSettings server;
    private static SwingSettings swing;
    private static ExtrapolationSettings extrapolation;

    public static void init() {
        facing = FacingSettings.getInstance();
        range = RangeSettings.getInstance();
        raytrace = RaytraceSettings.getInstance();
        rotation = RotationSettings.getInstance();
        server = ServerSettings.getInstance();
        swing = SwingSettings.getInstance();
        extrapolation = ExtrapolationSettings.getInstance();
    }

    public static double maxInteractRange() {
        return Math.max(getInteractRange(), getInteractWallsRange());
    }

    public static double maxPlaceRange() {
        return Math.max(getPlaceRange(), getPlaceWallsRange());
    }

    public static double maxMineRange() {
        return Math.max(getMineRange(), getMineWallsRange());
    }

    public static double getInteractRange() {
        return range.interactRange.get();
    }

    public static double getInteractWallsRange() {
        return range.interactRangeWalls.get();
    }

    public static double interactRangeTo(BlockPos pos) {
        return range.interactRangeTo(pos, null);
    }

    public static boolean inInteractRange(BlockPos pos) {
        return range.inInteractRange(pos, null);
    }

    public static boolean inInteractRange(BlockPos pos, Vec3d from) {
        return range.inInteractRange(pos, from);
    }

    public static boolean inInteractRangeNoTrace(BlockPos pos) {
        return range.inInteractRangeNoTrace(pos, null);
    }

    public static boolean inInteractRangeNoTrace(BlockPos pos, Vec3d from) {
        return range.inInteractRangeNoTrace(pos, from);
    }

    public static double getPlaceRange() {
        return range.placeRange.get();
    }

    public static double getPlaceWallsRange() {
        return range.placeRangeWalls.get();
    }

    public static double placeRangeTo(BlockPos pos) {
        return range.placeRangeTo(pos, null);
    }

    public static boolean inPlaceRange(BlockPos pos) {
        return range.inPlaceRange(pos, null);
    }

    public static boolean inPlaceRange(BlockPos pos, Vec3d from) {
        return range.inPlaceRange(pos, from);
    }

    public static boolean inPlaceRangeNoTrace(BlockPos pos) {
        return range.inPlaceRangeNoTrace(pos, null);
    }

    public static boolean inPlaceRangeNoTrace(BlockPos pos, Vec3d from) {
        return range.inPlaceRangeNoTrace(pos, from);
    }

    public static double getAttackRange() {
        return range.attackRange.get();
    }

    public static double getAttackWallsRange() {
        return range.attackRangeWalls.get();
    }

    public static double attackRangeTo(Box bb, Vec3d feet) {
        return range.innerAttackRangeTo(bb, feet, false);
    }

    public static double wallAttackRangeTo(Box bb, Vec3d feet) {
        return range.innerAttackRangeTo(bb, feet, true);
    }

    public static boolean inAttackRange(Box bb) {
        return range.inAttackRange(bb, null);
    }

    public static boolean inAttackRange(Box bb, Vec3d from) {
        return range.inAttackRange(bb, from);
    }

    public static boolean inAttackRangeNoTrace(Box bb) {
        return range.inAttackRangeNoTrace(bb, null);
    }

    public static boolean inAttackRangeNoTrace(Box bb, Vec3d from) {
        return range.inAttackRangeNoTrace(bb, from);
    }

    public static double getMineRange() {
        return range.mineRange.get();
    }

    public static double getMineWallsRange() {
        return range.mineRangeWalls.get();
    }

    public static double mineRangeTo(BlockPos pos) {
        return range.miningRangeTo(pos, null);
    }

    public static boolean inMineRange(BlockPos pos) {
        return range.inMineRange(pos);
    }

    public static boolean inMineRangeNoTrace(BlockPos pos) {
        return range.inMineRangeNoTrace(pos);
    }

    public static boolean startMineRot() {
        return rotation.startMineRot();
    }

    public static boolean endMineRot() {
        return rotation.endMineRot();
    }

    public static boolean shouldVanillaRotate() {
        return rotation.vanillaRotation.get();
    }

    public static boolean shouldRotate(RotationType type) {
        return rotation.shouldRotate(type);
    }

    public static boolean blockRotationCheck(BlockPos pos, Direction dir, float yaw, float pitch, RotationType type) {
        return rotation.blockRotationCheck(pos, dir, yaw, pitch, type);
    }

    public static boolean attackRotationCheck(Box box, float yaw, float pitch) {
        return rotation.attackRotationCheck(box, yaw, pitch);
    }

    public static double yawStep(RotationType type) {
        return rotation.yawStep(type);
    }

    public static double pitchStep(RotationType type) {
        return rotation.pitchStep(type);
    }

    public static Rotation getRotation(BlockPos pos, Direction dir, Vec3d vec, RotationType type) {
        return rotation.getRotation(pos, dir, vec, type);
    }

    public static Vec3d getRotationVec(BlockPos pos, Direction dir, Vec3d vec, RotationType type) {
        return rotation.getRotationVec(pos, dir, vec, type);
    }

    public static Rotation getRotation(Vec3d vec) {
        return rotation.getRotation(vec);
    }

    public static Rotation getAttackRotation(Box box, Vec3d vec) {
        return rotation.getAttackRotation(box, vec);
    }

    public static double returnSpeed() {
        return rotation.returnSpeed.get();
    }

    public static Rotation applyStep(Rotation rot, RotationType type, boolean rotated) {
        return rotation.applyStep(rot, type, rotated);
    }

    public static boolean attackLimit() {
        return rotation.attackLimit.get();
    }

    public static double attackSpeed() {
        return rotation.attackMaxSpeed.get();
    }

    public static int attackTicks() {
        return rotation.attackTicks.get();
    }

    public static boolean rotationIgnoreEnabled() {
        return rotation.noOwnTime.get() > 0.0 || rotation.noOtherTime.get() > 0.0;
    }

    public static boolean shouldIgnoreRotations(EndCrystalEntity entity) {
        IEndCrystalEntity iEntity = (IEndCrystalEntity) entity;
        long since = System.currentTimeMillis() - iEntity.blackout_Client$getSpawnTime();
        return since < (iEntity.blackout_Client$isOwn() ? rotation.noOwnTime : rotation.noOtherTime).get() * 1000.0;
    }

    public static void swing(SwingState state, SwingType type, Hand hand) {
        swing.swing(state, type, hand);
    }

    public static void mineSwing(SwingSettings.MiningSwingState state) {
        swing.mineSwing(state);
    }

    public static PlaceData getPlaceData(BlockPos pos) {
        return facing.getPlaceData(pos, null, null, true);
    }

    public static PlaceData getPlaceData(BlockPos pos, boolean ignoreContainers) {
        return facing.getPlaceData(pos, null, null, ignoreContainers);
    }

    public static PlaceData getPlaceData(
            BlockPos pos, DoublePredicate<BlockPos, Direction> predicateOR, DoublePredicate<BlockPos, Direction> predicateAND, boolean ignoreContainers
    ) {
        return facing.getPlaceData(pos, predicateOR, predicateAND, ignoreContainers);
    }

    public static PlaceData getPlaceData(
            BlockPos pos, DoublePredicate<BlockPos, Direction> predicateOR, DoublePredicate<BlockPos, Direction> predicateAND
    ) {
        return facing.getPlaceData(pos, predicateOR, predicateAND, true);
    }

    public static Direction getPlaceOnDirection(BlockPos pos) {
        return facing.getPlaceOnDirection(pos);
    }

    public static boolean shouldInteractTrace() {
        return raytrace.interactTrace.get();
    }

    public static boolean shouldPlaceTrace() {
        return raytrace.placeTrace.get();
    }

    public static boolean shouldAttackTrace() {
        return raytrace.attackTrace.get();
    }

    public static boolean shouldMineTrace() {
        return raytrace.mineTrace.get();
    }

    public static boolean interactTrace(BlockPos pos) {
        return raytrace.interactTrace(pos);
    }

    public static boolean placeTrace(BlockPos pos) {
        return raytrace.placeTrace(pos);
    }

    public static boolean attackTrace(Box bb) {
        return raytrace.attackTrace(bb);
    }

    public static boolean mineTrace(BlockPos pos) {
        return raytrace.mineTrace(pos);
    }

    public static boolean oldCrystals() {
        return server.oldCrystals.get();
    }

    public static boolean cc() {
        return server.cc.get();
    }

    public static boolean grimMovement() {
        return server.grimMovement.get();
    }

    public static boolean grimPackets() {
        return server.grimPackets.get();
    }

    public static boolean grimUsing() {
        return server.grimUsing.get();
    }

    public static boolean strictSprint() {
        return server.strictSprint.get();
    }

    public static boolean stepPredict() {
        return extrapolation.stepPredict.get();
    }

    public static int reverseStepTicks() {
        return extrapolation.reverseStepTicks.get();
    }

    public static boolean reverseStepPredict() {
        return extrapolation.reverseStepPredict.get();
    }

    public static int stepTicks() {
        return extrapolation.stepTicks.get();
    }

    public static boolean jumpPredict() {
        return extrapolation.jumpPredict.get();
    }
}
