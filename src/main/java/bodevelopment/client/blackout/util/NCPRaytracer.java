package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NCPRaytracer {
    public static boolean raytrace(Vec3 from, Vec3 to, AABB box) {
        int lx = 0;
        int ly = 0;
        int lz = 0;

        for (double delta = 0.0; delta < 1.0; delta += 0.001F) {
            double x = Mth.lerp(from.x, to.x, delta);
            double y = Mth.lerp(from.y, to.y, delta);
            double z = Mth.lerp(from.z, to.z, delta);
            if (box.contains(x, y, z)) {
                return true;
            }

            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            int iz = (int) Math.floor(z);
            if (lx != ix || ly != iy || lz != iz) {
                BlockPos pos = new BlockPos(ix, iy, iz);
                if (validForCheck(pos, BlackOut.mc.level.getBlockState(pos))) {
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
        if (!state.getCollisionShape(BlackOut.mc.level, pos).isEmpty()) {
            return true;
        } else if (state.getBlock() instanceof LiquidBlock) {
            return false;
        } else if (state.getBlock() instanceof StairBlock) {
            return false;
        } else {
            return !state.hasBlockEntity() && state.isCollisionShapeFullBlock(BlackOut.mc.level, pos);
        }
    }
}
