package bodevelopment.client.blackout.util.render;

public class AnimUtils {

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }
    public static double easeOutQuart(double start, double end, double progress) {
        return lerp(easeOutQuart(progress), start, end);
    }

    public static double getAnimProgressD(long startTime, double time) {
        return Math.min((double) (System.currentTimeMillis() - startTime), time * 1000.0) / time / 1000.0;
    }

    public static float getAnimProgressF(long startTime, float time) {
        return (float) getAnimProgressD(startTime, time);
    }

    public static double easeOutBack(double delta) {
        double c1 = 1.70158;
        double c3 = c1 + 1.0;
        return 1.0 + c3 * Math.pow(delta - 1.0, 3.0) + c1 * Math.pow(delta - 1.0, 2.0);
    }

    public static double easeOutBounce(double delta) {
        double n1 = 7.5625;
        double d1 = 2.75;
        if (delta < 1.0 / d1) {
            return n1 * delta * delta;
        } else if (delta < 2.0 / d1) {
            double bounceProgress = delta - 1.5 / d1;
            return n1 * bounceProgress * bounceProgress + 0.75;
        } else if (delta < 2.5 / d1) {
            double bounceProgress = delta - 2.25 / d1;
            return n1 * bounceProgress * bounceProgress + 0.9375;
        } else {
            double bounceProgress = delta - 2.625 / d1;
            return n1 * bounceProgress * bounceProgress + 0.984375;
        }
    }

    public static double easeInSine(double delta) {
        return 1.0 - Math.cos(delta * Math.PI / 2.0);
    }

    public static double easeOutSine(double delta) {
        return Math.sin(delta * Math.PI / 2.0);
    }

    public static double easeInOutSine(double delta) {
        return -(Math.cos(Math.PI * delta) - 1.0) / 2.0;
    }

    public static double easeInQuad(double delta) {
        return delta * delta;
    }

    public static double easeOutQuad(double delta) {
        return 1.0 - (1.0 - delta) * (1.0 - delta);
    }

    public static double easeInOutQuad(double delta) {
        return delta < 0.5 ? 2.0 * delta * delta : 1.0 - Math.pow(-2.0 * delta + 2.0, 2.0) / 2.0;
    }

    public static double easeInCubic(double delta) {
        return delta * delta * delta;
    }

    public static double easeOutCubic(double delta) {
        return 1.0 - Math.pow(1.0 - delta, 3.0);
    }

    public static double easeInOutCubic(double delta) {
        return delta < 0.5 ? 4.0 * delta * delta * delta : 1.0 - Math.pow(-2.0 * delta + 2.0, 3.0) / 2.0;
    }

    public static double easeInQuart(double delta) {
        return delta * delta * delta * delta;
    }

    public static double easeOutQuart(double delta) {
        return 1.0 - Math.pow(1.0 - delta, 4.0);
    }

    public static double easeInOutQuart(double delta) {
        return delta < 0.5 ? 8.0 * delta * delta * delta * delta : 1.0 - Math.pow(-2.0 * delta + 2.0, 4.0) / 2.0;
    }

    public static double easeInQuint(double delta) {
        return delta * delta * delta * delta * delta;
    }

    public static double easeOutQuint(double delta) {
        return 1.0 - Math.pow(1.0 - delta, 5.0);
    }

    public static double easeInOutQuint(double delta) {
        return delta < 0.5 ? 16.0 * delta * delta * delta * delta * delta : 1.0 - Math.pow(-2.0 * delta + 2.0, 5.0) / 2.0;
    }

    public static double easeInExpo(double delta) {
        return delta == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * delta - 10.0);
    }

    public static double easeOutExpo(double delta) {
        return delta == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * delta);
    }

    public static double easeInOutExpo(double delta) {
        return delta == 0.0
                ? 0.0
                : (delta == 1.0 ? 1.0 : (delta < 0.5 ? Math.pow(2.0, 20.0 * delta - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * delta + 10.0)) / 2.0));
    }

    public static double easeInCirc(double delta) {
        return 1.0 - Math.sqrt(1.0 - Math.pow(delta, 2.0));
    }

    public static double easeOutCirc(double delta) {
        return Math.sqrt(1.0 - Math.pow(delta - 1.0, 2.0));
    }

    public static double easeInOutCirc(double delta) {
        return delta < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * delta, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * delta + 2.0, 2.0)) + 1.0) / 2.0;
    }

    public static double easeInBack(double delta) {
        double c1 = 1.70158;
        double c3 = c1 + 1.0;
        return c3 * delta * delta * delta - c1 * delta * delta;
    }

    public static double easeInOutBack(double delta) {
        double c1 = 1.70158;
        double c2 = c1 * 1.525;
        return delta < 0.5
                ? Math.pow(2.0 * delta, 2.0) * ((c2 + 1.0) * 2.0 * delta - c2) / 2.0
                : (Math.pow(2.0 * delta - 2.0, 2.0) * ((c2 + 1.0) * (delta * 2.0 - 2.0) + c2) + 2.0) / 2.0;
    }

    public static double easeInElastic(double delta) {
        double c4 = Math.PI * 2.0 / 3.0;
        return delta == 0.0 ? 0.0 : (delta == 1.0 ? 1.0 : -Math.pow(2.0, 10.0 * delta - 10.0) * Math.sin((delta * 10.0 - 10.75) * c4));
    }

    public static double easeOutElastic(double delta) {
        double c4 = Math.PI * 2.0 / 3.0;
        return delta == 0.0 ? 0.0 : (delta == 1.0 ? 1.0 : Math.pow(2.0, -10.0 * delta) * Math.sin((delta * 10.0 - 0.75) * c4) + 1.0);
    }

    public static double easeInOutElastic(double delta) {
        double c5 = Math.PI * 4.0 / 9.0;
        double sin = Math.sin((20.0 * delta - 11.125) * c5);
        return delta == 0.0
                ? 0.0
                : (
                delta == 1.0
                        ? 1.0
                        : (
                        delta < 0.5
                                ? -(Math.pow(2.0, 20.0 * delta - 10.0) * sin) / 2.0
                                : Math.pow(2.0, -20.0 * delta + 10.0) * sin / 2.0 + 1.0
                )
        );
    }

    public static double easeInBounce(double delta) {
        return 1.0 - easeOutBounce(1.0 - delta);
    }

    public static double easeInOutBounce(double delta) {
        return delta < 0.5 ? (1.0 - easeOutBounce(1.0 - 2.0 * delta)) / 2.0 : (1.0 + easeOutBounce(2.0 * delta - 1.0)) / 2.0;
    }
}
