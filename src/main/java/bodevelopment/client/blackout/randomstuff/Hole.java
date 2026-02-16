package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.enums.HoleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Hole {
    public static final BlockPos[] POSITIONS = new BlockPos[0];
    public final BlockPos pos;
    public final HoleType type;
    public final BlockPos[] positions;
    public final Vec3d middle;

    public Hole(BlockPos pos, HoleType type) {
        this.pos = pos;
        this.type = type;
        switch (type) {
            case Single:
                this.positions = new BlockPos[]{pos};
                this.middle = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                break;
            case DoubleX:
                this.positions = new BlockPos[]{pos, pos.add(1, 0, 0)};
                this.middle = new Vec3d(pos.getX() + 1, pos.getY(), pos.getZ() + 0.5);
                break;
            case DoubleZ:
                this.positions = new BlockPos[]{pos, pos.add(0, 0, 1)};
                this.middle = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1);
                break;
            case Quad:
                this.positions = new BlockPos[]{pos, pos.add(1, 0, 0), pos.add(0, 0, 1), pos.add(1, 0, 1)};
                this.middle = new Vec3d(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
                break;
            default:
                this.positions = POSITIONS;
                this.middle = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }
    }
}
