package bodevelopment.client.blackout.util;

public class MathUtils {
    // Projectile physics constants (Minecraft tick-based simulation)
    public static final double THROWABLE_GRAVITY = 0.03;
    public static final double ARROW_GRAVITY = 0.05;
    public static final double EXP_BOTTLE_GRAVITY = 0.07;
    public static final double PROJECTILE_AIR_DRAG = 0.99;
    public static final double PROJECTILE_WATER_DRAG_FAST = 0.8;
    public static final double PROJECTILE_WATER_DRAG_SLOW = 0.6;

    public static double safeDivide(double v1, double v2) {
        double result = v1 / v2;
        return Double.isNaN(result) ? 1.0 : result;
    }

    public static int closerToZero(int x) {
        return (int) (x - Math.signum((float) x));
    }

    public static double approach(double from, double to, double delta) {
        return to > from ? Math.min(from + delta, to) : Math.max(from - delta, to);
    }
}
