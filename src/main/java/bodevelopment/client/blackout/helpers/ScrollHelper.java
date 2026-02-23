package bodevelopment.client.blackout.helpers;

import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import net.minecraft.util.math.MathHelper;

public class ScrollHelper {
    private final float friction;
    private final float speedMulti;
    private float targetScroll = -1.0F;
    private final SingleOut<Float> min;
    private final SingleOut<Float> max;
    private final TimerList<Float> scrolls = new TimerList<>(false);
    protected float speed = 0.0F;
    protected float scroll = 0.0F;
    private float limit = 0.0F;

    public ScrollHelper(float friction, float speedMulti, SingleOut<Float> min, SingleOut<Float> max) {
        this.friction = friction;
        this.speedMulti = speedMulti;
        this.min = min;
        this.max = max;
    }

    public ScrollHelper limit(float limit) {
        this.limit = limit;
        return this;
    }

    public float get() {
        return this.scroll;
    }

    public void reset() {
        this.speed = 0.0F;
        this.scroll = 0.0F;
    }

    public void update(float frameTime) {
        this.scrolls.update();
        this.scrolls.forEach(timer -> this.speed = this.speed + timer.value * frameTime * 20.0F);

        if (this.targetScroll != -1.0F) {
            float diff = this.targetScroll - this.scroll;
            this.scroll += diff * Math.min(1.0F, frameTime * 20.0F * 0.15F);
            this.speed = MathHelper.clampedLerp(this.speed, 0.0F, frameTime * 20.0F * 0.2F);

            if (Math.abs(diff) < 0.1F) {
                this.scroll = this.targetScroll;
                this.targetScroll = -1.0F;
            }
        } else {
            this.scroll = this.scroll + this.speed * frameTime * 20.0F;
        }

        this.clamp(this.min.get(), this.max.get());
        this.speed = MathHelper.clampedLerp(this.speed, 0.0F, frameTime * 20.0F * this.friction);
    }

    protected void clamp(float min, float max) {
        this.clampScroll(min, max);
        if (this.scroll == max) {
            this.speed = Math.min(this.speed, 0.0F);
        }

        if (this.scroll == min) {
            this.speed = Math.max(this.speed, 0.0F);
        }
    }

    private void clampScroll(float min, float max) {
        this.scroll = Math.min(this.scroll, max);
        this.scroll = Math.max(this.scroll, min);
        this.scroll = Math.min(this.scroll, max);
    }

    public void update(double frameTime) {
        this.update((float) frameTime);
    }

    public void add(float amount) {
        this.targetScroll = -1.0F;
        amount = -amount;
        if (this.limit > 0.0F) {
            amount = MathHelper.clamp(amount, -this.limit, this.limit);
        }

        this.scrolls.add(amount * this.speedMulti, 0.1);
    }

    public void set(float value) {
        this.scroll = value;
        this.speed = 0.0F;
        this.targetScroll = -1.0F;
        this.scrolls.clear();
    }

    public void animateTo(float target) {
        this.targetScroll = target;
    }

    public void offset(float amount) {
        this.scroll += amount;
    }

    public void add(double amount) {
        this.add((float) amount);
    }
}
