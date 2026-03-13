package bodevelopment.client.blackout.util.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public class WireframeContext {
    public final List<Vec3[]> lines = new ArrayList<>();
    public final List<Vec3[]> quads = new ArrayList<>();

    public static WireframeContext of(List<Vec3[]> positions) {
        WireframeContext context = new WireframeContext();
        context.quads.addAll(positions);

        for (Vec3[] arr : positions) {
            for (int i = 0; i < 4; i++) {
                Vec3 v1 = arr[i];
                Vec3 v2 = arr[(i + 1) % 4];

                Vec3[] line = new Vec3[]{v1, v2};
                if (!contains(context.lines, line)) {
                    context.lines.add(line);
                }
            }
        }
        return context;
    }

    protected static boolean contains(List<Vec3[]> list, Vec3[] line) {
        for (Vec3[] arr : list) {
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
