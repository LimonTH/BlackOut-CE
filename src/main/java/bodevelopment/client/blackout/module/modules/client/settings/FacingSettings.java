package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.DoublePredicate;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.PlaceData;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class FacingSettings extends SettingsModule {
    private static FacingSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> strictDir = this.sgGeneral.b("Strict Direction", false,
            "Only allows placing blocks on faces that you are realistically looking at to prevent unnatural rotations.");
    public final Setting<Boolean> ncpDirection = this.sgGeneral.b("NCP Directions", false,
            "Applies specialized direction logic to bypass NoCheatPlus and similar anti-cheat raytrace checks.", this.strictDir::get);
    public final Setting<Boolean> unblocked = this.sgGeneral.b("Unblocked", false,
            "Ensures the selected block face isn't obstructed by another solid block.");
    public final Setting<Boolean> airPlace = this.sgGeneral.b("Air Place", false,
            "Attempts to place blocks even if no valid neighbor exists. Requires server-side support for ghost/air placement.");
    public final Setting<MaxHeight> maxHeight = this.sgGeneral.e("Max Height", MaxHeight.New,
            "Sets the world height limit for placement. Old: 255 (1.12), New: 319 (1.17+), Disabled: No limit.");

    public FacingSettings() {
        super("Facing", false, true);
        INSTANCE = this;
    }

    public static FacingSettings getInstance() {
        return INSTANCE;
    }

    public PlaceData getPlaceData(
            BlockPos blockPos, DoublePredicate<BlockPos, Direction> predicateOR, DoublePredicate<BlockPos, Direction> predicateAND, boolean ignoreContainers
    ) {
        Direction direction = null;
        boolean closestSneak = false;
        double closestDist = 1000.0;

        for (Direction dir : Direction.values()) {
            BlockPos pos = blockPos.offset(dir);
            boolean sneak = this.ignoreBlock(this.state(pos));
            if (!this.outOfBuildHeightCheck(pos)
                    && (!sneak || !ignoreContainers)
                    && (!this.strictDir.get() || OLEPOSSUtils.strictDir(pos, dir.getOpposite(), this.ncpDirection.get()))
                    && (predicateOR != null && predicateOR.test(pos, dir) || this.solid(pos) && (predicateAND == null || predicateAND.test(pos, dir)))) {
                double dist = SettingUtils.placeRangeTo(pos.offset(dir));
                if (direction == null || dist < closestDist) {
                    closestDist = dist;
                    direction = dir;
                    closestSneak = sneak;
                }
            }
        }

        if (this.airPlace.get()) {
            return new PlaceData(blockPos, Direction.UP, true, false);
        } else {
            return direction == null
                    ? new PlaceData(null, null, false, false)
                    : new PlaceData(blockPos.offset(direction), direction.getOpposite(), true, closestSneak);
        }
    }

    private boolean ignoreBlock(BlockState state) {
        if (state.hasBlockEntity()) {
            return true;
        } else {
            Block block = state.getBlock();
            return block instanceof AnvilBlock || block instanceof BedBlock || block instanceof CraftingTableBlock;
        }
    }

    public Direction getPlaceOnDirection(BlockPos position) {
        Direction direction = null;
        double closestDist = 1000.0;

        for (Direction dir : Direction.values()) {
            BlockPos pos = position.offset(dir);
            if (!this.outOfBuildHeightCheck(pos)
                    && (!this.unblocked.get() || !this.solid(pos))
                    && (!this.strictDir.get() || OLEPOSSUtils.strictDir(position, dir, this.ncpDirection.get()))) {
                double dist = this.dist(position, dir);
                if (direction == null || dist < closestDist) {
                    closestDist = dist;
                    direction = dir;
                }
            }
        }

        return direction;
    }

    private boolean solid(BlockPos pos) {
        return !this.state(pos).getCollisionShape(BlackOut.mc.world, pos).isEmpty();
    }

    private BlockState state(BlockPos pos) {
        return Managers.BLOCK.blockState(pos);
    }

    private boolean outOfBuildHeightCheck(BlockPos pos) {
        return pos.getY() > switch (this.maxHeight.get()) {
            case Old -> 255;
            case New -> 319;
            case Disabled -> 1000;
        };
    }

    private double dist(BlockPos pos, Direction dir) {
        Vec3d vec = new Vec3d(
                pos.getX() + dir.getOffsetX() / 2.0F, pos.getY() + dir.getOffsetY() / 2.0F, pos.getZ() + dir.getOffsetZ() / 2.0F
        );
        Vec3d dist = BlackOut.mc.player.getEyePos().subtract(vec);
        return dist.length();
    }

    public enum MaxHeight {
        Old,
        New,
        Disabled
    }
}
