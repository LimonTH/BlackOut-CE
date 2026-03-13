package bodevelopment.client.blackout.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BoxUtils {
    public static Vec3 clamp(Vec3 vec, AABB box) {
        return new Vec3(
                Mth.clamp(vec.x, box.minX, box.maxX),
                Mth.clamp(vec.y, box.minY, box.maxY),
                Mth.clamp(vec.z, box.minZ, box.maxZ)
        );
    }

    public static AABB get(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public static Vec3 middle(AABB box) {
        return new Vec3((box.minX + box.maxX) / 2.0, (box.minY + box.maxY) / 2.0, (box.minZ + box.maxZ) / 2.0);
    }

    public static Vec3 feet(AABB box) {
        return new Vec3((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0);
    }

    public static AABB crystalSpawnBox(BlockPos pos) {
        double x = pos.getX();
        double y = pos.getY() + 1.0;
        double z = pos.getZ();

        double height = SettingUtils.cc() ? 1.0 : 2.0;

        return new AABB(x, y, z, x + 1.0, y + height, z + 1.0);
    }

    public static AABB lerp(float delta, AABB start, AABB end) {
        return new AABB(
                Mth.lerp(delta, start.minX, end.minX),
                Mth.lerp(delta, start.minY, end.minY),
                Mth.lerp(delta, start.minZ, end.minZ),
                Mth.lerp(delta, start.maxX, end.maxX),
                Mth.lerp(delta, start.maxY, end.maxY),
                Mth.lerp(delta, start.maxZ, end.maxZ)
        );
    }

    public static AABB expand(AABB box, double amount) {
        return new AABB(
                box.minX - amount, box.minY - amount, box.minZ - amount,
                box.maxX + amount, box.maxY + amount, box.maxZ + amount
        );
    }
}