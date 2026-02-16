package bodevelopment.client.blackout.helpers;

import bodevelopment.client.blackout.interfaces.functional.SingleOut;

public class SmoothScrollHelper extends ScrollHelper {
    public SmoothScrollHelper(float friction, float speedMulti, SingleOut<Float> max, SingleOut<Float> min) {
        super(friction, speedMulti, max, min);
    }

    @Override
    protected void clamp(float min, float max) {
        this.scroll = Math.max(this.scroll, min + this.speed);
        this.scroll = Math.min(this.scroll, max + this.speed);
        if (this.scroll == max) {
            this.speed = Math.min(this.speed, 0.0F);
        }

        if (this.scroll == min) {
            this.speed = Math.max(this.speed, 0.0F);
        }
    }
}
