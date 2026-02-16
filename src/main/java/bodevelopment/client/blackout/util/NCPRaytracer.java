package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class NCPRaytracer {
    public static boolean raytrace(Vec3d from, Vec3d to, Box box) {
        int lx = 0;
        int ly = 0;
        int lz = 0;

        for (double delta = 0.0; delta < 1.0; delta += 0.001F) {
            double x = MathHelper.lerp(from.x, to.x, delta);
            double y = MathHelper.lerp(from.y, to.y, delta);
            double z = MathHelper.lerp(from.z, to.z, delta);
            if (box.contains(x, y, z)) {
                return true;
            }

            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            int iz = (int) Math.floor(z);
            if (lx != ix || ly != iy || lz != iz) {
                BlockPos pos = new BlockPos(ix, iy, iz);
                if (validForCheck(pos, BlackOut.mc.world.getBlockState(pos))) {
                    return false;
                }
            }

            lx = ix;
            ly = iy;
            lz = iz;
        }

        return false;
    }

    public static boolean validForCheck(BlockPos pos, BlockState state) {
        if (!state.getCollisionShape(BlackOut.mc.world, pos).isEmpty()) {
            return true;
        } else if (state.getBlock() instanceof FluidBlock) {
            return false;
        } else if (state.getBlock() instanceof StairsBlock) {
            return false;
        } else {
            return !state.hasBlockEntity() && state.isFullCube(BlackOut.mc.world, pos);
        }
    }
}
