package bodevelopment.client.blackout.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class BoxUtils {
    public static Vec3d clamp(Vec3d vec, Box box) {
        return new Vec3d(
                MathHelper.clamp(vec.x, box.minX, box.maxX),
                MathHelper.clamp(vec.y, box.minY, box.maxY),
                MathHelper.clamp(vec.z, box.minZ, box.maxZ)
        );
    }

    public static Box get(BlockPos pos) {
        return new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public static Vec3d middle(Box box) {
        return new Vec3d((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
    }

    public static Vec3d feet(Box box) {
        return new Vec3d((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0);
    }

    public static Box crystalSpawnBox(BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY() + 1.0;
        double z = pos.getZ();

        double height = SettingUtils.cc() ? 1.0 : 2.0;

        return new Box(x, y, z, x + 1.0, y + height, z + 1.0);
    }

    public static Box lerp(float delta, Box start, Box end) {
        return new Box(
                MathHelper.lerp(delta, start.minX, end.minX),
                MathHelper.lerp(delta, start.minY, end.minY),
                MathHelper.lerp(delta, start.minZ, end.minZ),
                MathHelper.lerp(delta, start.maxX, end.maxX),
                MathHelper.lerp(delta, start.maxY, end.maxY),
                MathHelper.lerp(delta, start.maxZ, end.maxZ)
        );
    }

    public static Box expand(Box box, double amount) {
        return new Box(
                box.minX - amount, box.minY - amount, box.minZ - amount,
                box.maxX + amount, box.maxY + amount, box.maxZ + amount
        );
    }
}