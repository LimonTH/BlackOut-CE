package bodevelopment.client.blackout.util.render;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class WireframeContext {
    public final List<Vec3d[]> lines = new ArrayList<>();
    public final List<Vec3d[]> quads = new ArrayList<>();

    public static WireframeContext of(List<Vec3d[]> positions) {
        WireframeContext context = new WireframeContext();
        context.quads.addAll(positions);

        for (Vec3d[] arr : positions) {
            for (int i = 0; i < 4; i++) {
                Vec3d v1 = arr[i];
                Vec3d v2 = arr[(i + 1) % 4];

                Vec3d[] line = new Vec3d[]{v1, v2};
                if (!contains(context.lines, line)) {
                    context.lines.add(line);
                }
            }
        }
        return context;
    }

    protected static boolean contains(List<Vec3d[]> list, Vec3d[] line) {
        for (Vec3d[] arr : list) {
            if (arr[0].equals(line[0]) && arr[1].equals(line[1])) {
                return true;
            }

            if (arr[0].equals(line[1]) && arr[1].equals(line[0])) {
                return true;
            }
        }

        return false;
    }
}
