package bodevelopment.client.blackout.util.render;

import bodevelopment.client.blackout.BlackOut;
import com.mojang.blaze3d.platform.GlStateManager;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Nested scissor region management. Each push intersects with the parent region.
 * AutoCloseable for automatic cleanup.
 *
 * Usage:
 *   try (ScissorStack.Region region = ScissorStack.push(x, y, w, h)) {
 *       // draw calls clipped to region
 *       try (ScissorStack.Region inner = ScissorStack.push(ix, iy, iw, ih)) {
 *           // draw calls clipped to intersection of both regions
 *       }
 *   }
 */
public class ScissorStack {
    private static final Deque<int[]> stack = new ArrayDeque<>();

    /**
     * Push a scissor region in GUI-scaled coordinates.
     * The actual GL scissor is the intersection of this region with all parent regions.
     */
    public static Region push(float x, float y, float w, float h) {
        double scale = BlackOut.mc.getWindow().getGuiScale();
        int screenH = BlackOut.mc.getWindow().getScreenHeight();

        int glX = (int) (x * scale);
        int glY = (int) (screenH - (y + h) * scale);
        int glW = Math.max((int) (w * scale), 0);
        int glH = Math.max((int) (h * scale), 0);

        if (!stack.isEmpty()) {
            int[] parent = stack.peek();
            int px1 = parent[0];
            int py1 = parent[1];
            int px2 = px1 + parent[2];
            int py2 = py1 + parent[3];

            int cx1 = Math.max(glX, px1);
            int cy1 = Math.max(glY, py1);
            int cx2 = Math.min(glX + glW, px2);
            int cy2 = Math.min(glY + glH, py2);

            glX = cx1;
            glY = cy1;
            glW = Math.max(cx2 - cx1, 0);
            glH = Math.max(cy2 - cy1, 0);
        }

        stack.push(new int[]{glX, glY, glW, glH});
        applyTop();
        return Region.INSTANCE;
    }

    /**
     * Push a scissor region with raw GL pixel coordinates (already scaled).
     */
    public static Region pushRaw(int glX, int glY, int glW, int glH) {
        if (!stack.isEmpty()) {
            int[] parent = stack.peek();
            int px1 = parent[0];
            int py1 = parent[1];
            int px2 = px1 + parent[2];
            int py2 = py1 + parent[3];

            int cx1 = Math.max(glX, px1);
            int cy1 = Math.max(glY, py1);
            int cx2 = Math.min(glX + glW, px2);
            int cy2 = Math.min(glY + glH, py2);

            glX = cx1;
            glY = cy1;
            glW = Math.max(cx2 - cx1, 0);
            glH = Math.max(cy2 - cy1, 0);
        }

        stack.push(new int[]{glX, glY, glW, glH});
        applyTop();
        return Region.INSTANCE;
    }

    public static void pop() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            GlStateManager._disableScissorTest();
        } else {
            applyTop();
        }
    }

    private static void applyTop() {
        int[] top = stack.peek();
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(top[0], top[1], top[2], top[3]);
    }

    public static void clear() {
        stack.clear();
        GlStateManager._disableScissorTest();
    }

    public static boolean isEmpty() {
        return stack.isEmpty();
    }

    public static class Region implements AutoCloseable {
        static final Region INSTANCE = new Region();

        private Region() {}

        @Override
        public void close() {
            ScissorStack.pop();
        }
    }
}
