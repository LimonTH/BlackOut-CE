package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.HoleType;
import bodevelopment.client.blackout.randomstuff.Hole;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class HoleUtils {
    public static Hole getHole(BlockPos pos) {
        return getHole(pos, true, true, true, 3, true);
    }

    public static Hole getHole(BlockPos pos, int depth) {
        return getHole(pos, depth, true);
    }

    public static Hole getHole(BlockPos pos, int depth, boolean floor) {
        return getHole(pos, true, true, true, depth, floor);
    }

    public static Hole getHole(BlockPos pos, boolean s, boolean d, boolean q, int depth, boolean floor) {
        if (!isAir(pos, depth, floor)) {
            return new Hole(pos, HoleType.NotHole);
        }

        if (isBlock(pos.west()) && isBlock(pos.north())) {

            boolean x = isAir(pos.east(), depth, floor) && isBlock(pos.east().north()) && isBlock(pos.east(2));
            boolean z = isAir(pos.south(), depth, floor) && isBlock(pos.south().west()) && isBlock(pos.south(2));

            // 1x1
            if (s && !x && !z && isBlock(pos.east()) && isBlock(pos.south())) {
                return new Hole(pos, HoleType.Single);
            }
            // 2x2
            else if (q && x && z
                    && isAir(pos.south().east(), depth, floor)
                    && isBlock(pos.east().east().south())
                    && isBlock(pos.south().south().east())) {
                return new Hole(pos, HoleType.Quad);
            }
            // 2x1 (X)
            else if (d && x && !z && isBlock(pos.south()) && isBlock(pos.south().east())) {
                return new Hole(pos, HoleType.DoubleX);
            }
            // 1x2 (Z)
            else if (d && z && !x && isBlock(pos.east()) && isBlock(pos.south().east())) {
                return new Hole(pos, HoleType.DoubleZ);
            }
        }

        return new Hole(pos, HoleType.NotHole);
    }



    static boolean isBlock(BlockPos pos) {
        if (BlackOut.mc.world == null) return false;
        return OLEPOSSUtils.collidable(pos) && OLEPOSSUtils.solid2(pos) && OLEPOSSUtils.isSafe(pos);
    }

    static boolean isAir(BlockPos pos, int depth, boolean floor) {
        if (BlackOut.mc.world == null) return false;

        if (floor && !isBlock(pos.down())) return false;

        for (int i = 0; i < depth; i++) {
            BlockState state = BlackOut.mc.world.getBlockState(pos.up(i));
            if (!state.isAir() && !state.isReplaceable() && !OLEPOSSUtils.replaceable(pos.up(i))) {
                return false;
            }
        }
        return true;
    }

    public static Hole currentHole(BlockPos pos) {
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                Hole hole = getHole(pos.add(x, 0, z), 1);
                if (hole.type != HoleType.NotHole) {
                    if (isPosInHole(pos, hole)) return hole;
                }
            }
        }
        return new Hole(pos, HoleType.NotHole);
    }

    private static boolean isPosInHole(BlockPos pos, Hole hole) {
        return switch (hole.type) {
            case Single -> pos.equals(hole.pos);
            case DoubleX -> pos.equals(hole.pos) || pos.equals(hole.pos.east());
            case DoubleZ -> pos.equals(hole.pos) || pos.equals(hole.pos.south());
            case Quad -> pos.getX() >= hole.pos.getX() && pos.getX() <= hole.pos.getX() + 1
                    && pos.getZ() >= hole.pos.getZ() && pos.getZ() <= hole.pos.getZ() + 1;
            default -> false;
        };
    }

    public static boolean inHole(BlockPos pos) {
        return currentHole(pos).type != HoleType.NotHole;
    }
}
