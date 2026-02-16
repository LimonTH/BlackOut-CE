package bodevelopment.client.blackout.event.events;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

public class RenderEvent {
    public double frameTime = 0.0;
    public float tickDelta = 0.0F;
    private long prevEvent = 0L;

    protected void setFrameTime() {
        if (this.prevEvent > 0L) {
            this.frameTime = (System.currentTimeMillis() - this.prevEvent) / 1000.0;
        }

        this.prevEvent = System.currentTimeMillis();
    }

    public static class Hud extends RenderEvent {
        public DrawContext context;

        public static class Post extends Hud {
            private static final Post INSTANCE = new Post();

            public static Post get(DrawContext context, float tickDelta) {
                INSTANCE.context = context;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }

        public static class Pre extends Hud {
            private static final Pre INSTANCE = new Pre();

            public static Pre get(DrawContext context, float tickDelta) {
                INSTANCE.context = context;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }
    }

    public static class World extends RenderEvent {
        public MatrixStack stack = null;

        public static class Post extends World {
            private static final Post INSTANCE = new Post();

            public static Post get(MatrixStack stack, float tickDelta) {
                INSTANCE.stack = stack;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }

        public static class Pre extends World {
            private static final Pre INSTANCE = new Pre();

            public static Pre get(MatrixStack stack, float tickDelta) {
                INSTANCE.stack = stack;
                INSTANCE.tickDelta = tickDelta;
                INSTANCE.setFrameTime();
                return INSTANCE;
            }
        }
    }
}
